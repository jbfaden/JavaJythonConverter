
package com.cottagesystems.convert;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to verify syntax highliting.
 * @author jbf
 */
@WebServlet(name = "SyntaxTestServlet", urlPatterns = {"/SyntaxTestServlet"})
public class SyntaxTestServlet extends HttpServlet {

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
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<html>\n" +
"    <head>\n" +
"        <link rel=\"stylesheet\" href=\"styles/default.min.css\">\n" +
"    </head>\n" +
"        \n" +
"    <body>\n" +
"        \n" +
"        <script language=\"javascript\" src=\"highlight.min.js\"></script>\n" +
"        <script>hljs.highlightAll();</script>\n" +
"        \n" +
"        <table>\n" +
"            <tr>\n" +
"            <td>\n" +
"        <pre><code class=\"language-java\">\n" +
"package test;\n" +
"\n" +
"/**\n" +
" * Demo foreach statement\n" +
" * @author jbf\n" +
" */\n" +
"public class ForeachDemo {\n" +
"    public static void main( String[] args ) {\n" +
"        for ( String s : new String[] { \"a\", \"b\", \"c\" } ) {\n" +
"            System.err.println(s);\n" +
"        }\n" +
"    }\n" +
"}\n" +
"</code>\n" +
"        </pre>\n" +
"            </td>\n" +
"            <td>\n" +
"        <pre><code class=\"language-python\">import sys\n" +
"\n" +
"\n" +
"# Demo foreach statement\n" +
"# @author jbf\n" +
"class ForeachDemo:\n" +
"    @staticmethod\n" +
"    def main(args):\n" +
"        for s in [ 'a','b','c' ]:\n" +
"            sys.stderr.write(s+'\\n')\n" +
"\n" +
"\n" +
"ForeachDemo.main([])\n" +
"</code>\n" +
"        </pre>\n" +
"            </td>                \n" +
"            </tr>\n" +
"                \n" +
"            </table>\n" +
"    </body>\n" +
"        \n" +
"</html>\n" +
"");

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
