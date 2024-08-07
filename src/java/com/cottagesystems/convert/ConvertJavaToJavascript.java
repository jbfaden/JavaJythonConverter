
package com.cottagesystems.convert;

import static com.cottagesystems.convert.ConvertJavaToPython.stringMethods;
import japa.parser.ASTHelper;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.Node;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EmptyMemberDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.InitializerDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.MultiTypeParameter;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.comments.Comment;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.ConditionalExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.EnclosedExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.MarkerAnnotationExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.SuperExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.CatchClause;
import japa.parser.ast.stmt.EmptyStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.ForeachStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.stmt.SwitchEntryStmt;
import japa.parser.ast.stmt.SwitchStmt;
import japa.parser.ast.stmt.ThrowStmt;
import japa.parser.ast.stmt.TryStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author jbf
 */
public class ConvertJavaToJavascript {

    public ConvertJavaToJavascript() {
        this.stack = new Stack<>();
        this.stackFields= new Stack<>();
        this.stackMethods= new Stack<>();
        this.stackClasses= new Stack<>();
        this.localVariablesStack = new Stack<>();
        
        this.stack.push( new HashMap<>() );
        this.stackFields.push( new HashMap<>() );
        this.stackMethods.push( new HashMap<>() );
        this.stackClasses.push( new HashMap<>() );
        this.localVariablesStack.push( new HashMap<>() );
    }

    
    private static String s4="    ";
    
