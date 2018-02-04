package com.jalgoarena.compile
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class JythonCompiler{

    fun compile(path: String) : Int{

        try {
            //Runtime rt = Runtime.getRuntime();
            //String[] commands = {"C:\\jython2.7.0\\bin\\jython.exe", "-m compileall", "D:\\PythonT\\"};
            //Process process = rt.exec(commands);
            val process = Runtime.getRuntime().exec("C:\\jython2.7.0\\bin\\jython.exe -m compileall ${path}")
            val stdInput = BufferedReader(InputStreamReader(process.getInputStream()))
            val stdError = BufferedReader(InputStreamReader(process.getErrorStream()))
            var s: String? = null
            System.out.println("output")
            s = stdInput.readLine()
            while (s != null) {
                System.out.println(s)
                s = stdInput.readLine()
            }
            System.out.println("errors")
            s = stdError.readLine()
            while (s != null) {
                System.out.println(s)
                s = stdError.readLine()
            }
            System.out.println(process.exitValue())
            return process.exitValue()
        } catch (ex: IOException) {
            System.out.println(ex.message)
            return 1
        }
    }
}