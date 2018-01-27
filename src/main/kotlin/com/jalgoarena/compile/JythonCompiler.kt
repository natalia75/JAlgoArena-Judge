class JythonCompiler{

    fun compile() : int{

        try {
            //Runtime rt = Runtime.getRuntime();
            //String[] commands = {"C:\\jython2.7.0\\bin\\jython.exe", "-m compileall", "D:\\PythonT\\"};
            //Process process = rt.exec(commands);
            val process = Runtime.getRuntime().exec("C:\\jython2.7.0\\bin\\jython.exe -m compileall D:\\PythonT\\")
            val stdInput = BufferedReader(InputStreamReader(process.getInputStream()))
            val stdError = BufferedReader(InputStreamReader(process.getErrorStream()))
            var s: String? = null
            System.out.println("output")
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s)
            }
            System.out.println("errors")
            while ((s = stdError.readLine()) != null) {
                System.out.println(s)
            }
            System.out.println(process.exitValue())

        } catch (ex: IOException) {
            System.out.println(ex.getMessage())
        }


    }

}