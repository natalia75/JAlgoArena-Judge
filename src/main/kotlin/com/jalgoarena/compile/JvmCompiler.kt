package com.jalgoarena.compile

import com.jalgoarena.JudgeApplication
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader

interface JvmCompiler {

    fun compileMethod(qualifiedClassName: String, methodName: String, parameterCount: Int, source: String): Pair<Any, Method> {
        println("Qualified class name: ${qualifiedClassName}, Method name: ${methodName}")
        val classBytes = run(qualifiedClassName, source)
        classBytes.keys.forEach { println("key: ${it}") }
        var qClassName = qualifiedClassName
        if(programmingLanguage()=="python"){
            qClassName += "\$py"
        }
        val clazz = Class.forName(qClassName, true, MemoryClassLoader(classBytes)
        )
        clazz.declaredClasses.forEach { println("Class: ${it.name} ${it.modifiers}") }
        clazz.declaredMethods
                .filter { it.name == methodName && it.parameterCount == parameterCount && Modifier.isPublic(it.modifiers)}
                .forEach {
                    try {
                        return Pair(clazz.newInstance(), it)
                    } catch (e: InstantiationException) {
                        LOG.error("Error during creating of new class instance", e)
                        throw IllegalStateException(e.message)
                    } catch (e: IllegalAccessException) {
                        LOG.error("Error during creating of new class instance", e)
                        throw IllegalStateException(e.message)
                    }
                }

        throw NoSuchMethodError(methodName)
    }

    fun programmingLanguage(): String

    fun run(className: String, source: String): MutableMap<String, ByteArray?>

    private class MemoryClassLoader(val classNameToBytecode: MutableMap<String, ByteArray?>)
        : URLClassLoader(arrayOfNulls<URL>(0), JudgeApplication::class.java.classLoader) {

        override fun findClass(className: String): Class<*> {
            val bufferOfBytecode = classNameToBytecode[className]

            return if (bufferOfBytecode == null) {
                super.findClass(className)
            } else {
                clearByteMap(className)
                defineClass(className, bufferOfBytecode, 0, bufferOfBytecode.size)
            }
        }

        private fun clearByteMap(className: String) {
            classNameToBytecode.put(className, null)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(JvmCompiler::class.java)
    }
}
