<%-- 
    Document   : index.jsp
    Created on : Sep 30, 2022, 5:50:25 PM
    Author     : jbf
--%>

<%@page import="com.cottagesystems.convert.ConvertJavaToPython"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Java to Python Converter</title>
    </head>
    <body>
        <h1>Java to Python Converter</h1>
    <sm>version <%=ConvertJavaToPython.VERSION%></sm><br><br>
        
        This was motivated when I couldn't find a satisfactory Java to Jython
        converter online, so I found a Java AST and wrote a Jython-generating
        code using it.
        
        <p>This uses javaparser, found at <a href="https://github.com/javaparser/javaparser">https://github.com/javaparser/javaparser</a>
        and using a jar file from Maven Central.</p>
        
        <a href='ConvertJavaToPythonServlet'>Convert Java to Python</a><br>
        <a href='ConvertJavaToJavascriptServlet'>Convert Java to Javascript</a>
        <p>There are other codes which do the same thing, but not on-line, which
            are useful references, and should be considered as well:<ul>
            <li><a href="https://github.com/natural/java2python">https://github.com/natural/java2python</a> which is implemented in Python
        </ul>
        
        I'd still like for the editor to have syntax highliting. See 
            <a href="https://css-tricks.com/creating-an-editable-textarea-that-supports-syntax-highlighted-code/">https://css-tricks.com/creating-an-editable-textarea-that-supports-syntax-highlighted-code/</a>
        for how this might be done.

        

        </p>
    </body>
</html>
