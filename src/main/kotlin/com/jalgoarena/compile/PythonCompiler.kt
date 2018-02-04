package com.jalgoarena.compile
import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*
import org.slf4j.LoggerFactory

class PythonCompiler : JvmCompiler {
    override fun programmingLanguage() = "python"
    private val LOG=LoggerFactory.getLogger(this.javaClass)
    override fun run(className: String, source: String): MutableMap<String, ByteArray?> {
        println("PYTHON")
        val tmpDir = createTmpDir()
        val origErr = System.err
        LOG.info(">>> tmpDir: ${tmpDir.absolutePath}")
        try {
            val sourceFile = writeSourceFile(tmpDir, "$className.py", source)
            val out = File(tmpDir, "out")
            out.mkdirs()

            val errMessageBytes = ByteArrayOutputStream()
            System.setErr(PrintStream(errMessageBytes))

            when (compileAndReturnExitCode(tmpDir, sourceFile)) {
                ExitCode.OK -> return readClassBytes(tmpDir)
                else -> throw CompileErrorException(errMessageBytes.toString("utf-8"))
            }
        } finally {
            //tmpDir.deleteRecursively()
            System.setErr(origErr)
        }
    }

    private fun readClassBytes(out: File): MutableMap<String, ByteArray?> {
        val classBytes: MutableMap<String, ByteArray?> = HashMap()
        out.listFiles().forEach { LOG.info(">>> File: ${it.name}") }
        out.listFiles()
                .filter { it.absolutePath.endsWith(".class") }
                .forEach {
                    val byteCode = File(out, it.name).readBytes()
                    classBytes.put(it.nameWithoutExtension, byteCode)
                }

        return classBytes
    }

    private fun compileAndReturnExitCode(sourceFile: File, path: File): ExitCode {
    var exitCode = 0 
        try {
            val jCompiler = JythonCompiler()
            exitCode = jCompiler.compile(path.absolutePath)
            LOG.info(">>> ExitCode: ${exitCode}")
        } catch (e: Exception) {
            return ExitCode.COMPILATION_ERROR
        }

        if(exitCode==0){
            return ExitCode.OK
        }
        return ExitCode.COMPILATION_ERROR
    }

    private fun processToExitCode(process: Process): ExitCode {
        return when (process.exitValue()) {
            0 -> ExitCode.OK
            1 -> ExitCode.COMPILATION_ERROR
            3 -> ExitCode.SCRIPT_EXECUTION_ERROR
            else -> {
                ExitCode.INTERNAL_ERROR
            }
        }
    }

    private fun createTmpDir(): File {
        val tmpDir = File("tmp", "${UUID.randomUUID()}")
        tmpDir.mkdirs()
        return tmpDir
    }

    private fun writeSourceFile(tmpDir: File, fileName: String, sourceCode: String): File {
        val sourceFile = File(tmpDir, fileName)
        sourceFile.writeText(sourceCode)
        return sourceFile
    }
}