    public String doConvert( String javasrc ) throws ParseException {
        ParseException throwMe;
        try {
            ByteArrayInputStream ins= new ByteArrayInputStream( javasrc.getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= japa.parser.JavaParser.parse(ins,"UTF-8");
            String src= doConvert( "", unit );
            if ( !additionalImports.isEmpty() ) {
                StringBuilder additionalImportsSrc= new StringBuilder();
                for ( Entry<String,Boolean> ent : additionalImports.entrySet() ) {
                    if ( ent.getValue() ) {
                        additionalImportsSrc.append( ent.getKey() );
                    }
                }
                src= additionalImportsSrc.toString() + src;
            }
            if ( !additionalClasses.isEmpty() ) {
                StringBuilder additionalClassesSrc= new StringBuilder();
                for ( Entry<String,Boolean> ent : additionalClasses.entrySet() ) {
                    if ( ent.getValue() ) {
                        additionalClassesSrc.append( ent.getKey() );
                    }
                }
                src=  additionalClassesSrc.toString() + src;
            }
            
            if ( hasMain ) {
                StringBuilder sb = new StringBuilder(src);
                sb.append( theClassName ).append(".main([])\n");
                src= sb.toString();
            }
            
            return src;
        } catch ( ParseException ex ) {
            throwMe= ex;
        }
        
        
        int numLinesIn=0;
        
        try {
            String[] lines= javasrc.split("\n");
            numLinesIn= lines.length;
            int offset=0;
            Expression parseExpression = japa.parser.JavaParser.parseExpression(javasrc);
            StringBuilder bb= new StringBuilder( doConvert( "", parseExpression ) );
            int linesHandled=0;
            while ( (linesHandled+parseExpression.getEndLine())<lines.length ) {
                int additionalLinesHandled=0;
                for ( int i=0; i<parseExpression.getEndLine(); i++ ) {
                    offset += lines[i].length() ;
                    offset += 1;
                    additionalLinesHandled++;
                }
                if ( offset>javasrc.length() ) {
                    // something went wrong...
                    break;
                }
                linesHandled+= additionalLinesHandled;
                parseExpression = japa.parser.JavaParser.parseExpression(javasrc.substring(offset));
                bb.append( "\n" ).append( doConvert( "", parseExpression ) );
            }
            String src= bb.toString();

            StringBuilder sb= new StringBuilder();
            for ( Map.Entry<String,Boolean> e: additionalImports.entrySet() ) {
                if ( e.getValue() ) {
                    sb.append( e.getKey() );
                }
            }

            for (  Map.Entry<String,Boolean> e: additionalClasses.entrySet() ) {
                if ( e.getValue() ) {
                    sb.append( e.getKey() );
                }
            }
            sb.append(src);
            
            if ( hasMain ) {
                sb.append( theClassName ).append(".main([])\n");
            }
            
            src= sb.toString();

            return src;

        } catch (ParseException ex1) {
            throwMe= ex1;
        }
        
        try {
            if ( numLinesIn < 2 ) {
                Statement parsed = japa.parser.JavaParser.parseStatement(javasrc);
                return doConvert("", parsed);
            }
        } catch (ParseException ex2) {
            throwMe= ex2;
        }

        try {
            if ( numLinesIn < 2 ) {
                BodyDeclaration parsed = japa.parser.JavaParser.parseBodyDeclaration(javasrc);
                return doConvert("", parsed);
            }
        } catch (ParseException ex3 ) {
            throwMe= ex3;
        }

        try {
            Statement parsed = japa.parser.JavaParser.parseBlock(javasrc);
            return doConvert("", parsed);
        } catch ( ParseException ex ) {
            throwMe =ex;
        }
        
        try {
            String ssrc= utilMakeClass(javasrc);
            ByteArrayInputStream ins= new ByteArrayInputStream( ssrc.getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= japa.parser.JavaParser.parse(ins,"UTF-8");
            String src= doConvert( "", unit );
            src= utilUnMakeClass(src);
            
            if ( additionalImports!=null ) {
                StringBuilder sb= new StringBuilder();
                
                for ( Map.Entry<String,Boolean> e: additionalImports.entrySet() ) {
                    if ( e.getValue() ) {
                        sb.append( e.getKey() );
                    }
                }

                for (  Map.Entry<String,Boolean> e: additionalClasses.entrySet() ) {
                    if ( e.getValue() ) {
                        sb.append( e.getKey() );
                    }
                }
                sb.append(src);
                src= sb.toString();
            }
            return src;
        } catch ( ParseException ex ) {
            throwMe= ex;
        }

        throw throwMe;
                
    }

    /**
     * return the node as Jython/Python code, with each line indented with "indent"
     * @param indent whitespace to prefix each statement.  Note some blocks are converted with a comment prefix for reference.
     * @param n the node, which might be an expression, or a block of statements, or an entire program.
     * @return the Jython/Python code converted as much as possible.
     */
    private String doConvert( String indent, Node n ) { 
        if ( n==null ) {
            throw new IllegalArgumentException("n cannot be null.");
        }
        if ( n.getClass()==null ) {
            throw new IllegalArgumentException("no class");
        }
        String simpleName= n.getClass().getSimpleName();

        
        String result="<J2J243 "+simpleName+">";

//        if ( simpleName.equals("NameExpr") && n.toString().contains("DAY_OFFSET") ) {
//            System.err.println("here");
//        }
//        if ( simpleName.equals("FieldDeclaration") && n.toString().contains("DAY_OFFSET") ) {
//            System.err.println("here");
//        }
        switch ( simpleName ) {
            case "CompilationUnit":
                result= doConvertCompilationUnit(indent,(CompilationUnit)n);
                break;
            case "AssignExpr":
                result= doConvertAssignExpr(indent,(AssignExpr)n);
                break;
            case "BinaryExpr":
                result= doConvertBinaryExpr(indent,(BinaryExpr)n);
                break;
            case "NameExpr":
                result= doConvertNameExpr(indent,(NameExpr)n);
                break;
            case "EnclosedExpr": 
                result= indent + "(" + doConvert( "", ((EnclosedExpr)n).getInner() ) + ")";
                break;
            case "NullLiteralExpr":
                result= indent + "null";
                break;
            case "BooleanLiteralExpr":
                result= indent + ( ((BooleanLiteralExpr)n).getValue() ? "true" : "false" );
                break;
            case "LongLiteralExpr":
                String slong= ((LongLiteralExpr)n).getValue();
                result= indent + slong.substring(0,slong.length()-1);
                break;
            case "IntegerLiteralExpr":
                result= indent + ((IntegerLiteralExpr)n).getValue();
                break;
            case "DoubleLiteralExpr":
                String s = ((DoubleLiteralExpr)n).getValue();
                if ( s.endsWith("d") || s.endsWith("D") || s.endsWith("f") || s.endsWith("F") ) {
                    s= s.substring(0,s.length()-1);
                }
                result= indent + s;
                break;
            case "CharLiteralExpr":
                result= indent + "'" + ((CharLiteralExpr)n).getValue() +"'";
                break;
            case "CastExpr":
                result= doConvertCastExpr(indent,(CastExpr)n);
                break;
            case "MethodCallExpr":
                result= doConvertMethodCallExpr(indent,(MethodCallExpr)n);
                break;
            case "StringLiteralExpr":
                result= doConvertStringLiteralExpr(indent,(StringLiteralExpr)n);
                break;
            case "ConditionalExpr":
                result= doConvertConditionalExpr(indent,(ConditionalExpr)n);
                break;
            case "UnaryExpr":
                result= doConvertUnaryExpr(indent,(UnaryExpr)n);
                break;
            case "BodyDeclaration":
                result= "<Body Declaration>";
                break;
            case "BlockStmt":
                // it's important that the block statement should indented by the caller, and it will not add more.
                result= doConvertBlockStmt(indent,(BlockStmt)n);
                break;
            case "ExpressionStmt":
                result= doConvertExpressionStmt(indent,(ExpressionStmt)n);
                break;
            case "VariableDeclarationExpr":
                result= doConvertVariableDeclarationExpr(indent,(VariableDeclarationExpr)n);
                break;
            case "IfStmt":
                result= doConvertIfStmt(indent,(IfStmt)n);
                break;
            case "ForStmt":
                result= doConvertForStmt(indent,(ForStmt)n);
                break;
            case "WhileStmt":
                result= doConvertWhileStmt(indent,(WhileStmt)n);
                break;
            case "SwitchStmt":
                result= doConvertSwitchStmt(indent,(SwitchStmt)n);                
                break;
            case "ReturnStmt":
                result= doConvertReturnStmt(indent,(ReturnStmt)n);
                break;
            case "BreakStmt":
                result= indent + "break";
                break;
            case "ContinueStmt":
                result= indent + "continue";
                break;
            case "TryStmt":
                result= doConvertTryStmt(indent,(TryStmt)n);
                break;
            case "ReferenceType":
                result= doConvertReferenceType(indent,(ReferenceType)n);
                break;
            case "MultiTypeParameter":
                result= doConvertMultiTypeParameter(indent,(MultiTypeParameter)n);
                break;
            case "ThrowStmt":
                result= doConvertThrowStmt(indent,(ThrowStmt)n);
                break;
            case "ArrayCreationExpr":
                result= doConvertArrayCreationExpr(indent,(ArrayCreationExpr)n);
                break;
            case "ArrayInitializerExpr":
                result= doConvertArrayInitializerExpr(indent,(ArrayInitializerExpr)n);
                break;
            case "ArrayAccessExpr":
                result= doConvertArrayAccessExpr(indent,(ArrayAccessExpr)n);
                break;
            case "FieldAccessExpr":
                result= doConvertFieldAccessExpr(indent,(FieldAccessExpr)n);
                break;
            case "ThisExpr":
                result= "this";
                break;
            case "ImportDeclaration":
                //result= doConvertImportDeclaration(indent,(ImportDeclaration)n);
                break;
            case "PackageDeclaration":
                result= "";
                break;
            case "FieldDeclaration":
                result= doConvertFieldDeclaration(indent,(FieldDeclaration)n);
                break;
            case "MethodDeclaration":
                result= doConvertMethodDeclaration(indent,(MethodDeclaration)n);
                break;
            case "ClassOrInterfaceDeclaration":
                result= doConvertClassOrInterfaceDeclaration(indent,(ClassOrInterfaceDeclaration)n);
                break;
            case "ClassOrTypeInterfaceType":
                //result= doConvertClassOrInterfaceType(indent,(ClassOrInterfaceType)n); // TODO: this looks suspicious
                break;
            case "ConstructorDeclaration":
                result= doConvertConstructorDeclaration(indent,(ConstructorDeclaration)n);
                break;
            case "EnumDeclaration":
                result= doConvertEnumDeclaration(indent,(EnumDeclaration)n);
                break;
            case "InitializerDeclaration":
                //result= doConvertInitializerDeclaration(indent,(InitializerDeclaration)n);
                break;
            case "ObjectCreationExpr":
                result= doConvertObjectCreationExpr(indent,(ObjectCreationExpr)n);
                break;
            case "ClassOrInterfaceType":
                result= doConvertClassOrInterfaceType(indent,(ClassOrInterfaceType)n);
                break;
            case "Parameter":
                result= indent + ((Parameter)n).getId().getName(); // TODO: varargs, etc
                break;
            case "ForeachStmt":
                result= doConvertForeachStmt(indent,(ForeachStmt)n);
                break;
            case "EmptyStmt":
                result= doConvertEmptyStmt(indent,(EmptyStmt)n);
                break;
            case "VariableDeclaratorId":
                result= indent + ((VariableDeclaratorId)n).getName();
                break;
            case "SuperExpr":
                SuperExpr se= (SuperExpr)n;
                result= indent + "super()";
                break;
            default:
                result= indent + "*** "+simpleName + "*** " + n.toString() + "*** end "+simpleName + "****";
                break;
        }
        //if ( result.contains("join") ) {
        //    System.err.println("Here stop");
        //}
        return result;
    }
    
    private String doConvertCompilationUnit(String indent, CompilationUnit compilationUnit) {
        
        pushScopeStack(false);

        StringBuilder sb= new StringBuilder();
        
        List<Node> nodes= compilationUnit.getChildrenNodes();
        for ( int i=0; i<nodes.size(); i++ ) {
            Node n = nodes.get(i);
            if ( unittest && n instanceof ImportDeclaration ) {
                if ( ((ImportDeclaration)n).toString().startsWith("import org.junit") ) { // TODO: cheesy
                    continue;
                }
                if ( ((ImportDeclaration)n).toString().startsWith("import static org.junit") ) { // TODO: also cheesy
                    continue;
                }
            }
            if ( n instanceof ImportDeclaration ) {
                javaImports.put(((ImportDeclaration)n).getName().getName(), false );
                javaImportDeclarations.put(((ImportDeclaration)n).getName().getName(), (ImportDeclaration)n );
            } else {
                sb.append( doConvert( "", n ) ).append("\n");
            }
        }
        
        //for ( Comment c: compilationUnit.getComments() ) {
        //    sb.append("# ").append(c.getContent()).append("\n");
        //}
        
        popScopeStack();
        
        return sb.toString();
    }
    
    public static void main(String[] args ) throws ParseException, FileNotFoundException, MalformedURLException, IOException {
        ConvertJavaToJavascript c= new ConvertJavaToJavascript();
        //c.setOnlyStatic(false);
        //c.setUnittest(false);
//        System.err.println("----");
//        System.err.println(c.doConvert("{ int x= Math.pow(3,5); }"));
//        System.err.println("----");
//        System.err.println("----");
//        System.err.println(c.doConvert("{ int x=0; if (x>0) y=0; }"));
//        System.err.println("----");
//        System.err.println("----");
//        System.err.println(c.doConvert("x=3"));
//        System.err.println("----");
//        System.err.println("----");
//        System.err.println(c.doConvert("Math.pow(3,c)"));
//        System.err.println("----");
//        System.err.println("----");
//        System.err.println(c.doConvert("3*c"));
//        System.err.println("----");
//        System.err.println("----");
//        System.err.println(c.doConvert("\"apple\".subString(3)"));
//        System.err.println("----");

        //InputStream ins= new URL( "https://raw.githubusercontent.com/jbfaden/JavaJythonConverter/main/src/java/test/ForDemo.java" ).openStream();
        InputStream ins= new URL( "https://raw.githubusercontent.com/jbfaden/JavaJythonConverter/main/src/java/test/FibExample.java" ).openStream();
        String text = new BufferedReader(
            new InputStreamReader(ins, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        
        System.err.println( c.doConvert(text) );
    }
    
    /**
     * wrap the methods in a dummy class to see if it compiles.  There's a goofy
     * this with this parser where I can't figure out how to get it to compile
     * just one method, so I have to create a class around it to get it to compile.
     * I'm sure I'm missing something, but for now this is my solution.
     * 
     * If the first line does not contain a brace, then the assumption is that 
     * lines of code are to be wrapped to make a method.
     * @param javasrc
     * @return
     * @throws ParseException 
     */
    public String utilMakeClass( String javasrc ) throws ParseException {
        javasrc= javasrc.trim();
        int ibrace;
        int i= javasrc.indexOf("\n");
        if ( i==-1 ) {
            ibrace = -1;
        } else {
            String firstLine= javasrc.substring(0,i);
            ibrace= firstLine.indexOf("{");
        }
        if ( ibrace==-1 ) {
            javasrc= "void foo99() {\n" + javasrc + "\n}";
        }
        String modSrc= "class Foo99 { \n"+javasrc + "}\n";
        return modSrc;
    }
    
    /**
     * remove the extra lines we added and unindent
     * @param src
     * @return 
     */
    public String utilUnMakeClass( String src ) {
        // pop off the "class Foo" we added to make it into a class
        src= src.trim();
        int i= src.indexOf("\n");
        if ( i==-1 ) {
            return src;
        }
        src= src.substring(i+1);
        i= src.indexOf("\n");
        //if ( src.substring(0,i).contains("function foo99(") ) {
        if ( src.substring(0,i).contains("foo99(") ) {            
            src= src.substring(i+1);
        }
        String[] ss= src.split("\n");

        String search= ss[0].trim().substring(0,1);
        int indent= ss[0].indexOf(search);

        StringBuilder sb= new StringBuilder();
        for ( String s: ss ) {
            if ( s.length()>indent ) {
                sb.append( s.substring(indent) ).append("\n");
            }
        }
        return sb.toString();
    }
    
    Stack<Map<String,Type>> stack;
    Stack<Map<String,Type>> localVariablesStack;
    
    Stack<Map<String,VariableDeclarationExpr>> stackVariables;
    Stack<Map<String,FieldDeclaration>> stackFields;
    Stack<Map<String,MethodDeclaration>> stackMethods;
    Stack<Map<String,TypeDeclaration>> stackClasses;
    
    Map<String,String> nameMapForward= new HashMap<>();
    Map<String,String> nameMapReverse= new HashMap<>();
    
    /**
     * return the current scope, which includes local variables.  Presently this just uses one scope,
     * but this should check for code blocks, etc.
     * @return 
     */
    private Map<String,Type> getCurrentScope() {
        return stack.peek();
    }
    
    /**
     * this contains both the static and non-static (class) variables.
     * @return 
     */
    private Map<String,FieldDeclaration> getCurrentScopeFields() {
        return stackFields.peek();
    }
    
    private Map<String,MethodDeclaration> getCurrentScopeMethods() {
        return stackMethods.peek();
    }
    
    private Map<String,TypeDeclaration> getCurrentScopeClasses() {
        return stackClasses.peek();
    }
    
    /**
     * introduce a new level
     */
    private void pushScopeStack(boolean keepLocals) {
        HashMap newScope= new HashMap<>(getCurrentScope());
        if ( !keepLocals ) {
            newScope.putAll(localVariablesStack.peek());
            localVariablesStack.push(new HashMap<>());
        } else {
            localVariablesStack.push(localVariablesStack.peek());
        }
        stack.push( newScope );
        stackFields.push( new HashMap<>(getCurrentScopeFields()) );
        stackMethods.push( new HashMap<>(getCurrentScopeMethods()) );
        stackClasses.push( new HashMap<>(getCurrentScopeClasses()) );
    }
    
    /**
     * pop that new level
     */
    private void popScopeStack() {
        stack.pop();
        stackFields.pop();
        stackMethods.pop();
        stackClasses.pop();
        localVariablesStack.pop();
    }
            
    /**
     * record the name of the class (e.g. TimeUtil) so that internal references can be corrected.
     */
    private String theClassName;
    
    private final Stack<String> classNameStack= new Stack<>();
    
    /**
     * record the method names, since Python will need to refer to "self" to call methods but Java does not.
     */
    private final Map<String,ClassOrInterfaceDeclaration> classMethods = new HashMap<>();
    
    /**
     * return imported class names.
     */
    private final Map<String,Object> importedClasses = new HashMap<>();
     
    /**
     * return imported methods, from star imports.
     */
    private final Map<String,Object> importedMethods = new HashMap<>();
    
    /**
     * map from the import statement to a boolean.  If Boolean.TRUE, then use it.
     */
    private final Map<String,Boolean> additionalImports = new LinkedHashMap<>();
    
    /**
     * map from the additional class code to a boolean.  If Boolean.TRUE, then use it.
     */
    private final Map<String,Boolean> additionalClasses = new LinkedHashMap<>();
    
    private final Map<String,Boolean> javaImports= new LinkedHashMap<>();
    
    private final Map<String,ImportDeclaration> javaImportDeclarations= new LinkedHashMap<>();
    
    private boolean hasMain= false;
    
    // Constants
    private static final ReferenceType STRING_TYPE = ASTHelper.createReferenceType( "String", 0 );
    private static final ClassOrInterfaceType STRING_TYPE2 = new ClassOrInterfaceType("String");
    
    private static final AnnotationExpr DEPRECATED = new MarkerAnnotationExpr( new NameExpr("Deprecated") );
    
    /*** end, internal parsing state ***/

    private boolean onlyStatic = false;
    
    public static final String PROP_ONLYSTATIC = "onlyStatic";

    /**
     * true indicates that only static methods are to be pulled out of the class.
     * @return 
     */
    public boolean isOnlyStatic() {
        return onlyStatic;
    }

    /**
     * true indicates that only static methods are to be pulled out of the class.
     * @param onlyStatic 
     */
    public void setOnlyStatic(boolean onlyStatic) {
        this.onlyStatic = onlyStatic;
    }

    private boolean unittest = false;

    /**
     * true indicates this should write additional code to make a unittest suite
     * @return 
     */
    public boolean isUnittest() {
        return unittest;
    }

    /**
     * true indicates this should write additional code to make a unittest suite
     * @param unittest 
     */
    public void setUnittest(boolean unittest) {
        this.unittest = unittest;
    }
 
    private static String utilRewriteComments(String indent, Comment comments ) {
        return utilRewriteComments( indent, comments, false );
    }
    
    // Utilities
    private static String utilRewriteComments(String indent, Comment comments, boolean docs ) {
        if ( comments==null ) return "";
        StringBuilder b= new StringBuilder();
        if ( docs ) b.append(indent).append("/**\n");
        String[] ss= comments.getContent().split("\n");
        if ( ss[0].trim().length()==0 ) {
            ss= Arrays.copyOfRange( ss, 1, ss.length );
        }
        if ( ss[ss.length-1].trim().length()==0 ) {
            ss= Arrays.copyOfRange( ss, 0, ss.length-1 );
        }
        for ( String s : ss ) {
            b.append(s).append("\n"); // Note comments have indent in them already
        }
        if (docs ) b.append(indent).append(" */\n");
        return b.toString();
    }    
    
    // end, Utilities
    
    
    private String doConvertClassOrInterfaceDeclaration(String indent, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        StringBuilder sb= new StringBuilder();
        
        String name = classOrInterfaceDeclaration.getName();
        
        boolean makeInnerOuter= false;
        
        if ( classNameStack.isEmpty() ) {
            theClassName= name;
        } else {
            makeInnerOuter= true; // JavaScript doesn't support inner classes, so bring it out and print a warning.
        }
        
        classNameStack.push(name);
        
        getCurrentScopeClasses().put( name, classOrInterfaceDeclaration );
        
        pushScopeStack(false);
        getCurrentScope().put( "this", new ClassOrInterfaceType(name) );
        
        if ( onlyStatic ) {
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                sb.append( doConvert(indent,n) ).append("\n");
            });
        } else {
            
            if ( unittest ) {     
                sb.append( "// cheesy unittest temporary\n");
                sb.append( "function assertEquals(a,b) {\n");
                sb.append( "     if ( a!==b ) throw 'a!==b : ' + a + ' !== ' + b;\n");
                sb.append( "}\n");
                sb.append( "function assertArrayEquals(a,b) {\n");
                sb.append( "    if ( a.length===b.length ) {\n");
                sb.append( "        for ( i=0; i < a.length; i++ ) {\n");
                sb.append( "            if ( a[i]!==b[i] ) throw 'a['+i+']!==b['+i+'] : ' +a[i] + ' !== ' + b[i];\n");
                sb.append( "        }\n");
                sb.append( "    } else {\n");
                sb.append( "        throw 'array lengths differ';\n");
                sb.append( "    }\n");
                sb.append( "}\n");
                sb.append( "function fail(msg) {\n");
                sb.append( "    console.log(msg);\n");
                sb.append( "    throw 'fail: '+msg;\n");
                sb.append( "}                \n");
                sb.append( "\n" );
            }
            String comments= utilRewriteComments(indent, classOrInterfaceDeclaration.getComment(), true );
            if ( comments.trim().length()>0 ) {
                sb.append( comments );
            }
            
            String className;
            className= name;
            
            if ( classOrInterfaceDeclaration.getExtends()!=null && classOrInterfaceDeclaration.getExtends().size()==1 ) { 
                String extendName= doConvert( "", classOrInterfaceDeclaration.getExtends().get(0) );
                sb.append( indent ).append("class " ).append( className ).append(" extends " ).append(extendName).append(" {\n");
            } else if ( classOrInterfaceDeclaration.getImplements()!=null && classOrInterfaceDeclaration.getImplements().size()==1 ) { 
                List<ClassOrInterfaceType> impls= classOrInterfaceDeclaration.getImplements();
                StringBuilder implementsName= new StringBuilder( doConvert( "", impls.get(0) ) );
                for ( int i=1; i<impls.size(); i++ ) {
                    implementsName.append(",").append( doConvert( "", impls.get(i) ) );
                }
                sb.append( indent ).append("class " ).append( className ).append(" extends " ).append(implementsName).append(" {\n");
            } else {
                sb.append( indent ).append("class " ).append( className ).append(" {\n");
            }

            // check to see if any two methods can be combined.
            // https://github.com/jbfaden/JavaJythonConverter/issues/5
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                if ( n instanceof MethodDeclaration ) {
                    classMethods.put( ((MethodDeclaration) n).getName(), classOrInterfaceDeclaration );
                    getCurrentScopeMethods().put(((MethodDeclaration) n).getName(),(MethodDeclaration)n );
                } else if ( n instanceof ClassOrInterfaceDeclaration ) {
                    ClassOrInterfaceDeclaration coid= (ClassOrInterfaceDeclaration)n;
                    getCurrentScope().put( coid.getName(), new ClassOrInterfaceType(coid.getName()) );
                }
            });
            
            // check for unique names
            Map<String,Node> nn= new HashMap<>();
            for ( Node n: classOrInterfaceDeclaration.getChildrenNodes() ) {
                if ( n instanceof ClassOrInterfaceType ) {
                    String name1= ((ClassOrInterfaceType)n).getName();
                    if ( nn.containsKey(name1) ) {
                        sb.append(indent).append(s4).append("// J2J: Name is used twice in class: ")
                                .append(className).append(" ").append(name1).append("\n");
                    }
                    nn.put( name1, n );
                } else if ( n instanceof FieldDeclaration ) {
                    for ( VariableDeclarator vd : ((FieldDeclaration)n).getVariables() ) {
                        String name1= vd.getId().getName();
                        if ( nn.containsKey(name1) ) {
                            sb.append(indent).append(s4).append("// J2J: Name is used twice in class: ")
                                .append(className).append(" ").append(name1).append("\n");
                        }
                        nn.put( name1, vd );
                    }
                } else if ( n instanceof MethodDeclaration ) {
                    MethodDeclaration md= (MethodDeclaration)n;
                    if ( md.getAnnotations()!=null ) {
                        if ( md.getAnnotations().contains( DEPRECATED ) ) {
                            continue;
                        }
                    }
                    String name1= md.getName();
                    if ( nn.containsKey(name1) ) {
                            sb.append(indent).append(s4).append("// J2J: Name is used twice in class: ")
                                .append(className).append(" ").append(name1).append("\n");
                    }
                    nn.put( name1, n );
                } else {
                    System.err.println("Not supported: "+n);
                }
            }
            
            for ( Node n : classOrInterfaceDeclaration.getChildrenNodes() ) {
                if ( n instanceof ClassOrInterfaceType ) {
                    // skip this strange node
                } else if ( n instanceof EmptyMemberDeclaration ) {
                    // skip this strange node
                } else if ( n instanceof ConstructorDeclaration ) {
                    if ( unittest ) {
                        // skip
                    } else {
                        sb.append( doConvert( indent+s4, n ) ).append("\n");
                    }
                } else if ( n instanceof MethodDeclaration ) {
                    MethodDeclaration md=  (MethodDeclaration)n;
                    if ( md.getAnnotations()!=null ) {
                        if ( md.getAnnotations().contains( DEPRECATED ) ) {
                            continue;
                        }
                    }
                    sb.append( doConvert( indent+s4, n ) ).append("\n");
                } else {
                    sb.append( doConvert( indent+s4, n ) ).append("\n");
                }
            };
            
            if ( unittest ) {
                sb.append( indent ).append( "}" ).append("\n");
                sb.append("test = new ").append(classOrInterfaceDeclaration.getName()).append("();\n");
                for ( Node n : classOrInterfaceDeclaration.getChildrenNodes() ) {
                    if ( n instanceof MethodDeclaration 
                            && ((MethodDeclaration)n).getName().startsWith("test") 
                            && ((MethodDeclaration)n).getParameters()==null ) {
                        sb.append("test.").append(((MethodDeclaration) n).getName()).append("();\n");
                    }
                }
            }
            
        
        }
        
        if ( !onlyStatic && !unittest) {
            sb.append( indent ).append( "}" ).append("\n");
        }
        popScopeStack();
        classNameStack.pop();
        
        if ( makeInnerOuter ) {
            String outerClass= unindent( indent, sb.toString() );
            additionalClasses.put( outerClass, true );
            return "";
        } else {
            return sb.toString();
        }
        
    }

