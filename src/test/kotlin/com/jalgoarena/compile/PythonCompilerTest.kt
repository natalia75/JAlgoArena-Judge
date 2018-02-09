package com.jalgoarena.compile
import com.jalgoarena.compile.PythonCompiler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PythonCompilerTest {
    @Test
    fun compileAndRunMethod(){
        val(instance, method) = PythonCompiler().compileMethod("Solution", "power2", 1, POWER2_SOURCE_CODE)
        val result = method.invoke(instance, 4)
        assertThat(result).isEqualTo(16)
    }
    companion object {
        private val POWER2_SOURCE_CODE = """class Solution:
    def power2(value):
        return value*value"""
    }
}