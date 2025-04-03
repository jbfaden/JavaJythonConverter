<%-- 
    Document   : TranslateJava
    Created on : Sep 30, 2022, 12:18:38 PM
    Author     : jbf
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <h1>Java Translator to Jython</h1>
        
        <form action="ConvertServlet" method="post">
            Java expression, statement block, or code:<br>
            <textarea rows="40" cols="132" name="code">x = Math.pow(4,2)</textarea>
            <input type="submit" value="submit"></input>
        </form>
            
    </body>
</html>