    private String doConvertMethodDeclaration(String indent, MethodDeclaration methodDeclaration) {
        boolean isStatic= ModifierSet.isStatic(methodDeclaration.getModifiers() );
        
        if ( onlyStatic && !isStatic ) {
            return "";
        } else if ( isStatic ) {
            if ( methodDeclaration.getName().equals("main") ) {
                hasMain= true;
            }
        }
        
        if ( methodDeclaration.getAnnotations()!=null ) {
            for ( AnnotationExpr a : methodDeclaration.getAnnotations() ) {
                if ( a.getName().getName().equals("Deprecated") ) {
                    return "";
                }
            }
        }        

        StringBuilder sb= new StringBuilder();
        String comments= utilRewriteComments( indent, methodDeclaration.getComment(), true );
        if ( comments.trim().length()>0 ) {
            sb.append( comments );
        }
        
        if ( isStatic  && !onlyStatic ) {
            //sb.append( indent ).append( "@staticmethod\n" );
        }
        
        String methodName= methodDeclaration.getName();
        if ( isStatic ) {
            sb.append( indent ).append("static ").append( methodName ) .append("(");
        } else {
            sb.append( indent ).append( methodName ) .append("(");
        }
        boolean comma= false;
        
        pushScopeStack(false);

        if ( methodDeclaration.getParameters()!=null ) {
            for ( Parameter p: methodDeclaration.getParameters() ) { 
                String name= p.getId().getName();
                String pythonParameterName;
                pythonParameterName= name;

                if ( comma ) {
                    sb.append(", ");
                } else {
                    comma = true;
                }
                sb.append( pythonParameterName );
                localVariablesStack.peek().put( name, p.getType() );
            }
        }
        sb.append( ") {\n" );
        
        if ( methodDeclaration.getBody()!=null ) {
            sb.append( doConvert( indent+s4, methodDeclaration.getBody() ) );  
        } else {
            // nothing needed
        }
        
        sb.append( indent ).append( "}\n" );
        
        popScopeStack();
        
        if ( isStatic && !onlyStatic ) {
            // nothing special needed here like with python
        }
        return sb.toString();
        
    }

    private String doConvertBlockStmt(String indent, BlockStmt blockStmt) {
        pushScopeStack(true);
        
        StringBuilder result= new StringBuilder();
        
        List<Statement> statements= blockStmt.getStmts();
        if ( statements==null ) {
            popScopeStack();
            return "";
        }
        int lines=0;
        for ( Statement s: statements ) {
            String aline= doConvert(indent,s);
            if ( aline.trim().length()==0 ) continue;
            result.append(aline);
            if ( !aline.endsWith("\n") ) {
                result.append("\n"); //TODO: yuck!  Why is it sometimes there and sometimes not?  Fix this.
            }
            String[] thelines= aline.split("\\n");
            for ( String l : thelines ) { // comment before line
                if ( !l.trim().startsWith("#") ) lines++;
            }
        }
        
        popScopeStack();
        return result.toString();
        
    }
    
    private String doConvertExpressionStmt(String indent, ExpressionStmt expressionStmt) {
        StringBuilder sb= new StringBuilder();
        if ( expressionStmt.getComment()!=null ) {
            sb.append(indent).append("//").append(utilRewriteComments(indent, expressionStmt.getComment() ));
        }
        sb.append( doConvert( indent, expressionStmt.getExpression() ) ).append(";");
        return sb.toString();
    }
    
    private String doConvertVariableDeclarationExpr(String indent, VariableDeclarationExpr variableDeclarationExpr) {
        StringBuilder b= new StringBuilder();
        
        for ( int i=0; i<variableDeclarationExpr.getVars().size(); i++ ) {
            if ( i>0 ) b.append("\n");
            VariableDeclarator v= variableDeclarationExpr.getVars().get(i);
            String s= v.getId().getName();
             if ( v.getInit()!=null && v.getInit().toString().startsWith("Logger.getLogger") ) {
                //addLogger();
                localVariablesStack.peek().put(s,ASTHelper.createReferenceType("Logger",0) );
                return indent + "// J2J: "+variableDeclarationExpr.toString().trim();
            }
            if ( v.getInit()!=null 
                    && ( v.getInit() instanceof ArrayInitializerExpr ) 
                    && ( variableDeclarationExpr.getType() instanceof PrimitiveType ) ) {
                Type t= ASTHelper.createReferenceType( ((PrimitiveType)variableDeclarationExpr.getType()), 1 );
                localVariablesStack.peek().put( s, t );
            } else {
                localVariablesStack.peek().put( s, variableDeclarationExpr.getType() );
            }
            if ( v.getInit()!=null ) {
                b.append( indent ).append("var ").append(s).append(" = ").append(doConvert("",v.getInit()) );
            } else {
                b.append( indent ).append("var ").append(s);
            }
        }
        return b.toString();
    }

