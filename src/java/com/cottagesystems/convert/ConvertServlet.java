
package com.cottagesystems.convert;

import japa.parser.ParseException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jbf
 */
@WebServlet(name = "ConvertServlet", urlPatterns = {"/ConvertServlet"})
public class ConvertServlet extends HttpServlet {

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
        
        String code= request.getParameter("code");
        String mode;
        //mode= request.getParameter("mode");
        mode= "edit";
        
        if ( !"edit".equals(mode) ) {
            mode="view";
        }
        
        if ( code==null ) {
            code="";
        }
        
        boolean onlyStatic = "true".equals( request.getParameter("onlyStatic") );
        request.getParameterMap();
        String pythonTarget = request.getParameter("pythonTarget");
        if ( pythonTarget==null ) pythonTarget= PythonTarget.jython_2_2.toString();
                
        response.setContentType("text/html;charset=UTF-8");

        Convert convert= new Convert();
        convert.setOnlyStatic(onlyStatic);
        convert.setPythonTarget(PythonTarget.valueOf(pythonTarget));
        convert.setUnittest( "true".equals( request.getParameter("unittest") ) );
        String jythonCode;
        
        if ( code.trim().length()==0 ) {
            code= "class Simple {\n   public static void main( String[] args ) {\n      System.out.println(\"Hello\");\n   }\n}\n\n\n";
        }
        
        try {
            if (code!=null ) {
                jythonCode = convert.doConvert(code);
            } else {
                jythonCode = "";
                code= "";
            }
        } catch (ParseException ex) {
            jythonCode = "*** "+ex.getMessage()+" ***";
        }
        
        File dd= new File( "/tmp/javajython/");
        if ( !dd.exists() ) {
            if ( !dd.mkdirs() ) throw new IllegalArgumentException("unable to mkdirs");
        }
        String hash= String.format( "%09d", Math.abs( code.hashCode() ) );
        File ff= new File( dd, hash+".java");
        if ( ff.exists() ) {
            ff.delete();
        }
        try (FileOutputStream foa = new FileOutputStream( new File( dd, hash+".java") ) ) {
            foa.write( code.getBytes() );
        }
        ff= new File( dd, hash+".python");
        if ( ff.exists() ) {
            ff.delete();
        }
        try (FileOutputStream foa = new FileOutputStream( new File( dd, hash+".python")) ) {
            foa.write( jythonCode.getBytes() );
        }
        
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Jython Result</title>");            
            out.println("<link rel=\"stylesheet\" href=\"styles/default.min.css\">");
            out.println("</head>");
            out.println("<body>");
            out.println("<script language=\"javascript\" src=\"highlight.min.js\"></script>\n");
            out.println("<script>hljs.highlightAll();</script>\n");
            out.println("<h1>Java to Python Converter</h1>");
            out.println("Please read caveats below, seriously difficult bugs could be introduced when automatically converting code.<br><br>");
            out.println("<form action=\"ConvertServlet\" method=\"post\">");
            out.println("<table>");
            out.println("<tr>");
            out.println("<td>Java Code:<br>");
            if ( "edit".equals(mode) ) {
                out.println("<textarea rows=\"40\" cols=\"80\" id=\"code\" name=\"code\">"+code+"</textarea>");
            } else {
                out.println("<div style=\"height: 400px; width: 600px; overflow: scroll\"><pre><code class=\"language-java\" id=\"code\" name=\"code\" rows=\"40\" >"+code+"</code></pre></div>");
            }
            out.println("</td>");
            out.println("<td valign='top'>Jython Code:<br>");
            if ( "edit".equals(mode) ) {
                out.println("<textarea rows=\"40\" cols=\"132\">"+jythonCode+"</textarea>");
            } else {
                out.println("<div style=\"height: 400px; width: 600px; overflow: scroll\"><pre><code class=\"language-python\">"+jythonCode+"</code></pre></div>");
            }
            out.println("</td>");
            out.println("</tr></table>");
            
            if (  "edit".equals(mode) ) {            
                out.println( String.format( "<input type=\"checkbox\" id=\"onlyStatic\" name=\"onlyStatic\" value=\"true\" %s>Only Static Parts</input>",
                        convert.isOnlyStatic() ? "checked" : "" ) );
                out.println( String.format( "<input type=\"checkbox\" id=\"unittest\" name=\"unittest\" value=\"true\" %s>Unit Test</input>",
                        convert.isUnittest() ? "checked" : "" ) );
                out.println("<select name=\"pythonTarget\" id=\"pythonTarget\">");
                out.println(String.format( "    <option value=\"jython_2_2\" %s>Jython 2.2</option>", 
                    ( convert.getPythonTarget()==PythonTarget.jython_2_2 ? "selected=1" : ""  ) ) );
                out.println(String.format( "    <option value=\"python_3_6\" %s>Python 3.6</option>", 
                    ( convert.getPythonTarget()==PythonTarget.python_3_6 ? "selected=1" : ""  ) ) );
                out.println("</select>");
                out.println("<button id=\"clear\" value=\"clear\" onclick=\"javascript:document.getElementById('code').value=''\">Clear</button>");
                out.println("<input type=\"submit\" value=\"submit\"></input>");
            } else {
                out.println( convert.isOnlyStatic() ? "Only Static Parts" : "Not Only Static Parts" );
                out.println( "," );
                out.println( convert.isUnittest() ? "Unit Test" : "Not unit test" );
                out.println( "," );
                out.println( convert.getPythonTarget() );
                out.println("<input name=\"mode\" type=\"hidden\" value=\"edit\"></input><input type=\"submit\" value=\"edit\"></input>");
            }
            out.println("</form action=\"ConvertServlet\" method=\"post\">");        
            out.println("<small>Version 20221101a</small><br>\n");
            out.println("Please note:<ul>\n");
            out.println("<li>The goal is to get something close to translated, but not perfect.\n");
            out.println("<li>The Java code must be working, this assumes that it is a functioning and correct code.\n");
            out.println("<li>Semmantics are considered, for example s.substring is assumed to be using the substring method of string.\n");
            out.println("<li>Other assumptions like xx.length -> len(xx) are made.\n");
            out.println("<li>Some Java class use may remain, and you will need to find translations to Python.\n");
            out.println("<li>Single methods are handled by wrapping the method with a class, then this is removed.\n");
            out.println("<li>Several statements are made into a method similarly.\n");
            out.println("<li>This currently targets Jython 2.2 and Python 3.6.\n");
            out.println("<li>I am a Java developer who knows enough Python to cause problems, see <a href='https://github.com/jbfaden/JavaJythonConverter'>GitHub project</a> to provide feedback\n");
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

}
