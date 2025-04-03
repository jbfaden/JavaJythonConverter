
package com.cottagesystems.convert;

import com.github.javaparser.ParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import test.FibExample;

/**
 *
 * @author jbf
 */
@WebServlet(name = "ConvertJavaToIDLServlet", urlPatterns = {"/ConvertJavaToIDLServlet"})
public class ConvertJavaToIDLServlet extends HttpServlet {

    private static String getProcessId(final String fallback) {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return fallback;
        }

        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            // ignore
        }
        return fallback;
    }

    private static String pid= getProcessId("000");

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //String sample= request.getParameter("examples");

        String code= request.getParameter("code");

        //if ( sample!=null && sample.endsWith(".java" ) ) {
        //    code= loadExample(sample);
        //}

        String mode;
        //mode= request.getParameter("mode");
        mode= "edit";

        if ( !"edit".equals(mode) ) {
            mode="view";
        }

        if ( code==null ) {
            code="";
        }

        String sonlyStatic= request.getParameter("onlyStatic");
        boolean onlyStatic = ( sonlyStatic==null ) ? false : ( "true".equals( sonlyStatic ) );
        request.getParameterMap();
        //String pythonTarget = request.getParameter("pythonTarget");
        //if ( pythonTarget==null ) pythonTarget= PythonTarget.jython_2_2.toString();

        response.setContentType("text/html;charset=UTF-8");

        ConvertJavaToIDL convert= new ConvertJavaToIDL();
        convert.setOnlyStatic(onlyStatic);
        //convert.setPythonTarget(PythonTarget.valueOf(pythonTarget));
        convert.setUnittest( "true".equals( request.getParameter("unittest") ) );
        convert.setCamelToSnake( "true".equals( request.getParameter("camelToSnake") ));

        String idlCode;

        if ( code.trim().length()==0 ) {
            // code= "class Simple {\n   public static void main( String[] args ) {\n      System.out.println(\"Hello\");\n   }\n}\n\n\n";
        }

        try {
            idlCode = convert.doConvert(code);
        } catch (ParseException ex) {
            idlCode = "*** "+ex.getMessage()+" ***";
        }


        File dd= new File( "/tmp/javaidl/"+pid+"/");
        if ( !dd.exists() ) {
            File pp= dd.getParentFile();
            if ( !pp.exists() ) {
                pp.mkdirs();
                //pp.setWritable( true, false );  // I don't think this works
            }
            if ( !dd.mkdir() ) throw new IllegalArgumentException("unable to mkdir: "+dd);
        }
        String hash= String.format( "%09d", Math.abs( code.hashCode() ) );
        File ff= new File( dd, hash+".java");
        if ( ff.exists() ) {
            ff.delete();
        }
        try (FileOutputStream foa = new FileOutputStream( new File( dd, hash+".java") ) ) {
            foa.write( code.getBytes() );
        }
        ff= new File( dd, hash+".pro");
        if ( ff.exists() ) {
            ff.delete();
        }
        try (FileOutputStream foa = new FileOutputStream( new File( dd, hash+".js")) ) {
            foa.write( idlCode.getBytes() );
        }

        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Java to IDL Converter</title>");
            out.println("<link rel=\"stylesheet\" href=\"styles/default.min.css\">");
            out.println("</head>");
            out.println("<body>");
            out.println("<script language=\"idl\" src=\"highlight.min.js\"></script>\n");
            out.println("<script>hljs.highlightAll();</script>\n");
            out.println("<script language=\"idl\" src=\"copytext.js\"></script>\n");
            out.println("<h1>Java to IDL Converter</h1>");
            out.println("Please read caveats below, seriously difficult bugs could be introduced when automatically converting code.<br><br>");
            out.println("<form action=\"ConvertJavaToIDLServlet\" method=\"post\">");
            out.println("<table>");
            out.println("<tr>");
            out.println("<td>Java Code:<br>");
            if ( "edit".equals(mode) ) {
                out.println("<textarea rows=\"40\" cols=\"80\" id=\"code\" name=\"code\">"+code+"</textarea>");
            } else {
                out.println("<div style=\"height: 400px; width: 600px; overflow: scroll\"><pre><code class=\"language-java\" id=\"code\" name=\"code\" rows=\"40\" >"+code+"</code></pre></div>");
                out.println("<textarea rows=\"40\" cols=\"80\" id=\"code\" name=\"code\" hidden=\"true\">"+code+"</textarea>");
            }
            out.println("</td>");
            out.println("<td valign='top'>IDL Code:<br>");
            if ( "edit".equals(mode) ) {
                out.println("<textarea rows=\"40\" cols=\"132\" id=\"outputcode\">"+idlCode+"</textarea>");
            } else {
                out.println("<div style=\"height: 400px; width: 600px; overflow: scroll\"><pre><code >"+idlCode+"</code></pre></div>");
            }
            out.println("<button onclick=\"copytext()\">Copy IDL</button>");
            out.println("</td>");
            out.println("</tr></table>");

            if (  "edit".equals(mode) ) {
                out.println( String.format( "<input type=\"checkbox\" id=\"onlyStatic\" name=\"onlyStatic\" value=\"true\" %s>Only Static Parts</input>",
                        convert.isOnlyStatic() ? "checked" : "" ) );
                out.println( String.format( "<input type=\"checkbox\" id=\"unittest\" name=\"unittest\" value=\"true\" %s>Unit Test</input>",
                        convert.isUnittest() ? "checked" : "" ) );
                out.println( String.format( "<input type=\"checkbox\" id=\"camelToSnake\" name=\"camelToSnake\" value=\"true\" %s>Camel to Snake</input>",
                        convert.isCamelToSnake() ? "checked" : "" ) );
                //out.println("<select name=\"pythonTarget\" id=\"pythonTarget\">");
                //out.println(String.format( "    <option value=\"jython_2_2\" %s>Jython 2.2</option>",
                //    ( convert.getPythonTarget()==PythonTarget.jython_2_2 ? "selected=1" : ""  ) ) );
                //out.println(String.format( "    <option value=\"python_3_6\" %s>Python 3.6</option>",
                //    ( convert.getPythonTarget()==PythonTarget.python_3_6 ? "selected=1" : ""  ) ) );
                out.println("</select>");
                out.println("<button id=\"clear\" value=\"clear\" onclick=\"javascript:document.getElementById('code').value=''\">Clear</button>");
                out.println("<input type=\"submit\" value=\"submit\"></input>\n");
                //addExamples(out);
            } else {
                out.println( convert.isOnlyStatic() ? "Only Static Parts" : "Not Only Static Parts" );
                out.println( "," );
                out.println( convert.isUnittest() ? "Unit Test" : "Not unit test" );
                out.println( "," );
                //out.println( convert.isCamelToSnake() ? "camel_to_snake" : "Not camelToSnake" );
                //out.println( "," );
                //out.println( convert.getPythonTarget() );
                out.println("<input name=\"mode\" value=\"edit\" hidden=\"true\"></input><input type=\"submit\" value=\"edit\"></input>");            }
            out.println("</form>");
            out.println("Please note:<ul>\n");
            out.println("<li>The goal is to get something close to translated, but not perfect.\n");
            out.println("<li>The Java code must be working, this assumes that it is a functioning and correct code.\n");
            out.println("<li>Semmantics are considered, for example s.substring is assumed to be using the substring method of string.\n");
            out.println("<li>Some Java class use may remain, and you will need to find translations to IDL.\n");
            out.println("<li>Single methods are handled by wrapping the method with a class, then this is removed.\n");
            out.println("<li>Several statements are made into a method similarly.\n");
            out.println("<li>I am a Java developer with limited time, see <a href='https://github.com/jbfaden/JavaJythonConverter'>GitHub project</a> to provide feedback\n");
            out.println("<hr>");
            out.println("<a href='index.jsp'>Home</a>");
            out.println("<small>"+ConvertJavaToIDL.VERSION+"</small><br>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    /**
     * this doesn't work...  TODO: make work
     * @param s
     * @return
     * @throws IOException
     */
    private String loadExample(String s) throws IOException {
        StringBuilder b= new StringBuilder();
        InputStream ibs= FibExample.class.getResourceAsStream("https://raw.githubusercontent.com/jbfaden/JavaJythonConverter/main/src/java/test/"+s);
        BufferedReader read = new BufferedReader( new InputStreamReader( ibs ) );
        String line;
        while ( ( line= read.readLine() )!=null ) {
            b.append(line);
        }
        String prog= b.toString();
        return prog;
    }

    private void addExamples(PrintWriter out) {
        String[] exs= {"SwitchStatement.java"};
        out.println("<select name=\"examples\" id=\"examples\">\n");
        out.println("   <option value=\"\">Select example...</option>\n");
        for ( String s: exs ) {
            out.println("   <option value=\""+s+"\">"+s+"</option>\n");
        }
        out.println("</select>\n");
    }

}