    private String doConvertForStmt(String indent, ForStmt forStmt) {
        StringBuilder b= new StringBuilder();
        localVariablesStack.push( new HashMap<>(localVariablesStack.peek()) );
        
        List<Expression> init = forStmt.getInit();
        VariableDeclarationExpr init1= null;
        VariableDeclaratorId v=null;
        if ( init!=null && init.size()==1 && init.get(0) instanceof VariableDeclarationExpr ) {
            init1= (VariableDeclarationExpr)init.get(0);
            if (init1.getVars().size()!=1 ) {
                init1= null;
            } else {
                v= (init1.getVars().get(0)).getId();
            }
        }
        BinaryExpr compare= forStmt.getCompare() instanceof BinaryExpr ? ((BinaryExpr)forStmt.getCompare()) : null;
        List<Expression> update= forStmt.getUpdate();
        UnaryExpr update1= null;
        if ( update.size()==1 && update.get(0) instanceof UnaryExpr ) {
            update1= (UnaryExpr)update.get(0);
            if ( !(update1.getOperator()==UnaryExpr.Operator.posIncrement 
                    || update1.getOperator()==UnaryExpr.Operator.preIncrement 
                    || update1.getOperator()==UnaryExpr.Operator.posDecrement 
                    || update1.getOperator()==UnaryExpr.Operator.preDecrement  ) ) {
                update1= null;
            }
        }
        
        StringBuilder sinit=  new StringBuilder();
        forStmt.getInit().forEach((e) -> {
            sinit.append(",").append( doConvert( "", e ) );
        });
        
        String scompare= doConvert( "", forStmt.getCompare() );
        StringBuilder sincrement= new StringBuilder();
        forStmt.getUpdate().forEach((e) -> {
            sincrement.append(",").append( doConvert( "", e ) );
        });
        
        b.append( indent ).append("for ( ").append(sinit.substring(1)).append("; ").append(scompare).append("; ") 
                .append(sincrement.substring(1)). append(")");

        if ( forStmt.getBody() instanceof BlockStmt ) {
            b.append( " {\n") ;
            String sbody= doConvert( indent+s4, forStmt.getBody() );
            b.append( sbody );
            b.append( indent ) . append("}\n");
        } else {
            b.append( doConvert( indent + s4, forStmt.getBody() ) );
        }
                
        localVariablesStack.pop();
        return b.toString();

    }

    private String doConvertAssignExpr(String indent, AssignExpr assign) {
        String target= doConvert( "", assign.getTarget() );
        AssignExpr.Operator operator= assign.getOperator();
        
        switch (operator) {
            case minus:
                return indent + target + " -= " + doConvert( "", assign.getValue() );
            case plus:
                return indent + target + " += " + doConvert( "", assign.getValue() );
            case star:
                return indent + target + " *= " + doConvert( "", assign.getValue() );
            case slash:
                return indent + target + " /= " + doConvert( "", assign.getValue() );
            case or:
                return indent + target + " |= " + doConvert( "", assign.getValue() );
            case and:
                return indent + target + " &= " + doConvert( "", assign.getValue() );
            case assign:
                return indent + target + " = " + doConvert( "", assign.getValue() );
            default:
                return indent + target + " ??? " + doConvert( "", assign.getValue() ) + " (J2J AssignExpr not supported)";
        }
    }

    private String doConvertNameExpr(String indent, NameExpr nameExpr) {
        nameExpr.getBeginLine();
        String name= nameExpr.getName();
        if ( getCurrentScope().containsKey(name) ) {
            if ( localVariablesStack.peek().containsKey(name) ) {
                return name;
            } else if ( getCurrentScopeFields().containsKey(name) ) {
                FieldDeclaration decl= getCurrentScopeFields().get(name); // The problem is Java will figure out the scope, JavaScript needs the class name
                if ( ModifierSet.isStatic( decl.getModifiers() ) ) {
                    return theClassName + "." +nameExpr.getName();
                } else {
                    return "this." + name;
                }
            } else {
                return name;
            }
        } else if ( getCurrentScopeFields().containsKey(name) ) {
            FieldDeclaration decl = getCurrentScopeFields().get(name);
            boolean isStatic= ModifierSet.isStatic(decl.getModifiers() );
            if ( isStatic ) {
                return indent + theClassName + "." + name; //TODO: usually correct, nested classes.
            } else {
                return indent + "this" + "." + name; //TODO: usually correct, nested classes.
            }
            
        } else {
            return indent + nameExpr.getName();
        }
    }
    
    /**
     * return true if integer division should be used.
     * //TODO: verify short
     * @param exprType
     * @return true if it's null or it's an integer and integer division should be used.
     */
    private boolean isIntegerType(Type exprType) {
        if ( exprType==null ) {
            return false;
        }
        return exprType.equals(ASTHelper.INT_TYPE)
                || exprType.equals(ASTHelper.LONG_TYPE) 
                || exprType.equals(ASTHelper.SHORT_TYPE );
    }

    /**
     * return true if the type is a String or char.
     * // TODO: think about whether char is really a string type.  This is for Java, right?
     * @param exprType
     * @return 
     */
    private boolean isStringType(Type exprType) {
        if ( exprType==null ) {
            return false;
        }
        return exprType.equals(STRING_TYPE) || exprType.equals(ASTHelper.CHAR_TYPE);
    }
        
    /**
     * return the type of binary expressions +, -, /, *
     * @param leftType
     * @param rightType
     * @return null or the resolved type.
     */
    private Type utilBinaryExprType(Type leftType, Type rightType) {
        if ( leftType!=null && leftType.equals(rightType) ) {
            return leftType;
        }
        if ( ( leftType==ASTHelper.DOUBLE_TYPE || leftType==ASTHelper.FLOAT_TYPE ) 
            && ( isIntegerType(rightType) || rightType==ASTHelper.BYTE_TYPE ) ) {
            return leftType;
        }
        if ( ( rightType==ASTHelper.DOUBLE_TYPE || rightType==ASTHelper.FLOAT_TYPE ) 
            && ( isIntegerType(leftType) || leftType==ASTHelper.BYTE_TYPE ) ) {
            return rightType;
        }
        return null;
    }
        
