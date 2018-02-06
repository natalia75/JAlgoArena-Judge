package com.jalgoarena.judge

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jalgoarena.compile.CompileErrorException
import com.jalgoarena.compile.JvmCompiler
import com.jalgoarena.domain.Function
import com.jalgoarena.domain.JudgeRequest
import com.jalgoarena.domain.JudgeResult
import com.jalgoarena.domain.JudgeResult.*
import com.jalgoarena.domain.Problem
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.*
import javax.inject.Inject

@Component
open class JvmJudgeEngine(
        @Inject private val objectMapper: ObjectMapper,
        @Inject private val compilers: List<JvmCompiler>
) : JudgeEngine {

    private val NUMBER_OF_ITERATIONS = 5
    private val MEMORY_LIMIT_256_MB = 256L

    private val threadFactory = ThreadFactoryBuilder()
            .setNameFormat("Judge-%d")
            .setDaemon(true)
            .build()

    override fun judge(problem: Problem, judgeRequest: JudgeRequest): JudgeResult {

        val (sourceCode, userId, language) = judgeRequest

        val className = findClassName(language, sourceCode)

        if (!className.isPresent) {
            return CompileError("ClassNotFoundException: No public class found")
        }

        val compiler = compilers.first { it.programmingLanguage() == language }

        return compileAndJudge(className, compiler, problem.func!!, problem, judgeRequest.sourceCode)
    }

    private fun judge(clazz: Any, method: Method, problem: Problem): JudgeResult {

        val executorService = Executors.newSingleThreadExecutor(threadFactory)
        try {
            val testCases = readInternalTestCases(problem)
            val judge = executorService.submit(JudgeTask(clazz, method, testCases))

            return handleFutureRunExceptions {
                val (testCasesResults, performanceResult) = coldRun(judge, problem)

                val failedTestCases = testCasesResults.filter({ !it }).count()

                when {
                    isMemoryLimitExceeded(performanceResult) -> MemoryLimitExceeded(performanceResult.usedMemoryInBytes)
                    failedTestCases > 0 -> JudgeResult.WrongAnswer(testCasesResults)
                    else -> hotRun(clazz, method, problem, testCases)
                }
            }
        } finally {
            executorService.shutdownNow()
        }
    }

    private fun hotRun(clazz: Any, method: Method, problem: Problem, testCases: Array<InternalTestCase>) =
            runPerformanceEvaluation(clazz, method, problem, testCases)

    private fun coldRun(judge: Future<Pair<List<Boolean>, PerformanceResult>>, problem: Problem) =
            judge.get(problem.timeLimit, TimeUnit.SECONDS)

    private fun runPerformanceEvaluation(
            clazz: Any, method: Method, problem: Problem, testCases: Array<InternalTestCase>
    ): JudgeResult {

        val executorService = Executors.newSingleThreadExecutor(threadFactory)
        try {
            var performanceResultFuture = evaluatePerformance(clazz, method, problem, executorService)
            var performanceResult = performanceResultFuture.get(problem.timeLimit, TimeUnit.SECONDS)

            if (isMemoryLimitExceeded(performanceResult)) {
                return MemoryLimitExceeded(performanceResult.usedMemoryInBytes)
            }

            for (i in 0..NUMBER_OF_ITERATIONS - 1) {
                performanceResultFuture = evaluatePerformance(clazz, method, problem, executorService)
                val nextPerformanceResult = performanceResultFuture.get(problem.timeLimit, TimeUnit.SECONDS)

                if (isMemoryLimitExceeded(nextPerformanceResult)) {
                    return MemoryLimitExceeded(nextPerformanceResult.usedMemoryInBytes)
                }

                performanceResult = performanceResult chooseBetterComparingWith nextPerformanceResult
            }

            return Accepted(testCases.size, performanceResult.usedTimeInMs, performanceResult.usedMemoryInBytes)
        } finally {
            executorService.shutdownNow()
        }
    }

    private fun isMemoryLimitExceeded(performanceResult: PerformanceResult): Boolean {
        return performanceResult.usedMemoryInBytes / (1024L * 1024L) > MEMORY_LIMIT_256_MB
    }

    private fun handleFutureRunExceptions(call: () -> JudgeResult) = try {
        call()
    } catch(e: Throwable) {
        when (e) {
            is InterruptedException, is ExecutionException -> RuntimeError(e.message)
            is TimeoutException -> TimeLimitExceeded()
            else -> RuntimeError(e.message)
        }
    }

    private fun evaluatePerformance(clazz: Any, method: Method, problem: Problem, executorService: ExecutorService) =
            executorService.submit(JudgePerformanceTask(clazz, method, readInternalTestCases(problem)))

    private fun compileAndJudge(
            className: Optional<String>,
            compiler: JvmCompiler,
            function: Function,
            problem: Problem,
            userCode: String
    ) = try {

        var classToFound = className.get()
        //if(compiler.programmingLanguage()=="python"){
        //    classToFound += "\$py"
        //}
        println("class name: ${classToFound}")
        val (instance, method) = compiler.compileMethod(classToFound, function.name, function.parameters.size, userCode)
        println("[1]")
        judge(instance, method, problem)
    } catch (e: Throwable) {
        when (e) {
            is ClassNotFoundException -> CompileError("${e.javaClass} : ${e.message}")
            is CompileErrorException -> CompileError(CreateFriendlyMessage().from(e.message!!))
            is NoSuchMethodError -> CompileError("No such method: ${e.message}")
            else -> RuntimeError(e.message)
        }
    }

    private fun findClassName(language: String, sourceCode: String) = when (language) {
        "kotlin" -> sourceCode.findKotlinClassName()
        "python" -> sourceCode.findPythonClassName()
        else -> sourceCode.findJavaClassName()
    }

    private fun readInternalTestCases(problem: Problem): Array<InternalTestCase> {
        val testCases = problem.testCases
        val function = problem.func

        val parser = InternalTestCaseParser(function!!, objectMapper)

        val internalTestCases = testCases!!.indices
                .map { parser.parse(testCases[it]) }
                .toTypedArray()

        return shuffle(internalTestCases)
    }

    private fun shuffle(internalTestCases: Array<InternalTestCase>): Array<InternalTestCase> {
        val internalTestCasesAsList = internalTestCases.asList()
        Collections.shuffle(internalTestCasesAsList)
        return internalTestCasesAsList.toTypedArray()
    }
}
