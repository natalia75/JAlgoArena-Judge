package com.jalgoarena.codegeneration

import com.jalgoarena.domain.Function
import kotlin.reflect.KClass

class PythonCodeGenerator : JvmCodeGeneration {
    override fun programmingLanguage() = "python"

    override fun generateEmptyFunction(function: Function) = """class Solution:
    ${functionComment(function)}
    ${functionDeclaration(function)}
        // Write your code here"""

    override fun functionComment(function: Function): String {
        var retVal = "${parametersComment(function)}"
        retVal += System.lineSeparator()
        retVal += "//@return ${function.returnStatement.comment}"
        return retVal
    }

    override fun parametersComment(function: Function): String {
        return function.parameters.map { parameter ->
            "//@param ${parameter.name} ${parameter.comment}"
        }.joinToString(System.lineSeparator())
    }

    override fun generateParameterDeclaration(type: String, parameterName: String, generic: String?) =
            "$parameterName"

    private fun functionDeclaration(function: Function): String =
            "def ${function.name}(self, ${parametersOf(function)}):"
}