    /**
     * guess the class type, and return null if there is no guess.
     * @param clas
     * @return 
     */
    private Type guessType( Expression clas ) {
        if ( clas instanceof NameExpr ) {
            String clasName= ((NameExpr)clas).getName();
            if ( Character.isUpperCase(clasName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                return ASTHelper.createReferenceType( clasName,0 );
            } else if ( localVariablesStack.peek().containsKey(clasName) ) {
                return localVariablesStack.peek().get(clasName);
            } else if ( getCurrentScope().containsKey(clasName) ) {
                return getCurrentScope().get(clasName);
            }
        } else if ( clas instanceof CastExpr ) {
            CastExpr ex= (CastExpr)clas;
            return ex.getType();
        } else if ( clas instanceof EnclosedExpr ) {
            EnclosedExpr expr= (EnclosedExpr)clas;
            return guessType(expr.getInner());
        } else if ( clas instanceof UnaryExpr ) {
            UnaryExpr ue= ((UnaryExpr)clas);
            return guessType(ue.getExpr());
        } else if ( clas instanceof BinaryExpr ) {
            BinaryExpr be= ((BinaryExpr)clas);
            Type leftType= guessType( be.getLeft() );
            Type rightType= guessType( be.getRight() );
            if ( be.getOperator()==BinaryExpr.Operator.and ||
                    be.getOperator()==BinaryExpr.Operator.or || 
                    be.getOperator()==BinaryExpr.Operator.equals ||
                    be.getOperator()==BinaryExpr.Operator.notEquals ||
                    be.getOperator()==BinaryExpr.Operator.greater ||
                    be.getOperator()==BinaryExpr.Operator.greaterEquals ||
                    be.getOperator()==BinaryExpr.Operator.less ||
                    be.getOperator()==BinaryExpr.Operator.lessEquals ) {
                return ASTHelper.BOOLEAN_TYPE;
            }
            if ( be.getOperator()==BinaryExpr.Operator.plus ) { // we automatically convert ints to strings.
                if ( ( leftType!=null && leftType.equals(STRING_TYPE) ) ||
                        ( rightType!=null && rightType.equals(STRING_TYPE) ) ) {
                    return STRING_TYPE;
                }
            }
            
            return utilBinaryExprType( leftType, rightType );
            
        } else if ( clas instanceof FieldAccessExpr ) {
            String fieldName= ((FieldAccessExpr)clas).getField();
            return getCurrentScope().get(fieldName);
        } else if ( clas instanceof ArrayAccessExpr ) {
            ArrayAccessExpr aae= (ArrayAccessExpr)clas;
            Type arrayType=  guessType( aae.getName() );
            if ( arrayType==null ) {
                return null;
            } else if ( arrayType instanceof ReferenceType && ((ReferenceType)arrayType).getArrayCount()==1 ) {
                return ((ReferenceType) arrayType).getType();
            } else {
                return null;
            }
        } else if ( clas instanceof IntegerLiteralExpr ) {
            return ASTHelper.INT_TYPE;
        } else if ( clas instanceof CharLiteralExpr ) {
            return ASTHelper.CHAR_TYPE;
        } else if ( clas instanceof LongLiteralExpr ) {
            return ASTHelper.LONG_TYPE;
        } else if ( clas instanceof DoubleLiteralExpr ) {
            return ASTHelper.DOUBLE_TYPE;
        } else if ( clas instanceof StringLiteralExpr ) {
            return STRING_TYPE;
        } else if ( clas instanceof MethodCallExpr ) {
            MethodCallExpr mce= (MethodCallExpr)clas;
            Type scopeType=null;
            if ( mce.getScope()!=null ) scopeType= guessType(mce.getScope());
            MethodDeclaration md= getCurrentScopeMethods().get(mce.getName());
            if ( md!=null ) {
                return md.getType();
            }
            if ( scopeType!=null ) {
                if (  scopeType.toString().equals("Pattern") ) {
                    if ( mce.getName().equals("matcher") ) {
                        return ASTHelper.createReferenceType("Matcher", 0);
                    } else if ( mce.getName().equals("quote" ) ) {
                        return STRING_TYPE;
                    } else if ( mce.getName().equals("split") ) {
                        return ASTHelper.createReferenceType("String", 1);
                    }
                } else if ( scopeType.toString().equals("StringBuilder") ) {
                    switch ( mce.getName() ) {
                        case "toString":
                            return STRING_TYPE;
                    }
                } else if ( scopeType.toString().equals("String") ) {
                    switch ( mce.getName() ) {
                        case "substring": 
                        case "trim": 
                        case "valueOf": 
                        case "concat": 
                        case "replace": 
                        case "replaceAll": 
                        case "replaceFirst":
                        case "toLowerCase": 
                        case "toUpperCase": 
                            return STRING_TYPE;
                        case "length": 
                            return ASTHelper.INT_TYPE;
                        case "charAt":
                            return ASTHelper.CHAR_TYPE;
                    }
                } else if ( scopeType.toString().equals("Arrays") ) {
                    switch ( mce.getName() ) {
                        case "copyOfRange":
                            return guessType( mce.getArgs().get(0) );
                    }
                }
            }
            switch ( mce.getName() ) { // TODO: consider t
                case "charAt": return ASTHelper.CHAR_TYPE;
                case "copyOfRange": return guessType( mce.getArgs().get(0) );
            }
        }
        return null;
    }
    
    private boolean utilIsAsciiString( Expression e ) {
        Type rightType = guessType(e);
        if ( STRING_TYPE.equals(rightType) ) {
            String s= e.toString();
            boolean isAscii=true;
            for ( int i=0; isAscii && i<s.length(); i++ ) {
                char c= s.charAt(i);
                if ( c!=9 && c<32 && c>127 ) {
                    isAscii= false;
                }
            }
            return isAscii;
        } else {
            return false;
        }
    }

    private String doConvertBinaryExpr(String indent, BinaryExpr b) {
        String left= doConvert(indent,b.getLeft());
        String right= doConvert(indent,b.getRight());
        Type leftType = guessType(b.getLeft());
        Type rightType = guessType(b.getRight());
        
        if ( b.getLeft() instanceof MethodCallExpr && b.getRight() instanceof IntegerLiteralExpr ) {
            MethodCallExpr mce= (MethodCallExpr)b.getLeft();
            if ( mce.getName().equals("compareTo") 
                    && ((IntegerLiteralExpr)b.getRight()).toString().equals("0") ) {
                Expression a0= mce.getArgs().get(0);
                if ( null!=b.getOperator() ) switch (b.getOperator()) {
                    case greater:
                        return doConvert("",mce.getScope()) + " > " + doConvert("",a0);
                    case greaterEquals:
                        return doConvert("",mce.getScope()) + " >= " + doConvert("",a0);
                    case less:
                        return doConvert("",mce.getScope()) + " < " + doConvert("",a0);
                    case lessEquals:
                        return doConvert("",mce.getScope()) + " <= " + doConvert("",a0);
                    case equals:
                        return doConvert("",mce.getScope()) + " == " + doConvert("",a0);
                    default:
                        break;
                }
            }
        }
        BinaryExpr.Operator op= b.getOperator();
        if (  rightType!=null && rightType.equals(ASTHelper.CHAR_TYPE) && left.startsWith("(") && left.endsWith(")") ) {
            left= left.substring(4,left.length()-1);
        }
        
        if ( leftType!=null && rightType!=null ) {
            if ( leftType.equals(ASTHelper.CHAR_TYPE) && rightType.equals(ASTHelper.INT_TYPE) ) {
                left= left + ".charCodeAt(0)";
            }
            if ( leftType.equals(ASTHelper.INT_TYPE) && rightType.equals(ASTHelper.CHAR_TYPE) ) {
                left= left + ".charCodeAt(0)";
            }
            if ( rightType.equals(STRING_TYPE) 
                    && leftType instanceof PrimitiveType 
                    && !leftType.equals(ASTHelper.CHAR_TYPE) ) {
                //left= "("+left+")";
            }
            if ( leftType.equals(STRING_TYPE) 
                    && rightType instanceof PrimitiveType 
                    && !rightType.equals(ASTHelper.CHAR_TYPE) ) {
                //right= "("+right+")";
            }
        }
        
        if ( leftType!=null && !( b.getRight() instanceof NullLiteralExpr )
                && leftType.equals(ASTHelper.createReferenceType("String", 0)) && rightType==null ) {
            right= "("+right+")";
        }
        
        switch (op) {
            case plus:
                return left + " + " + right;
            case minus:
                return left + " - " + right;
            case divide:
                if ( isIntegerType( leftType ) && isIntegerType( rightType ) ) {
                    return "Math.trunc(" +left + " / " + right + ")";
                } else {
                    return left + " / " + right;
                }
            case times:
                return left + " * " + right;
            case greater:
                return left + " > " + right;
            case less:
                return left + " < " + right;
            case greaterEquals:
                return left + " >= " + right;                
            case lessEquals:
                return left + " <= " + right;
            case and:
                return left + " && " + right;
            case or:
                return left + " || " + right;
            case equals:
                if ( right.equals("null") ) {
                    return left + " === undefined || " + left + " === " + right;
                } else if ( ASTHelper.INT_TYPE.equals(rightType) ) {
                    return left + " === " + right;
                } else {
                    if ( utilIsAsciiString(b.getLeft()) && utilIsAsciiString(b.getRight()) ) {
                        return left + " === " + right;
                    } else {
                        return left + " == " + right;
                    }
                }
            case notEquals:
                if ( right.equals("null") ) {
                    return left + " !== undefined && " + left + " !== " + right;
                } else if ( ASTHelper.INT_TYPE.equals(rightType) ) {
                    return left + " !== " + right;
                } else {
                    if ( utilIsAsciiString(b.getLeft()) && utilIsAsciiString(b.getRight()) ) {
                        return left + " != " + right;
                    } else {
                        return left + " !== " + right;
                    }
                }
            case remainder:
                return left + " % " + right;
            default:
                throw new IllegalArgumentException("not supported: "+op);
        }

    }
    
    private String doConvertUnaryExpr(String indent, UnaryExpr unaryExpr) {
        String n= doConvert("",unaryExpr.getExpr()); 
        switch (unaryExpr.getOperator()) {
            case preIncrement: {
                return indent + "++" + n;
            }
            case preDecrement: {
                return indent + "--" + n;
            }
            case posIncrement: {
                return indent + n + "++";
            }
            case posDecrement: {
                return indent + n + "--";
            }
            case positive: {
                return indent + "+" + n;
            }
            case negative: {
                return indent + "-" + n;
            }
            case not: {
                if ( unaryExpr.getExpr() instanceof MethodCallExpr && ((MethodCallExpr)unaryExpr.getExpr()).getName().equals("equals") ) {
                    if ( n.split("==").length==2 ) {
                        return indent + n.replaceAll("==","!="); // I will regret this some day.
                    } else {
                        return indent + "!(" + n + ")";
                    }
                } else {
                    return indent + "!(" + n + ")";
                }
            } 
            default:
                throw new IllegalArgumentException("not supported: "+unaryExpr);
        }
    }
    
    /**
     * wrap this with "str()" if this is not a string already.
     * @param e
     * @return doConvert(e) or "str(" + doConvert(e) + ")"
     */
    private String utilAssertStr( Expression e ) {
        Type t = guessType(e);
        if ( STRING_TYPE.equals(t) || STRING_TYPE2.equals(t) ) {
            return doConvert( "", e );
        } else {
            return "String("+doConvert("",e)+")";
        }
        
    }
    
    /**
     * turn a quoted replacement string into an raw string.  This
     * is beyond my mental capacity because it's code not interpreted
     * code, but I know "\\" needs to return "".
     * @param s for example "\\$"
     * @return for example "$"
     * @see java.util.regex.Matcher#quoteReplacement(s)
     */
    private String utilUnquoteReplacement( String s ) {
        if ((!s.contains("\\\\")) && (!s.contains("\\$"))) {
            return s;
        }
        return s.replaceAll("\\\\", "");
    }    
    
    /**
     * convert the string into a Javascript regex expression.  For example,
     * '"[ab]{2}"' becomes '/[ab]{2}/'
     * @param s
     * @return 
     */
    private String utilMakeRegex( String s ) {
        if ( !( s.startsWith("\"") && s.endsWith("\"")) ) {
            throw new IllegalArgumentException("expected quotes on string constant");
        }
        String unquote= s.substring(1,s.length()-1);
        while ( unquote.contains("\\\\") ) { // unquote.replaceAll is difficult to do!
            unquote= unquote.replace("\\\\","\\");
        }
        return "/"+unquote+"/g";
    }
    
    private String utilFormatExprList( List<Expression> l ) {
        if ( l==null ) return "";
        if ( l.isEmpty() ) return "";
        StringBuilder b= new StringBuilder( doConvert("",l.get(0)) );
        for ( int i=1; i<l.size(); i++ ) {
            b.append(", ");
            b.append(doConvert("",l.get(i)));
        }
        return b.toString();
    }

    private String utilFormatParameterList( List<Parameter> l ) {
        if ( l==null ) return "";
        if ( l.isEmpty() ) return "";
        StringBuilder b= new StringBuilder( doConvert("",l.get(0)) );
        for ( int i=1; i<l.size(); i++ ) {
            b.append(",");
            b.append(doConvert("",l.get(i)));
        }
        return b.toString();
    }

    private String utilFormatTypeList(List<Type> l) {
        if ( l==null ) return "";
        if ( l.isEmpty() ) return "";
        StringBuilder b= new StringBuilder( doConvert("",l.get(0)) );
        for ( int i=1; i<l.size(); i++ ) {
            b.append(",");
            b.append(doConvert("",l.get(i)));
        }
        return b.toString();
    }    
    
    /**
     * This is somewhat the "heart" of this converter, where Java library use is translated to JavaScript use.
     * This is where most of the business happens, where language semantics differ.   This ought to be reviewed and
     * refactored to work more independently.
     * @param indent
     * @param methodCallExpr
     * @return 
     */
    private String doConvertMethodCallExpr(String indent, MethodCallExpr methodCallExpr) {
        Expression clas= methodCallExpr.getScope();
        String name= methodCallExpr.getName();
        List<Expression> args= methodCallExpr.getArgs();
            
        if ( name==null ) {
            name=""; // I don't think this happens
        }
        
        /**
         * try to identify the class of the scope, which could be either a static or non-static method.
         */
        String clasType=""; // = guessType(clas);  TODO: these should be merged
        if ( clas instanceof NameExpr ) {
            String contextName= ((NameExpr)clas).getName(); // sb in sb.append, or String in String.format.
            Type contextType= localVariablesStack.peek().get(contextName); // allow local variables to override class variables.
            if ( contextType==null ) contextType= getCurrentScope().get(contextName);
            if ( Character.isUpperCase(contextName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                clasType= contextName;
            } else if ( stringMethods.contains(name) 
                    && ( contextType==null || contextType.equals(ASTHelper.createReferenceType("String", 0) )) ) {
                clasType= "String";
            } else if ( contextType!=null ) {
                Type t= contextType;
                if ( t.toString().equals("StringBuilder") ) {
                    clasType= "StringBuilder";
                } else {
                    clasType= t.toString();
                }
            }
        } else if ( clas instanceof StringLiteralExpr ) {
            clasType= "String";
        } else if ( stringMethods.contains(name) ) {
            if ( name.equals("format") ) {
                if ( ASTHelper.createReferenceType("String", 0).equals(guessType( clas )) ) {
                    clasType= "String";
                }
            } else {
                clasType= "String";
            }
        } else {
            Type t= guessType(clas);
            if ( t==null ) {
                clasType= "";
            } else {
                clasType= t.toString();
            }
        }

        if ( name.equals("equals") && args.size()==1 ) {
            return indent + doConvert( "",clas ) + "==" + doConvert( "",args.get(0) );
        }
        
        // remove diamond typing (Map<String,String> -> Map)
        int i= clasType.indexOf("<"); 
        if ( i>=0 ) {
            clasType= clasType.substring(0,i);
        }
        
        if ( clasType.equals("Logger") ) {
            return indent + "// J2J (logger) "+methodCallExpr.toString();
        }
        if ( clasType.equals("String") ) {
            if ( name.equals("length") ) {
                return indent + doConvert( "",clas ) + ".length";
            } else if ( name.equals("equalsIgnoreCase") ) {
                String aaa= doConvert("",clas)+".toUpperCase()";
                String bbb= doConvert("",args.get(0))+".toUpperCase()"; // TODO: we can check if literal is already upper case.
                return indent + aaa + "===" + bbb;
            } else if ( name.equals("contains") ) {
                return indent +  doConvert( "",clas ) + ".indexOf(" + doConvert("",args.get(0))+")!==-1";
            } else if ( name.equals("valueOf") ) {
                return indent +  "new String("+doConvert( "",args.get(0))+")";
            }
        }
        if ( clasType.equals("Character") ) {
            String s;
            switch ( name ) {
                case "isDigit":
                    s= doConvert( "",args.get(0) );
                    return "/[0-9]/.test(" + s + ")";
                case "isSpace":
                    s= doConvert( "",args.get(0) );
                    return "/ /.test(" + s + ")";
                case "isWhitespace":
                    s= doConvert( "",args.get(0) );
                    return "/\\s/.test(" + s + ")";
                case "isLetter":
                    s= doConvert( "",args.get(0) );
                    return "/[a-z]/i.test(" + s + ")";
                case "isAlphabetic": //TODO
                    s= doConvert( "",args.get(0) );
                    return "/[a-z]/i.test(" + s + ")";
                default: 
                    break;
            }
        }

        if ( clasType.equals("Arrays") ) {
            switch (name) {
                case "copyOfRange": {
                    StringBuilder sb= new StringBuilder();
                    sb.append(indent).append(args.get(0)).append(".slice(");
                    sb.append(doConvert("",args.get(1))).append(",").append(doConvert("",args.get(2)));
                    sb.append(")");
                    return sb.toString();
                }
                case "toString": {
                    Type t= guessType(args.get(0));
                    String js;
                    if ( t instanceof ReferenceType && ((ReferenceType)t).getArrayCount()==1 && isIntegerType(((ReferenceType)t).getType()) ) {
                        js= "', '.join( map( str, "+doConvert("",args.get(0))+" ) )";
                    } else {
                        js= "', '.join("+doConvert("",args.get(0))+")";
                    }
                    return "('['+"+js + "+']')";
                }
                case "equals": {
                    additionalImports.put( "function arrayequals( a, b ) { // private\n" +
                        "    if ( a.length!==b.length ) {\n" +
                        "        return false;\n" +
                        "    } else {\n" +
                        "        for (var i = 0; i<a.length; i++ ) {\n" +
                        "            if ( a[i]!==b[i] ) {\n" +
                        "                return false;\n" +
                        "            }\n" +
                        "        }\n" +
                        "        return true;\n" +
                        "    }\n" +
                        "}\n", true );
                    return indent + String.format( "arrayequals( %s, %s )", doConvert("",args.get(0)), doConvert("",args.get(1)) );
                }
                case "asList": 
                    // since in Python we are treating lists and arrays as the same thing, do nothing.
                    return doConvert("",args.get(0));
                
            }
        } else if ( clasType.equals("System") ) {
            switch (name) {
                case "currentTimeMillis":
                    return indent + "Date.now()";
                case "arraycopy":
                    additionalImports.put( "function arraycopy( srcPts, srcOff, dstPts, dstOff, size) {  // private\n" +
                        "    if (srcPts !== dstPts || dstOff >= srcOff + size) {\n" +
                        "        while (--size >= 0)\n" +
                        "            dstPts[dstOff++] = srcPts[srcOff++];\n" +
                        "    }\n" +
                        "    else {\n" +
                        "        var tmp = srcPts.slice(srcOff, srcOff + size);\n" +
                        "        for (var i = 0; i < size; i++)\n" +
                        "            dstPts[dstOff++] = tmp[i];\n" +
                        "    } \n" +
                        "}\n", true );
                    String a1= doConvert("",args.get(0));
                    String a2= doConvert("",args.get(1));
                    String a3= doConvert("",args.get(2));
                    String a4= doConvert("",args.get(3));
                    String a5= doConvert("",args.get(4));
                    return indent + String.format( "arraycopy( %s, %s, %s, %s, %s )", a1, a2, a3, a4, a5 );
            }
        } else if ( clasType.equals("StringBuilder") ) {
            if ( name.equals("append") ) {
                return indent + doConvert("",clas) + "+= " + utilAssertStr(args.get(0)) ;
            } else if ( name.equals("toString") ) {
                return indent + doConvert("",clas);
            } else if ( name.equals("insert") ) {
                String n= doConvert("",clas);
                String i0= doConvert("",args.get(0));
                String ins= doConvert("",args.get(1));
                String expr1= n+".substring(0,"+i0+")";
                String expr2= n+".substring("+i0+")";
                return indent + n + " = " + expr1 + "+" + ins +"+" + expr2 + ";  // J2J expr -> assignment"; // expr becomes assignment, this will cause problems
            } else if ( name.equals("length") ) {
                return indent + doConvert("",clas) + ".length";
            }
        } else if ( clasType.equals("Pattern") ) {
            switch ( name ) {
                case "compile":
                    return "new RegExp("+doConvert("",args.get(0))+")";
                case "quote":
                    return "re.escape("+doConvert("",args.get(0))+")";
                case "matcher":
                    return doConvert("",clas) + ".exec(" + doConvert("",args.get(0) ) + ")";
            }
        } else if ( clasType.equals("Matcher") ) {
            if ( name.equals("matches") ) {
                return doConvert("",clas) + "!=null";
            } else if ( name.equals("find") ) {
                if ( clas instanceof MethodCallExpr ) {
                    return doConvert("",clas).replaceAll("match", "search") + "!=null";
                } else {
                    return doConvert("",clas) + "!=null) {  //J2J: USE search not match above";
                }
            } else if ( name.equals("groupCount") ) {
                return doConvert("",clas) + ".length";
            } else if ( name.equals("group") ) {
                return doConvert("",clas) + "["+ doConvert("",args.get(0))+"]";
            }
        } else if ( clasType.equals("String") ) {
            if ( name.equals("format") ) {
                additionalImports.put("// import sprintf.js\n",true);
                if ( args.size()>1 ) {
                    return "sprintf("+doConvert("",args.get(0))+","+utilFormatExprList( args.subList(1,args.size()) ) + ")"; 
                }
            } else if ( name.equals("join") ) {
                return doConvert("",args.get(0)) + ".join(" + doConvert("",args.get(1)) + ")";
            } else if ( name.equals("split") ) {
                return doConvert("",clas ) + ".split(" + utilUnquoteReplacement( doConvert("",args.get(0)) ) + ")";
            } else if ( name.equals("replaceAll") ) {
                String search= utilMakeRegex( doConvert("",args.get(0)) );
                String replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                return indent + clas + ".replaceAll("+search+", "+replac+")";
            } else if ( name.equals("replace") ) {
                String search= doConvert("",args.get(0));
                String replac= doConvert("",args.get(1));
                return indent + clas + ".replaceAll("+search+", "+replac+")";
            } else if ( name.equals("replaceFirst") ) {
                String search= utilMakeRegex( doConvert("",args.get(0)) );
                String replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                return indent + clas + ".replace("+search+", "+replac+")";
            }
        } else if ( clasType.equals("Float") ) {
            if ( name.equals("parseFloat") ) {
                return "parseFloat("+doConvert("",args.get(0))+")";
            }
        } else if ( clasType.equals("Double") ) {
            if ( name.equals("parseDouble") ) {
                return "parseFloat("+doConvert("",args.get(0))+")";
            }
        } else if ( clasType.equals("Integer") ) {
            if ( name.equals("parseInt") ) {
                return "parseInt("+doConvert("",args.get(0))+")";
            }
        } else if ( clasType.equals("Long") ) {
            if ( name.equals("parseLong") ) {
                return "parseInt("+doConvert("",args.get(0))+")";
            }
        }

        if ( clasType.equals("HashMap") || clasType.equals("Map") ) {
            
            switch (name) {
                case "put":
                    return indent + doConvert("",clas) + ".set("+doConvert("",args.get(0))+", "+doConvert("",args.get(1))+")";
                case "get":
                    return indent + doConvert("",clas) + ".get("+doConvert("",args.get(0))+")";
                case "containsKey": // Note that unlike Java, getting a key which doesn't exist is a runtime error.
                    return indent + doConvert("",clas) + ".has(" + doConvert("",args.get(0)) + ")";
                case "remove":
                    return indent + doConvert("",clas) + ".delete(" + doConvert("",args.get(0)) + ")";
                case "size":
                    return indent + "len(" + doConvert("",clas) + ")";
                    
                default:
                    break;
            }
        }
        
        if ( clasType.equals("HashSet") || clasType.equals("Set") ) {
            switch (name) {
                case "contains":
                    return indent + doConvert("",clas) + ".has(" + doConvert("",args.get(0)) + ")";
                case "add":
                    return indent + doConvert("",clas) + ".add("+doConvert("",args.get(0))+")";
                case "remove":
                    return indent + doConvert("",clas) + ".delete(" + doConvert("",args.get(0)) + ")";  
                case "size":
                    return indent + doConvert("",clas) + ".size()"; 
            }
        }
        
        if ( clasType.equals("ArrayList") || clasType.equals("List") ) {
            switch (name) {
                case "size":
                    return indent + doConvert("",clas) + ".length";
                case "add":
                    if ( args.size()==2 ) {
                        return indent + doConvert("",clas) + "["+doConvert("",args.get(0))+"]="+doConvert("",args.get(1))+"";
                    } else {
                        return indent + doConvert("",clas) + ".push("+doConvert("",args.get(0))+")";
                    }
                case "remove":
                    if ( guessType(args.get(0)).equals(ASTHelper.INT_TYPE) ) {
                        return indent + doConvert("",clas) + ".pop("+doConvert("",args.get(0))+")";
                    } else {
                        return indent + doConvert("",clas) + ".remove(" + doConvert("",args.get(0)) + ")";
                    }
                case "get":
                    return indent + doConvert("",clas) + "at("+doConvert("",args.get(0))+")";
                case "contains":
                    return indent + "(" + doConvert("",args.get(0)) + ".index("+ doConvert("",args.get(0))+") > -1)";
                case "indexOf":
                    return indent + doConvert("",clas) + ".index(" + doConvert("",args.get(0)) + ")";
                case "toArray":
                    return indent + doConvert("",clas); // It's already an array (a list really).
                default:
                    break;
            }
        }
        
        //if ( name.equals("normalizeTime") ) {
        //    System.err.println("line1647");
        //}
        
        if ( clas==null ) {
            ClassOrInterfaceDeclaration m= classMethods.get(name);
            if ( m!=null ) {
                MethodDeclaration mm= this.getCurrentScopeMethods().get(name);
                if ( mm==null ) {
                    return indent + m.getName() + "." + name + "("+ utilFormatExprList(args) +")";
                } else {
                    boolean isStatic= ModifierSet.isStatic(mm.getModifiers() );
                    if ( isStatic ) {
                        return indent + m.getName() + "." + name + "("+ utilFormatExprList(args) +")";
                    } else {
                        return indent + "this." + name + "("+ utilFormatExprList(args) +")";
                    }
                }
            } else {
                return indent + name + "("+ utilFormatExprList(args) +")";
            }                
        } else {
            String clasName = doConvert("",clas);
            if ( name.equals("append") && clas instanceof MethodCallExpr && args.size()==1 ) {
                return indent + clasName + " + " + utilAssertStr( args.get(0));
            } else if ( clasName.equals("System.out") ) {
                if (name.equals("println") || name.equals("print") ) {
                    return indent + "console.info(" + utilFormatExprList(args)+")";
                }
            } else if ( clasName.equals("System.err") ) {
                if (name.equals("println") || name.equals("print") ) {
                    return indent + "console.info(" + utilFormatExprList(args)+")";  // console.error is more like a log level.
                }
            }
            if ( onlyStatic && clasName.equals(theClassName) )  {
                return indent            + javaNameToPythonName( name ) + "("+ utilFormatExprList(args) +")";
            } else {
                return indent + clasName +"."+javaNameToPythonName( name )+ "("+ utilFormatExprList(args) +")";
            }
        }

    }
    
    private static String javaNameToPythonName( String name ) {
        return name;
    }

    private String doConvertStringLiteralExpr(String indent, StringLiteralExpr stringLiteralExpr) {
        return "\"" + stringLiteralExpr.getValue() + "\"";
    }

    private String doConvertFieldAccessExpr(String indent, FieldAccessExpr fieldAccessExpr) {
        String s= doConvert( "", fieldAccessExpr.getScope() );
                
        // test to see if this is an array and "length" of the array is accessed.
        if ( fieldAccessExpr.getField().equals("length") ) {
            String inContext= s;
            if ( inContext.startsWith("this.") ) {
                inContext= inContext.substring(5);
            }
            Type t= localVariablesStack.peek().get(inContext);
            if (t==null ) {
                t= getCurrentScope().get(inContext);
            }
            if ( t!=null && t instanceof ReferenceType && ((ReferenceType)t).getArrayCount()>0 ) { 
                return indent + s + ".length"; // don't change
            }
        }
        
        if ( onlyStatic && !classNameStack.isEmpty() && s.equals(classNameStack.peek()) ) {
            return fieldAccessExpr.getField();
        } else if ( !classNameStack.isEmpty() && s.equals(classNameStack.peek()) ) {            
            return doConvert("",fieldAccessExpr.getScope()) + "."+  fieldAccessExpr.getField();
        } else {
            if ( s.equals("Collections") ) {
                String f= fieldAccessExpr.getField();
                switch (f) {
                    case "EMPTY_MAP":
                        return indent + "new Map()";
                    case "EMPTY_SET":
                        return indent + "new Set()";
                    case "EMPTY_LIST":
                        return indent + "new Array()";
                    default:
                        break;
                }
            }
            return indent + s + "." + fieldAccessExpr.getField();
        }

    }
    
    private String doConvertArrayCreationExpr(String indent, ArrayCreationExpr arrayCreationExpr) {
        if ( arrayCreationExpr.getInitializer()!=null ) {
            return doConvert( indent, arrayCreationExpr.getInitializer() );
        } else {
            if ( arrayCreationExpr.getDimensions()!=null && arrayCreationExpr.getDimensions().size()==1 ) {
                Expression e1= arrayCreationExpr.getDimensions().get(0);
                if ( e1 instanceof IntegerLiteralExpr ) {
                    int len= Integer.parseInt(((IntegerLiteralExpr)e1).getValue());
                    // aa= Array.apply(null, Array(50)).map(function (x, i) { return 0; });
                    if ( len>7 ) {
                        return "Array.apply(null, Array("+len + ")).map(function (x, i) { return 0; })";
                    } else {
                        StringBuilder sb= new StringBuilder(indent);
                        sb.append("[0");
                        for ( int i=1; i<len; i++ ) {
                            sb.append(",0");
                        }
                        sb.append("]");
                        return sb.toString();
                    }
                } else if ( e1 instanceof NameExpr && getCurrentScopeFields().containsKey(((NameExpr)e1).getName()) ) {
                    FieldDeclaration fd= getCurrentScopeFields().get(((NameExpr)e1).getName());
                    if ( fd.getType().equals(ASTHelper.INT_TYPE) && ModifierSet.isStatic( fd.getModifiers() ) ) {
                        int len= Integer.parseInt( fd.getVariables().get(0).getInit().toString() );
                        if ( len>7 ) {
                            return "Array.apply(null, Array("+len + ")).map(function (x, i) { return 0; })";
                        } else {
                            StringBuilder sb= new StringBuilder(indent);
                            sb.append("[0");
                            for ( int i=1; i<len; i++ ) {
                                sb.append(",0");
                            }
                            sb.append("]");
                            return sb.toString();
                        }
                    }
                        
                    return indent + "[]";
                } else if ( e1 instanceof NameExpr ) {
                    return "Array.apply(null, Array("+((NameExpr) e1).getName() + ")).map(function (x, i) { return 0; })";
                } else if ( e1 instanceof FieldAccessExpr ) {
                    return "Array.apply(null, Array("+doConvert("",e1) + ")).map(function (x, i) { return 0; })";                    
                }
            }  
        }
        return indent + "[]";
    }
    
    private String doConvertArrayInitializerExpr(String indent, ArrayInitializerExpr arrayInitializerExpr) {
        return indent + "[" + utilFormatExprList( arrayInitializerExpr.getValues() ) + "]";
    }    

    private String doConvertReturnStmt(String indent, ReturnStmt returnStmt) {
        if ( returnStmt.getExpr()==null ) {
            return indent + "return;";
        } else {
            return indent + "return " + doConvert("", returnStmt.getExpr()) + ";";
        }        
    }

    private String doConvertConditionalExpr(String indent, ConditionalExpr conditionalExpr) {
        return indent + doConvert("",conditionalExpr.getCondition()) + " ? " + 
                doConvert("",conditionalExpr.getThenExpr()) + " : " + 
                doConvert("",conditionalExpr.getElseExpr());
    }

    private String doConvertIfStmt(String indent, IfStmt ifStmt) {
        if ( ifStmt.getElseStmt()!=null ) {
            if ( ifStmt.getThenStmt() instanceof BlockStmt ) {
                if ( ifStmt.getElseStmt() instanceof BlockStmt ) {
                    return indent + "if (" + doConvert("",ifStmt.getCondition()) +
                            ") {\n" + doConvert(indent+s4,ifStmt.getThenStmt() ) + indent + "} else "
                            + "{\n" + doConvert(indent+s4,ifStmt.getElseStmt() ) + indent + "}\n";
                            
                } else {
                    if ( ifStmt.getElseStmt() instanceof IfStmt ) {
                        String elseStuff= doConvert(indent,ifStmt.getElseStmt() ); // this will start with indent, but we remove it
                        elseStuff= elseStuff.substring(indent.length());
                        return indent + "if (" + doConvert("",ifStmt.getCondition()) +
                            ") {\n" + doConvert(indent+s4,ifStmt.getThenStmt() ) + indent + "} else "
                            + elseStuff + indent + "\n";                        
                    } else {
                        return indent + "if (" + doConvert("",ifStmt.getCondition()) +
                            ") {\n" + doConvert(indent+s4,ifStmt.getThenStmt() ) + indent + "} else "
                            + "{\n" + doConvert(indent+s4,ifStmt.getElseStmt() ) + indent + "}\n";
                    }
                }
            } else if ( ifStmt.getElseStmt() instanceof BlockStmt || ifStmt.getElseStmt() instanceof IfStmt ) {
                return indent + "if (" + doConvert("",ifStmt.getCondition()) +
                            ") {\n" + doConvert(indent+s4,ifStmt.getThenStmt() ) + indent + "\n" + indent + "} else "
                            + "{\n" + doConvert(indent+s4,ifStmt.getElseStmt() ) + indent + "}\n";
            } else {
                return indent + "if (" + doConvert("",ifStmt.getCondition()) +
                    ") " + doConvert("",ifStmt.getThenStmt() ) + "" + 
                    " else " + doConvert("",ifStmt.getElseStmt() );
            }
        } else {
            if ( ifStmt.getThenStmt() instanceof BlockStmt ) {
                return indent + "if (" + doConvert("",ifStmt.getCondition()) +
                    ") {\n" + doConvert(indent+s4,ifStmt.getThenStmt() ) + indent + "}\n";
            } else {
                return indent + "if (" + doConvert("",ifStmt.getCondition()) +
                    ") " + doConvert("",ifStmt.getThenStmt() ) + "\n";
            }
        }
    }

    private String doConvertArrayAccessExpr(String indent, ArrayAccessExpr arrayAccessExpr) {
        String sindex= doConvert("",arrayAccessExpr.getIndex());
        return indent + doConvert("",arrayAccessExpr.getName())+"["+sindex+"]";
    }

    private String doConvertObjectCreationExpr(String indent, ObjectCreationExpr objectCreationExpr) {
        if ( objectCreationExpr.getType().toString().equals("StringBuilder") ) {
            if ( objectCreationExpr.getArgs()!=null ) {
                if ( objectCreationExpr.getArgs().size()==1 ) {
                    Expression e= objectCreationExpr.getArgs().get(0);
                    if ( ASTHelper.INT_TYPE.equals(guessType(e)) ) { // new StringBuilder(100);
                        return "\"\"";
                    } else {
                        return indent + utilAssertStr(e);
                    }
                } else {
                    return indent + "\"\""; // TODO: are there any stringbuilder methods which take more than one arg?
                }
            } else {
                return indent + "\"\"";
            }
        } else if ( objectCreationExpr.getType().toString().endsWith("Exception") ) {
            if ( objectCreationExpr.getArgs()==null ) {
                return indent + "\"Error\"";
            } else {
                return indent + doConvert("",objectCreationExpr.getArgs().get(0));
            }
        } else {
            String qualifiedName= null; // TODO: leftover from Python converter
            if ( qualifiedName!=null ) {
                return indent + "new " + qualifiedName + "("+ utilFormatExprList(objectCreationExpr.getArgs())+ ")";
            } else {
                if ( objectCreationExpr.getAnonymousClassBody()!=null ) {
                    StringBuilder sb= new StringBuilder();
                    String body= doConvert( indent, objectCreationExpr.getAnonymousClassBody().get(0) );
                    sb.append(indent).append(objectCreationExpr.getType()).append("(").append(utilFormatExprList(objectCreationExpr.getArgs())).append(")"); 
                    sb.append("*** // J2J: This is extended in an anonymous inner class ***");
                    return sb.toString();
                } else {
                    if ( objectCreationExpr.getType().getName().equals("HashMap") ) { 
                        return indent + "new Map()";
                    } else if ( objectCreationExpr.getType().getName().equals("ArrayList") ) { 
                        return indent + "new Array()";
                    } else if ( objectCreationExpr.getType().getName().equals("HashSet") ) {
                        return indent + "new Set()"; 
                    } else {
                        if ( javaImports.keySet().contains( objectCreationExpr.getType().getName() ) ) {
                            javaImports.put( objectCreationExpr.getType().getName(), true );
                        }
                        if ( objectCreationExpr.getType().getName().equals("String") ) {
                            if ( objectCreationExpr.getArgs().size()==1 ) {
                                Expression e= objectCreationExpr.getArgs().get(0);
                                Type t= guessType(e);
                                if ( t instanceof ReferenceType 
                                        && t.equals(ASTHelper.createReferenceType(ASTHelper.CHAR_TYPE,1) ) ) {
                                    return indent + doConvert("",e) + ".join( \"\" )";
                                } else if ( t.equals(ASTHelper.createReferenceType("StringBuilder",0) ) ) {
                                    return  doConvert("",e); // these are just strings.
                                }
                            }
                        }
                        return indent + "new " + objectCreationExpr.getType().getName() + "("+ utilFormatExprList(objectCreationExpr.getArgs()) + ")"; 
                    }
                }
            }
        }        
    }

    private String doConvertFieldDeclaration(String indent, FieldDeclaration fieldDeclaration) {
        boolean s= ModifierSet.isStatic( fieldDeclaration.getModifiers() ); 
        StringBuilder sb= new StringBuilder();
        
        List<VariableDeclarator> vv= fieldDeclaration.getVariables();
        sb.append( utilRewriteComments( indent, fieldDeclaration.getComment(), true ) );
        if ( vv!=null ) {
            for ( VariableDeclarator v: vv ) {
                VariableDeclaratorId id= v.getId();
                String name= id.getName();
                
                if ( v.getInit()!=null && v.getInit().toString().startsWith("Logger.getLogger") ) {
                    getCurrentScope().put( name, ASTHelper.createReferenceType("Logger", 0) );
                    //addLogger();
                    sb.append( indent ).append("// J2J: ").append(fieldDeclaration.toString());
                    continue;
                }
                
                getCurrentScope().put( name,fieldDeclaration.getType() );
                getCurrentScopeFields().put( name,fieldDeclaration);

                String modifiers= s ? "static " : "";
                
                if ( v.getInit()==null ) {
                    sb.append( indent ).append(modifiers).append( name ).append(";\n");
                } else {
                    sb.append( indent ).append(modifiers).append(name).append(" = ").append( doConvert( "",v.getInit() ) ).append(";\n");
                    
                }
            }
        }
        return sb.toString();        
    }

    private String doConvertSwitchStmt(String indent, SwitchStmt switchStmt) {
        StringBuilder b= new StringBuilder();
        b.append( indent ).append( "switch (" );
        b.append( doConvert("",switchStmt.getSelector()) ).append(") {\n");
        String nextIndent= indent + s4;
        String nextNextIndent = nextIndent + s4;
        for ( SwitchEntryStmt ses: switchStmt.getEntries() ) {
            Expression label= ses.getLabel();
            String slabel;
            if ( label==null ) {
                b.append( nextIndent ).append( "default:\n");
            } else {
                slabel= doConvert("",ses.getLabel());
                b.append( nextIndent ).append( "case ").append( slabel ).append(":\n");
            }
            
            if ( ses.getStmts()!=null ) {
                for ( Statement s : ses.getStmts() ) {
                    b.append( doConvert( nextNextIndent,s) );
                    b.append( "\n" );
                }
            }
        }
        b.append( indent ).append("}\n");
        return b.toString();
    }

    private String doConvertThrowStmt(String indent, ThrowStmt throwStmt) {
        return indent + "throw "+ doConvert("",throwStmt.getExpr()) + ";";
    }

    private String readFromFile(URL resource) {
        int size= 1024;
        ByteArrayOutputStream outs= new ByteArrayOutputStream(size);
        try ( InputStream ins= resource.openStream() ) {
            byte[] buf= new byte[size];
            int bytesRead= ins.read(buf);
            while ( bytesRead>-1 ) {
                outs.write( buf, 0, bytesRead );
                bytesRead= ins.read(buf);
            }
        } catch (IOException ex) {
            Logger.getLogger(ConvertJavaToJavascript.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            return outs.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String doConvertWhileStmt(String indent, WhileStmt whileStmt) {
        StringBuilder sb= new StringBuilder();
        sb.append( indent) .append( "while (");
        sb.append( doConvert( "", whileStmt.getCondition() ) );
        sb.append( ") {\n" );
        if ( whileStmt.getBody() instanceof ExpressionStmt ) {
            sb.append( doConvert( indent+s4, whileStmt.getBody() ) );
        } else {
            sb.append( doConvert( indent+s4, whileStmt.getBody() ) );
        }
        sb.append(indent).append("}");
        return sb.toString();
    }

    private String doConvertConstructorDeclaration(String indent, ConstructorDeclaration constructorDeclaration) {
        StringBuilder sb= new StringBuilder();
        String comments= utilRewriteComments( indent, constructorDeclaration.getComment(), true );
        if ( comments.trim().length()>0 ) {
            sb.append( comments );
        }
        String params= utilFormatParameterList( constructorDeclaration.getParameters() );
        sb.append(indent).append("constructor(");
        if ( params.trim().length()>0 ) sb.append(params);
        sb.append(") {\n");
        if ( constructorDeclaration.getParameters()!=null ) {
            for ( Parameter p: constructorDeclaration.getParameters() ) { 
                String name= p.getId().getName();
                localVariablesStack.peek().put( name, p.getType() );
            }
        }
        sb.append( doConvert(indent+s4,constructorDeclaration.getBlock()) );
        sb.append( indent ).append("}\n");
        
        return sb.toString();
    }

    private String doConvertTryStmt(String indent, TryStmt tryStmt) {
        StringBuilder sb= new StringBuilder();
        sb.append( indent ).append( "try {\n");
        sb.append( doConvert( indent+s4, tryStmt.getTryBlock() ) );
        int count=0;
        for ( CatchClause cc: tryStmt.getCatchs() ) {
            count++;
            String id= doConvert( "",cc.getExcept().getId() );
            sb.append(indent).append("} catch (").append(id).append(") {");
            if ( count==1 ) {
                sb.append("\n");
            } else {
                sb.append(" // J2J: these must be combined\n");
            }
            
            sb.append( doConvert( indent+s4, cc.getCatchBlock() ) );
        }
        if ( tryStmt.getFinallyBlock()!=null ) {
            sb.append( indent ).append( "} finally {\n");
            sb.append( doConvert( indent+s4, tryStmt.getFinallyBlock() ) );
        }
        sb.append(indent).append("}\n");
        return sb.toString();
    }

    private String doConvertMultiTypeParameter(String indent, MultiTypeParameter multiTypeParameter) {
        return indent + utilFormatTypeList( multiTypeParameter.getTypes() );
    }

    private String doConvertReferenceType(String indent, ReferenceType referenceType) {
        switch (referenceType.getArrayCount()) {
            case 0:
                return referenceType.getType().toString();
            case 1:
                return referenceType.getType().toString()+"[]";
            case 2:
                return referenceType.getType().toString()+"[][]";
            case 3:
                return referenceType.getType().toString()+"[][][]";
            default:
                return "***" + referenceType.toString() +"***";
        }
    }

    private String doConvertCastExpr(String indent, CastExpr castExpr) {
        String type= castExpr.getType().toString();
        switch (type) {
            case "String":
                type= "str";
                break;
            case "char":
                if ( isIntegerType(guessType(castExpr.getExpr())) ) {
                    type = "chr";
                } else {
                    type = "str";
                }   
                break;
            case "int":
                type= "int";
                break;
            case "long":
                type= "long";
                break;
            default:
                type = ""; // (FieldHandler)fh
                break;
        }
        String scastExpr= doConvert("", castExpr.getExpr() );
        
        Type argType= guessType(castExpr.getExpr());
        if ( argType!=null ) {
            if ( argType.equals( ASTHelper.DOUBLE_TYPE ) ) {
                if ( type.equals("int") || type.equals("long") ) {
                    return indent + "Math.trunc( "+scastExpr+" )";
                }
            } else if ( argType.equals( ASTHelper.INT_TYPE ) ) {
                if ( type.equals("chr") ) {
                    return indent + "String.fromCharCode( "+scastExpr+" )";
                }
            }
            return indent + scastExpr;
        } else {
            if ( type.equals("int") || type.equals("long") ) { 
                return indent + "Math.trunc( "+scastExpr+" )";
            } else {
                return indent + type + "(" + scastExpr + ")";
            }
        }
        
    }

    private String doConvertClassOrInterfaceType(String indent, ClassOrInterfaceType classOrInterfaceType) {
        return indent + classOrInterfaceType.getName();
    }

    /**
     * remove the indent from the lines of code, for example when making inner class an outer class.
     * If one of the lines doesn't start with the indent, then no lines are indented.
     * @param indent
     * @param code
     * @return 
     */
    private String unindent(String indent, String code) {
        String[] ss= code.split("\n",-2);
        for ( int i=0; i<ss.length; i++ ) {
            if ( !ss[i].startsWith(indent) && ss[i].length()>0 ) {
                return code;
            }
        }
        int n= indent.length();
        for ( int i=0; i<ss.length; i++ ) {
            if ( ss[i].length()>0 ) {
                ss[i]= ss[i].substring(n);
            }
        }
        return String.join( "\n", ss );
    }

    private String doConvertForeachStmt(String indent, ForeachStmt foreachStmt) {
        String vv= foreachStmt.getVariable().getVars().get(0).getId().getName();
        StringBuilder sb= new StringBuilder(indent);
        sb.append( doConvert( "", foreachStmt.getIterable() ) );
        sb.append(".forEach( function ( ").append(vv).append(" ) {\n ");
        sb.append(  doConvert( indent + s4, foreachStmt.getBody() ) );
        sb.append( indent ).append("}, this )");
        return sb.toString();
    }

    private String doConvertEnumDeclaration(String indent, EnumDeclaration enumDeclaration) {
        StringBuilder builder= new StringBuilder();
        if ( true ) {        
            
            getCurrentScopeClasses().put( enumDeclaration.getName(), enumDeclaration );
            
            builder.append(indent).append("class ").append(enumDeclaration.getName()).append(" {\n");
            List<EnumConstantDeclaration> ll = enumDeclaration.getEntries();
            
            for ( Node n: enumDeclaration.getChildrenNodes() ) {
                if ( n instanceof ConstructorDeclaration ) {
                    String params= utilFormatParameterList( ((ConstructorDeclaration)n).getParameters() );
                    builder.append(indent).append(s4 + "compare( o1, o2 ) {\n");
                    builder.append(indent).append(s4 + "    throw Exception(\"Implement me\");\n");
                    builder.append(indent).append(s4 + "}");
                }
            }

            for ( EnumConstantDeclaration l : ll ) {
                String args = utilFormatExprList(l.getArgs()); 
                args= "";// we drop the args
                builder.append(indent).append(s4).append("static ").append(l.getName()).append(" = new ")
                        .append(enumDeclaration.getName()).append("(").append(args).append(")") .append(";\n");
                String methodName=null;
                if ( l.getArgs()!=null && l.getArgs().get(0).getChildrenNodes()!=null ) {
                    for ( Node n: l.getArgs().get(0).getChildrenNodes() ) {
                        if ( n instanceof MethodDeclaration ) {
                            methodName= ((MethodDeclaration)n).getName();
                            builder.append( doConvert( indent, n ) );
                        }
                    }
                }
                if (methodName!=null) {
                    builder.append(indent).append(enumDeclaration.getName()).append(".").append(l.getName()).append(".")
                            .append(methodName).append('=').append(methodName).append(";\n");
                }
                
            }
            builder.append("}\n");
            
        } else {
            List<EnumConstantDeclaration> ll = enumDeclaration.getEntries();
            for ( EnumConstantDeclaration l : ll ) {
                builder.append(indent).append( "def " ).append(enumDeclaration.getName()).append("_").append(l.getName()) .append(":\n");
                if ( l.getClassBody()!=null ) {            
                    if ( l.getClassBody().size()==1 ) {
                        builder.append( doConvert(indent,l.getClassBody().get(0)) );
                    }
                } else {
                    builder.append(indent).append(s4).append("pass\n");
                }
            }
            builder.append(indent).append(enumDeclaration.getName()).append(" = {}\n");
            for ( EnumConstantDeclaration l : ll ) {
                builder.append(indent).append(enumDeclaration.getName()).append("[").append(l.getName()).append("]=")
                        .append(enumDeclaration.getName()).append("_").append(l.getName()).append("\n");
            }
        }
        additionalClasses.put( builder.toString(), true );
        return "";
        //return builder.toString();
    }

    private String doConvertEmptyStmt(String indent, EmptyStmt emptyStmt) {
        return "";
    }
    
}
