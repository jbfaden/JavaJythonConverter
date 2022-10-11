
package com.cottagesystems.convert;

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
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.comments.Comment;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.AssignExpr.Operator;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.type.Type;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.ConditionalExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.EnclosedExpr;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.stmt.BreakStmt;
import japa.parser.ast.stmt.CatchClause;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.SwitchEntryStmt;
import japa.parser.ast.stmt.SwitchStmt;
import japa.parser.ast.stmt.ThrowStmt;
import japa.parser.ast.stmt.TryStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

/**
 * Class for converting Java to Jython using an AST.
 * @author jbf
 */
public class Convert {

    public Convert() {
        this.stack = new Stack<>();
        this.stack.push( new HashMap<>() );
    }
    
    private String doConvertInitializerDeclaration(String indent, InitializerDeclaration initializerDeclaration) {
        return doConvert(indent,initializerDeclaration.getBlock());
    }
        
    private PythonTarget pythonTarget = PythonTarget.jython_2_2;

    public static final String PROP_PYTHONTARGET = "pythonTarget";

    public PythonTarget getPythonTarget() {
        return pythonTarget;
    }

    public void setPythonTarget(PythonTarget pythonTarget) {
        PythonTarget oldPythonTarget = this.pythonTarget;
        this.pythonTarget = pythonTarget;
    }

    private boolean onlyStatic = true;
    
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

    private boolean unittest = true;

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
    
    // Constants
    static final ReferenceType STRING_TYPE = ASTHelper.createReferenceType( "String", 0 );
    
    /*** internal parsing state ***/
    
    Stack<Map<String,Type>> stack;
    
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
     * introduce a new level
     */
    private void pushScopeStack() {
        Map<String,Type> n= new HashMap<>(getCurrentScope());
        stack.push(n);
    }
    
    /**
     * pop that new level
     */
    private void popScopeStack() {
        stack.pop();
    }
            
    /**
     * record the name of the class (e.g. TimeUtil) so that internal references can be corrected.
     */
    private String theClassName;
    
    /**
     * record the method names, since Python will need to refer to "self" to call methods but Java does not.
     */
    private Map<String,ClassOrInterfaceDeclaration> classMethods = new HashMap<>();
    
    /**
     * record the class fields.
     */
    private Map<String,FieldDeclaration> classFields = new HashMap<>();
    
    /**
     * return imported class names.
     */
    private Map<String,Object> importedClasses = new HashMap<>();
     
    /**
     * return imported methods, from star imports.
     */
    private Map<String,Object> importedMethods = new HashMap<>();
    
    private Set<String> additionalImports = new TreeSet<>();
    
    /*** end, internal parsing state ***/

    private String utilFormatExprList( List<Expression> l ) {
        if ( l==null ) return "";
        if ( l.isEmpty() ) return "";
        StringBuilder b= new StringBuilder( doConvert("",l.get(0)) );
        for ( int i=1; i<l.size(); i++ ) {
            b.append(",");
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
     * the indent level.
     */
    private static final String s4="    ";
    
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
        src= src.substring(i+1);
        i= src.indexOf("\n");
        if ( src.substring(0,i).contains("def foo99(") ) {
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
    
    public String doConvert( String javasrc ) throws ParseException {
        Exception throwMe= null;
        try {
            ByteArrayInputStream ins= new ByteArrayInputStream( javasrc.getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= japa.parser.JavaParser.parse(ins,"UTF-8");
            String src= doConvert( "", unit );
            if ( additionalImports!=null ) {
                StringBuilder sb= new StringBuilder();
                
                for ( String s: additionalImports ) {
                    sb.append(s);
                }
                sb.append(src);
                src= sb.toString();
            }
            return src;
        } catch ( ParseException ex ) {
            throwMe= ex;
        }
        
        try {
            String ssrc= utilMakeClass(javasrc);
            ByteArrayInputStream ins= new ByteArrayInputStream( ssrc.getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= japa.parser.JavaParser.parse(ins,"UTF-8");
            String src= doConvert( "", unit );
            src= utilUnMakeClass(src);
            
            if ( additionalImports!=null ) {
                StringBuilder sb= new StringBuilder();
                for ( String s: additionalImports ) {
                    sb.append(s);
                }
                sb.append(src);
                src= sb.toString();
            }
            return src;
        } catch ( ParseException ex ) {
            throwMe= ex;
        }
        
        try {
                String[] lines= javasrc.split("\n");
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
                if ( additionalImports!=null ) {
                    StringBuilder sb= new StringBuilder();
                    for ( String s: additionalImports ) {
                        sb.append(s);
                    }
                    sb.append(src);
                    src= sb.toString();
                }

                return src;
            
        } catch (ParseException ex1) {
            throwMe= ex1;
        }
        
        try {
            Statement parsed = japa.parser.JavaParser.parseStatement(javasrc);
            return doConvert("", parsed);
        } catch (ParseException ex2) {
            throwMe= ex2;
        }
        
        try {
            BodyDeclaration parsed = japa.parser.JavaParser.parseBodyDeclaration(javasrc);
            return doConvert("", parsed);
        } catch (ParseException ex3 ) {
            Statement parsed = japa.parser.JavaParser.parseBlock(javasrc);
            return doConvert("", parsed);
        }

    }

    private String doConvertBinaryExpr(String indent,BinaryExpr b) {
        String left= doConvert(indent,b.getLeft());
        String right= doConvert(indent,b.getRight());
        Type leftType = guessType(b.getLeft());
        Type rightType = guessType(b.getRight());
        
        if ( b.getRight() instanceof IntegerLiteralExpr && b.getLeft() instanceof MethodCallExpr ) {
            MethodCallExpr mce= (MethodCallExpr)b.getLeft();
            if ( mce.getName().equals("compareTo") 
                    && ((IntegerLiteralExpr)b.getRight()).toString().equals("0") ) {
                if ( null!=b.getOperator() ) switch (b.getOperator()) {
                    case greater:
                        return doConvert("",mce.getScope()) + ">" + doConvert("",mce.getArgs().get(0));
                    case greaterEquals:
                        return doConvert("",mce.getScope()) + ">=" + doConvert("",mce.getArgs().get(0));
                    case less:
                        return doConvert("",mce.getScope()) + "<" + doConvert("",mce.getArgs().get(0));
                    case lessEquals:
                        return doConvert("",mce.getScope()) + "<=" + doConvert("",mce.getArgs().get(0));
                    case equals:                    
                        return doConvert("",mce.getScope()) + "==" + doConvert("",mce.getArgs().get(0));
                    default:
                        break;
                }
            }
        }
        BinaryExpr.Operator op= b.getOperator();
        if (  rightType!=null && rightType.equals(ASTHelper.CHAR_TYPE) && left.startsWith("ord(") && left.endsWith(")") ) {
            left= left.substring(4,left.length()-1);
        }
        
        if ( leftType!=null && rightType!=null ) {
            if ( leftType.equals(ASTHelper.CHAR_TYPE) && rightType.equals(ASTHelper.INT_TYPE) ) {
                left= "ord("+left+")";
            }
            if ( leftType.equals(ASTHelper.INT_TYPE) && rightType.equals(ASTHelper.CHAR_TYPE) ) {
                left= "ord("+left+")";
            }
        }
        
        switch (op) {
            case plus:
                return left + "+" + right;
            case minus:
                return left + "-" + right;
            case divide:
                return left + "/" + right;
            case times:
                return left + "*" + right;
            case greater:
                return left + ">" + right;
            case less:
                return left + "<" + right;
            case greaterEquals:
                return left + ">=" + right;                
            case lessEquals:
                return left + "<=" + right;
            case and:
                return left + " and " + right;
            case or:
                return left + " or " + right;
            case equals:
                return left + "==" + right;
            case notEquals:
                return left + "!=" + right;
            case remainder:
                return left + "%" + right;
            default:
                throw new IllegalArgumentException("not supported: "+op);
        }
    }
    
    private Type guessType( Expression clas ) {
        if ( clas instanceof NameExpr ) {
            String clasName= ((NameExpr)clas).getName();
            if ( Character.isUpperCase(clasName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                return ASTHelper.createReferenceType( clasName,0 );
            } else if ( getCurrentScope().containsKey(clasName) ) {
                return getCurrentScope().get(clasName);
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
            Type t= guessType( mce.getScope() );
            switch ( mce.getName() ) { // TODO: consider t
                case "charAt": return ASTHelper.CHAR_TYPE;
            }
        }
        return null;
    }

    static HashSet stringMethods= new HashSet();

    static {
        
        stringMethods.add("format");
        stringMethods.add("substring");
        stringMethods.add("indexOf");
        stringMethods.add("contains");
        stringMethods.add("toUpperCase");
        stringMethods.add("toLowerCase"); 
        stringMethods.add("charAt"); 
        stringMethods.add("startsWith");
        stringMethods.add("endsWith");
        stringMethods.add("equalsIgnoreCase");
    }

    static HashSet characterMethods= new HashSet();
    static {
        characterMethods.add("isDigit");
        characterMethods.add("isSpace");
        characterMethods.add("isWhitespace");
        characterMethods.add("isLetter");
    }

            
    private String doConvertMethodCallExpr(String indent,MethodCallExpr methodCallExpr) {
        Expression clas= methodCallExpr.getScope();
        String name= methodCallExpr.getName();
        List<Expression> args= methodCallExpr.getArgs();
            
        if ( name==null ) {
            name=""; // I don't think this happens
        }
                
        /**
         * try to identify the class of the scope, which could be either a static or non-static method.
         */
        String clasType="";  
        if ( clas instanceof NameExpr ) {
            String contextName= ((NameExpr)clas).getName(); // sb in sb.append, or String in String.format.
            if ( Character.isUpperCase(contextName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                clasType= contextName;
            } else if ( stringMethods.contains(name) ) {
                clasType= "String";
            } else if ( characterMethods.contains(name) ) {
                clasType= "Character";
            } else if ( getCurrentScope().containsKey(contextName) ) {
                Type t= getCurrentScope().get(contextName);
                if ( t.toString().equals("StringBuilder") ) {
                    clasType= "StringBuilder";
                } else {
                    System.err.println("don't know what to do with type: "+ t);
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
        }
                
        if ( clasType.equals("System") && name.equals("currentTimeMillis") ) {
            additionalImports.add("from java.lang import System\n");
        } else if ( clasType.equals("Double") ) {
            additionalImports.add("from java.lang import Double\n");
        } else if ( clasType.equals("Integer") ) {
            additionalImports.add("from java.lang import Integer\n");
        } else if ( clasType.equals("Short") ) {
            additionalImports.add("from java.lang import Short\n");
        } else if ( clasType.equals("Character") ) {
            additionalImports.add("from java.lang import Character\n");
        } else if ( clasType.equals("Byte") ) {
            additionalImports.add("from java.lang import Byte\n");
        } else if ( clasType.equals("IllegalArgumentException") ) {
            additionalImports.add("from java.lang import IllegalArgumentException\n");
        }
        
        if ( clasType.equals("StringBuilder") ) {
            if ( name.equals("append") ) {
                return indent + doConvert("",clas) + "+= " + utilAssertStr(args.get(0)) ;
            } else if ( name.equals("toString") ) {
                return indent + doConvert("",clas);
            }
        }
        if ( clasType.equals("Math") ) {
            switch (name) {
                case "pow":
                    if ( args.get(1) instanceof IntegerLiteralExpr ) {
                        return doConvert(indent,args.get(0)) + "**"+ doConvert(indent,args.get(1));
                    } else {
                        return doConvert(indent,args.get(0)) + "**("+ doConvert(indent,args.get(1))+")";
                    }
                case "max":
                    return name + "(" + doConvert(indent,args.get(0)) + ","+ doConvert(indent,args.get(1))+")";
                case "min":
                    return name + "(" + doConvert(indent,args.get(0)) + ","+ doConvert(indent,args.get(1))+")";
                default:
                    break;
            }
        }
        if ( clasType.equals("String") ) {
            switch (name) {
                case "format":
                    StringBuilder sb= new StringBuilder();
                    sb.append(indent).append(args.get(0)).append(" % (");
                    sb.append( utilFormatExprList( args.subList(1, args.size() ) ) );
                    sb.append(" )");
                    return sb.toString();
                case "substring":
                    if ( args.size()==2 ) {
                        return doConvert(indent,clas)+"["+ doConvert("",args.get(0)) + ":"+ doConvert("",args.get(1)) +"]";
                    } else {
                        return doConvert(indent,clas)+"["+ doConvert("",args.get(0)) +":]";
                    }
                case "indexOf":
                    return doConvert(indent,clas)+".find("+ doConvert("",args.get(0)) + ")";
                case "contains":
                    return doConvert(indent,args.get(0)) + " in " + doConvert(indent,clas);
                case "toUpperCase":
                    return doConvert(indent,clas) + ".upper()";
                case "toLowerCase":
                    return doConvert(indent,clas) + ".lower()";
                case "charAt":
                    return indent + doConvert(indent,clas)+"["+ doConvert("",args.get(0)) +"]";
                case "startsWith":
                    return doConvert(indent,clas)+".startswith("+ utilFormatExprList(args) +")";
                case "endsWith":
                    return doConvert(indent,clas)+".endswith("+ utilFormatExprList(args) +")";
                case "equalsIgnoreCase":
                    return doConvert(indent,clas)+".lower()=="+ utilFormatExprList(args) +".lower()";
                default:
                    break;
            }
        }
        if ( clasType.equals("Character") ) {
            String s;
            switch ( name ) {
                case "isDigit":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isdigit()"; // TODO: cheesy
                case "isSpace":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isspace()"; // TODO: cheesy
                case "isWhitespace":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isspace()"; // TODO: cheesy
                case "isLetter":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isalpha()"; // TODO: cheesy
                default: 
                    break;
            }
        }
        
        if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("err") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArgs().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArgs().get(0) );
                String sWithNewLine= s.substring(0,s.length()-1) + "\\n'";
                sb.append(indent).append( "sys.stderr.write(" ).append(sWithNewLine).append(")");
            } else {
                sb.append(indent).append( "sys.stderr.write(" ).append( doConvert( "", methodCallExpr.getArgs().get(0) ) ).append( "+'\\n')" );
            }
            return sb.toString();
        } else if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("out") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArgs().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArgs().get(0) );
                sb.append(indent).append( "print(" ).append( s ).append( ")" );
            } else {
                sb.append(indent).append( "print(" ).append( doConvert( "", methodCallExpr.getArgs().get(0) ) ).append( ")" );
            }    
            return sb.toString();
        } else if ( name.equals("length") && args==null ) {
            return indent + "len("+ doConvert("",clas)+")";
        } else if ( name.equals("equals") ) {
            return indent + doConvert(indent,clas)+"=="+ utilFormatExprList(args);
        } else if ( name.equals("arraycopy") && clasType.equals("System") ) {
            String target = doConvert( "", methodCallExpr.getArgs().get(2) );
            String source = doConvert( "", methodCallExpr.getArgs().get(0) );
            Expression sourceIdx = methodCallExpr.getArgs().get(1);
            Expression targetIdx = methodCallExpr.getArgs().get(3);
            Expression length= methodCallExpr.getArgs().get(4);
            String targetIndexs= utilCreateIndexs(targetIdx, length);
            String sourceIndexs= utilCreateIndexs(sourceIdx, length);
            String j= String.format( "%s[%s]=%s[%s]", 
                    target, targetIndexs, source, sourceIndexs ); 
            return indent + j;
        } else {
            if ( clas==null ) {
                ClassOrInterfaceDeclaration m= classMethods.get(name);
                if ( m!=null ) {
                    return indent + m.getName() + "." + name + "("+ utilFormatExprList(args) +")";
                } else {
                    return indent + name + "("+ utilFormatExprList(args) +")";
                }                
            } else {
                String clasName = doConvert("",clas);
                if ( name.equals("append") && clas instanceof MethodCallExpr && args.size()==1 ) {
                    return indent + clasName + " + " + utilAssertStr( args.get(0));
                    
                } else {
                    if ( onlyStatic && clasName.equals(theClassName) )  {
                        return indent            + name + "("+ utilFormatExprList(args) +")";
                    } else {
                        return indent + clasName +"."+name + "("+ utilFormatExprList(args) +")";
                    }
                }
            }
        }
    }

    private String doAssignExpr(String indent, AssignExpr assign ) {
        String target= doConvert( "", assign.getTarget() );
        Operator operator= assign.getOperator();
        
        if ( null==operator ) {
            return indent + target + " = " + doConvert( "", assign.getValue() );
        } else switch (operator) {
            case minus:
                return indent +target + " -= " + doConvert( "", assign.getValue() );
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
            default:
                return indent + target + " = " + doConvert( "", assign.getValue() );
        }
    }
    
    private String doConvert( String indent, Node n ) {
        
        if ( n==null ) {
            throw new IllegalArgumentException("n cannot be null.");
        }
        if ( n.getClass()==null ) {
            throw new IllegalArgumentException("no class");
        }
        String simpleName= n.getClass().getSimpleName();
        switch ( simpleName ) {
            case "foo":
                return "foo";
            case "CompilationUnit":
                return doConvertCompilationUnit(indent,(CompilationUnit)n);
            case "AssignExpr":
                return doAssignExpr(indent,(AssignExpr)n);
            case "BinaryExpr":
                return doConvertBinaryExpr(indent,(BinaryExpr)n);
            case "NameExpr":
                String s= ((NameExpr)n).getName();
                if ( classFields.containsKey(s) ) {
                    if ( nameMapForward.containsKey(s) ) {
                        return indent + theClassName + "." + nameMapForward.get(s);
                    } else {
                        return indent + theClassName + "." + s;
                    }
                } else {
                    if ( nameMapForward.containsKey(s) ) {
                        return indent + nameMapForward.get(s);
                    } else {
                        return indent + s;
                    }
                }
            case "EnclosedExpr": 
                return indent + "(" + doConvert( "", ((EnclosedExpr)n).getInner() ) + ")";
            case "NullLiteralExpr":
                return indent + "None";
            case "BooleanLiteralExpr":
                return indent + ( ((BooleanLiteralExpr)n).getValue() ? "True" : "False" );
            case "LongLiteralExpr":
                return indent + ((LongLiteralExpr)n).getValue();
            case "IntegerLiteralExpr":
                return indent + ((IntegerLiteralExpr)n).getValue();
            case "DoubleLiteralExpr":
                return indent + ((DoubleLiteralExpr)n).getValue();
            case "CharLiteralExpr":
                return indent + "'" + ((CharLiteralExpr)n).getValue() +"'";
            case "CastExpr":
                return doConvertCastExpr(indent,(CastExpr)n);
            case "MethodCallExpr":
                return doConvertMethodCallExpr(indent,(MethodCallExpr)n);
            case "StringLiteralExpr":
                return doConvertStringLiteralExpr(indent,(StringLiteralExpr)n);
            case "ConditionalExpr":
                return doConvertConditionalExpr(indent,(ConditionalExpr)n);
            case "UnaryExpr":
                return doConvertUnaryExpr(indent,(UnaryExpr)n);
            case "BodyDeclaration":
                return "<Body Declaration>";
            case "BlockStmt":
                return doConvertBlockStmt(indent+s4,(BlockStmt)n);
            case "ExpressionStmt":
                return doConvertExpressionStmt(indent,(ExpressionStmt)n);
            case "VariableDeclarationExpr":
                return doConvertVariableDeclarationExpr(indent,(VariableDeclarationExpr)n);
            case "IfStmt":
                return doConvertIfStmt(indent,(IfStmt)n);
            case "ForStmt":
                return doConvertForStmt(indent,(ForStmt)n);
            case "WhileStmt":
                return doConvertWhileStmt(indent,(WhileStmt)n);
            case "SwitchStmt":
                return doSwitchStmt(indent,(SwitchStmt)n);                
            case "ReturnStmt":
                return doConvertReturnStmt(indent,(ReturnStmt)n);
            case "BreakStmt":
                return indent + "break";
            case "TryStmt":
                return doConvertTryStmt(indent,(TryStmt)n);
            case "ReferenceType":
                return doConvertReferenceType(indent,(ReferenceType)n);
            case "MultiTypeParameter":
                return doMultiTypeParameter(indent,(MultiTypeParameter)n);
            case "ThrowStmt":
                return doConvertThrowStmt(indent,(ThrowStmt)n);
            case "ArrayCreationExpr":
                return doConvertArrayCreationExpr(indent,(ArrayCreationExpr)n);
            case "ArrayInitializerExpr":
                return doConvertArrayInitializerExpr(indent,(ArrayInitializerExpr)n);
            case "ArrayAccessExpr":
                return doConvertArrayAccessExpr(indent,(ArrayAccessExpr)n);
            case "FieldAccessExpr":
                return doConvertFieldAccessExpr(indent,(FieldAccessExpr)n);
            case "ThisExpr":
                return "self";
            case "ImportDeclaration":
                return doConvertImportDeclaration(indent,(ImportDeclaration)n);
            case "PackageDeclaration":
                return "";
            case "FieldDeclaration":
                return doConvertFieldDeclaration(indent,(FieldDeclaration)n);
            case "MethodDeclaration":
                return doConvertMethodDeclaration(indent,(MethodDeclaration)n);
            case "ClassOrInterfaceDeclaration":
                return doConvertClassOrInterfaceDeclaration(indent,(ClassOrInterfaceDeclaration)n);
            case "ClassOrTypeInterfaceType":
                return doConvertClassOrInterfaceType(indent,(ClassOrInterfaceType)n); // TODO: this looks suspicious
            case "ConstructorDeclaration":
                return doConvertConstructorDeclaration(indent,(ConstructorDeclaration)n);
            case "EnumDeclaration":
                return doConvertEnumDeclaration(indent,(EnumDeclaration)n);
            case "InitializerDeclaration":
                return doConvertInitializerDeclaration(indent,(InitializerDeclaration)n);
            case "ObjectCreationExpr":
                return doConvertObjectCreationExpr(indent,(ObjectCreationExpr)n);
            case "ClassOrInterfaceType":
                return doConvertClassOrInterfaceType(indent,(ClassOrInterfaceType)n);
            case "Parameter":
                return indent + ((Parameter)n).getId().getName(); // TODO: varargs, etc
            default:
                return indent + "*** "+simpleName + "*** " + n.toString() + "*** end "+simpleName + "****";
        }
    }
    

    private String doConvertStringLiteralExpr(String indent,StringLiteralExpr stringLiteralExpr) {
        return "'" + stringLiteralExpr.getValue() + "'";
    }

    private String doConvertBlockStmt(String indent,BlockStmt blockStmt) {
        
        pushScopeStack();
        
        StringBuilder result= new StringBuilder();
        List<Statement> statements= blockStmt.getStmts();
        if ( statements==null ) {
            popScopeStack();
            return indent + "pass\n";
        }
        for ( Statement s: statements ) {
            String aline= doConvert(indent,s);
            if ( aline.trim().length()==0 ) continue;
            result.append(doConvert(indent,s));
            result.append("\n");
        }
        popScopeStack();
        return result.toString();
    }

    private String doConvertExpressionStmt(String indent, ExpressionStmt expressionStmt) {
        return doConvert(indent,expressionStmt.getExpression());
    }

    private String doConvertVariableDeclarationExpr(String indent, VariableDeclarationExpr variableDeclarationExpr) {
        StringBuilder b= new StringBuilder();
        for ( VariableDeclarator v: variableDeclarationExpr.getVars() ) {
            String s= v.getId().getName();
            if ( s.equals("len") ) {
                String news= "llen446";
                nameMapForward.put( s, news );
                nameMapReverse.put( news, s );
                s= news;
            }
            getCurrentScope().put( s, variableDeclarationExpr.getType() );
            if ( v.getInit()!=null ) {
                if ( v.getInit() instanceof ConditionalExpr ) {
                    ConditionalExpr cc  = (ConditionalExpr)v.getInit();
                    b.append( indent ).append("if ").append(doConvert("",cc.getCondition())).append(":\n");
                    b.append(indent).append(s4).append( s );
                    b.append(" = ").append(doConvert("",cc.getThenExpr() )).append("\n");
                    b.append( indent ).append("else:\n" );
                    b.append(indent).append(s4).append( s );
                    b.append(" = ").append(doConvert("",cc.getElseExpr() )).append("\n");
                } else {
                    if ( v.getInit() instanceof ObjectCreationExpr && ((ObjectCreationExpr)v.getInit()).getAnonymousClassBody()!=null ) {
                        for ( BodyDeclaration bd: ((ObjectCreationExpr)v.getInit()).getAnonymousClassBody() ) {
                            b.append(doConvert( indent+"#J2J:", bd ) );
                        }
                    }
                    b.append( indent ).append(s).append(" = ").append(doConvert("",v.getInit()) );
                }
            }
        }
        return b.toString();
    }

    private String specialConvertElifStmt( String indent, IfStmt ifStmt ) {
        StringBuilder b= new StringBuilder();
        b.append(indent).append("elif ");
        b.append( doConvert("", ifStmt.getCondition() ) );
        b.append(":\n");
        b.append( doConvert(indent,ifStmt.getThenStmt() ) );
        if ( ifStmt.getElseStmt()!=null ) {
            if ( ifStmt.getElseStmt() instanceof IfStmt ) {
                b.append( specialConvertElifStmt( indent, (IfStmt)ifStmt.getElseStmt() ) );
            } else {
                b.append(indent).append("else:\n");
                b.append( doConvert(indent,ifStmt.getElseStmt()) );
            }
        }
        return b.toString();        
    }
    
    private String doConvertIfStmt(String indent, IfStmt ifStmt) {
        StringBuilder b= new StringBuilder();
        b.append(indent).append("if ");
        b.append( doConvert("", ifStmt.getCondition() ) );
        if ( ifStmt.getThenStmt() instanceof BlockStmt ) {
            b.append(":\n");
            b.append( doConvert(indent,ifStmt.getThenStmt() ) );
        } else {
            b.append(": ");
            b.append( doConvert("",ifStmt.getThenStmt() ) ).append("\n");
        }
        if ( ifStmt.getElseStmt()!=null ) {
            if ( ifStmt.getElseStmt() instanceof IfStmt ) {
                b.append( specialConvertElifStmt( indent, (IfStmt)ifStmt.getElseStmt() ) );
            } else {
                b.append(indent).append("else");
                if ( ifStmt.getElseStmt() instanceof BlockStmt ) {
                    b.append(":\n");
                    b.append( doConvert(indent,ifStmt.getElseStmt()) );
                } else {
                    b.append(": ");
                    b.append( doConvert("",ifStmt.getThenStmt() ) ).append("\n");
                }
            }
        }
        return b.toString();
    }

    private String doConvertForStmt(String indent, ForStmt forStmt) {
        StringBuilder b= new StringBuilder();
        forStmt.getInit().forEach((e) -> {
            b.append(indent).append( doConvert( "", e ) ).append( "\n" );
        });
        b.append( indent ).append("while ").append(doConvert( "", forStmt.getCompare() )).append(":\n");
        if ( forStmt.getBody() instanceof ExpressionStmt ) {
            b.append(indent).append(s4).append( doConvert( "", forStmt.getBody() ) ).append("\n");
        } else {
            b.append( doConvert( indent, forStmt.getBody() ) );
        }
        forStmt.getUpdate().forEach((e) -> {
            b.append(indent).append(s4).append( doConvert( "", e ) ).append( "\n" );
        });
        return b.toString();
    }
    
    private String doConvertImportDeclaration( String indent, ImportDeclaration d ) {
        StringBuilder sb= new StringBuilder();
        NameExpr n= d.getName();
        if ( n instanceof QualifiedNameExpr ) {
            QualifiedNameExpr qn= (QualifiedNameExpr)n;
            sb.append( "from " ).append( qn.getQualifier() ).append( " import " ).append( n.getName() );
        } else {
            String nn= n.getName();
            int i= nn.lastIndexOf(".");
            sb.append( "from " ).append( nn.substring(0,i) ).append( " import " ).append( nn.substring(i));
        }
        return sb.toString();
    }

    private String doConvertCompilationUnit(String indent, CompilationUnit compilationUnit) {
        
        pushScopeStack();

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
            sb.append( doConvert( "", n ) ).append("\n");
        }
        
        //for ( Comment c: compilationUnit.getComments() ) {
        //    sb.append("# ").append(c.getContent()).append("\n");
        //}
        
        popScopeStack();
        
        return sb.toString();
    }

    private String doConvertReturnStmt(String indent, ReturnStmt returnStmt) {
        if ( returnStmt.getExpr()==null ) {
            return indent + "return";
        } else {
            return indent + "return " + doConvert("", returnStmt.getExpr());
        }
    }

    private String doConvertArrayCreationExpr(String indent, ArrayCreationExpr arrayCreationExpr) {
        if ( arrayCreationExpr.getInitializer()!=null ) {
            ArrayInitializerExpr ap= arrayCreationExpr.getInitializer();
            StringBuilder sb= new StringBuilder();
            return "[ " + utilFormatExprList( ap.getValues() ) + " ]";
        } else {
            return "[0] * " + doConvert( "", arrayCreationExpr.getDimensions().get(0) );
        }
    }

    private String doConvertFieldAccessExpr(String indent, FieldAccessExpr fieldAccessExpr) {
        String s= doConvert( "", fieldAccessExpr.getScope() );
        
        // test to see if this is an array and "length" of the array is accessed.
        if ( fieldAccessExpr.getField().equals("length") ) {
            Type t= getCurrentScope().get(s);
            if ( t!=null && t instanceof ReferenceType && ((ReferenceType)t).getArrayCount()>0 ) { 
                return indent + "len("+ s + ")";
            }
        }
        if ( onlyStatic && s.equals(theClassName) ) {
            return fieldAccessExpr.getField();
        } else {
            return indent + s + "." + fieldAccessExpr.getField();
        }
    }

    private String doConvertArrayAccessExpr(String indent, ArrayAccessExpr arrayAccessExpr) {
        return doConvert( indent,arrayAccessExpr.getName()) + "["+doConvert("",arrayAccessExpr.getIndex())+"]";
    }

    private String doConvertUnaryExpr(String indent, UnaryExpr unaryExpr) {
        if ( null==unaryExpr.getOperator() ) {
            throw new IllegalArgumentException("not supported: "+unaryExpr);
        } else switch (unaryExpr.getOperator()) {
            case posIncrement: {
                String n= doConvert("",unaryExpr.getExpr());
                return indent + n + "=" + n + "+1";
            }
            case posDecrement: {
                String n= doConvert("",unaryExpr.getExpr());
                return indent + n + "=" + n + "-1";
            }
            case positive: {
                String n= doConvert("",unaryExpr.getExpr());
                return indent + "+"+n;
            }
            case negative: {
                String n= doConvert("",unaryExpr.getExpr());
                return indent + "-"+n;
            }
            case not: {
                String n= doConvert("",unaryExpr.getExpr());
                return indent + "not "+n;
            } 
            default:
                throw new IllegalArgumentException("not supported: "+unaryExpr);
        }
    }

    private String doConvertClassOrInterfaceDeclaration(String indent, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        StringBuilder sb= new StringBuilder();
        theClassName= classOrInterfaceDeclaration.getName();
        
        pushScopeStack();
        getCurrentScope().put( "this", new ClassOrInterfaceType(theClassName) );
        
        if ( onlyStatic ) {
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                sb.append( doConvert(indent,n) ).append("\n");
            });
        } else {
            
            if ( unittest ) {
                sb.append( "\n# cheesy unittest temporary\n");
                sb.append( "def assertEquals(a,b):\n    print a\n    print b\n    if ( not a==b ): raise Exception('a!=b')\n");
                sb.append( "def assertArrayEquals(a,b):\n");
                sb.append( "    for a1 in a: print a1, \n");
                sb.append( "    print ' '+str(len(a)) \n");
                sb.append( "    for b1 in b: print b1, \n");
                sb.append( "    print ' '+str(len(b)) \n");
                sb.append( "    if ( len(a)==len(b) ): \n");
                sb.append( "        for i in xrange(len(a)): \n");
                sb.append( "            if ( a[i]!=b[i] ): raise Exception('a[%d]!=b[%d]'%(i,i))\n" );
            }
            String comments= utilRewriteComments(indent, classOrInterfaceDeclaration.getComment() );
            sb.append( "\n" );
            sb.append( comments );
            
            if ( classOrInterfaceDeclaration.getExtends()!=null && classOrInterfaceDeclaration.getExtends().size()==1 ) { 
                String extendName= doConvert( "", classOrInterfaceDeclaration.getExtends().get(0) );
                sb.append( indent ).append("class " ).append( theClassName ).append("(" ).append(extendName).append(")").append(":\n");
            } else if ( classOrInterfaceDeclaration.getImplements()!=null ) { 
                List<ClassOrInterfaceType> impls= classOrInterfaceDeclaration.getImplements();
                StringBuilder implementsName= new StringBuilder( doConvert( "", impls.get(0) ) );
                for ( int i=1; i<impls.size(); i++ ) {
                    implementsName.append(",").append( doConvert( "", impls.get(i) ) );
                }
                sb.append( indent ).append("class " ).append( theClassName ).append("(" ).append(implementsName).append(")").append(":\n");
            } else {
                sb.append( indent ).append("class " ).append( theClassName ).append(":\n");
            }

            // check to see if any two methods can be combined.
            // https://github.com/jbfaden/JavaJythonConverter/issues/5
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                if ( n instanceof MethodDeclaration ) {
                    classMethods.put( ((MethodDeclaration) n).getName(), classOrInterfaceDeclaration );
                } else if ( n instanceof ClassOrInterfaceDeclaration ) {
                    ClassOrInterfaceDeclaration coid= (ClassOrInterfaceDeclaration)n;
                    getCurrentScope().put( coid.getName(), new ClassOrInterfaceType(coid.getName()) );
                }
            });
            
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                if ( n instanceof ClassOrInterfaceType ) {
                    // skip this strange node
                } else if ( n instanceof EmptyMemberDeclaration ) {
                    // skip this strange node
                } else {
                    sb.append( doConvert( indent+s4, n ) ).append("\n");
                }
            });
            
            if ( unittest ) {
                sb.append("test=").append(classOrInterfaceDeclaration.getName()).append("()\n");
                for ( Node n : classOrInterfaceDeclaration.getChildrenNodes() ) {
                    if ( n instanceof MethodDeclaration ) {
                        sb.append("test.").append(((MethodDeclaration) n).getName()).append("()\n");
                    }
                }
            }
            
        
        }
        
        popScopeStack();
        
        return sb.toString();
    }

    private String doConvertMethodDeclaration(String indent, MethodDeclaration methodDeclaration) {
        boolean isStatic= ModifierSet.isStatic(methodDeclaration.getModifiers() );
        
        if ( onlyStatic && !isStatic ) {
            return "";
        }
        
        if ( methodDeclaration.getAnnotations()!=null ) {
            for ( AnnotationExpr a : methodDeclaration.getAnnotations() ) {
                if ( a.getName().getName().equals("Deprecated") ) {
                    return "";
                }
            }
        }
        
        //if ( methodDeclaration.getName().equals("dayOfYear") ) {
        //    System.err.println("here stop");
        //}
        StringBuilder sb= new StringBuilder();
        String comments= utilRewriteComments( indent, methodDeclaration.getComment() );
        sb.append( comments );
        if ( pythonTarget==PythonTarget.python_3_6 && isStatic  && !onlyStatic ) {
            sb.append( indent ).append( "@staticmethod\n" );
        }
        sb.append( indent ).append( "def " ).append( methodDeclaration.getName() ) .append("(");
        boolean comma; 
        
        if ( !isStatic ) {
            sb.append("self");
            comma= true;
        } else {
            comma = false;
        }

        pushScopeStack();

        if ( methodDeclaration.getParameters()!=null ) {
            for ( Parameter p: methodDeclaration.getParameters() ) { 
                String name= p.getId().getName();
                if ( comma ) {
                    sb.append(", ");
                } else {
                    comma = true;
                }
                sb.append( name );
                getCurrentScope().put( name, p.getType() );
            }
        }
        sb.append( "):\n" );
        
        if ( methodDeclaration.getBody()!=null ) {
            sb.append( doConvert( indent, methodDeclaration.getBody() ) );  
        } else {
            sb.append(indent).append(s4).append("pass");  
        }
        popScopeStack();
        
        if ( pythonTarget==PythonTarget.jython_2_2 && isStatic && !onlyStatic ) {
            sb.append(indent).append(methodDeclaration.getName()).append(" = staticmethod(").append(methodDeclaration.getName()).append(")");
            sb.append(indent).append("\n");
        }
        return sb.toString();
    }
    
    private String doConvertFieldDeclaration(String indent, FieldDeclaration fieldDeclaration) {
        boolean s= ModifierSet.isStatic( fieldDeclaration.getModifiers() ); // TODO: static fields
        
        if ( onlyStatic && !s ) {
            return "";
        }

        StringBuilder sb= new StringBuilder();
        
        List<VariableDeclarator> vv= fieldDeclaration.getVariables();
        sb.append( utilRewriteComments( indent, fieldDeclaration.getComment() ) );
        if ( vv!=null ) {
            for ( VariableDeclarator v: vv ) {
                if ( s ) {
                    classFields.put( v.getId().getName(),fieldDeclaration );
                }
                Map<String,Type> currentScope= getCurrentScope();
                currentScope.put( v.getId().getName(),fieldDeclaration.getType() );
                if ( v.getInit()==null ) {
                    String implicitDeclaration = utilImplicitDeclaration( fieldDeclaration.getType() );
                    if ( implicitDeclaration!=null ) {
                        sb.append( indent ) .append( v.getId() ).append("=").append( implicitDeclaration ).append("\n");
                    } else {
                        sb.append( indent ).append("#1266 ") .append( v.getId() ).append("=").append( implicitDeclaration ).append("\n");
                    }
                } else if ( v.getInit() instanceof ConditionalExpr ) {
                    ConditionalExpr ce= (ConditionalExpr)v.getInit();
                    sb.append( indent ).append("if ").append(doConvert( "",ce.getCondition() )).append(":\n");
                    sb.append( indent ).append(s4).append( v.getId()).append("=").append( doConvert( "",ce.getThenExpr() ) ).append("\n");
                    sb.append( indent ).append( "else:\n");
                    sb.append( indent ).append(s4).append( v.getId()).append("=").append( doConvert( "",ce.getElseExpr() ) ).append("\n");
                    
                } else {
                    sb.append( indent ) .append( v.getId() ).append("=").append( doConvert( "",v.getInit() ) ).append("\n");
                    
                }
            }
        }
        return sb.toString();
    }

    private String doConvertThrowStmt(String indent, ThrowStmt throwStmt) {
        return indent + "raise "+ doConvert("",throwStmt.getExpr());
    }

    private String doConvertWhileStmt(String indent, WhileStmt whileStmt) {
        StringBuilder sb= new StringBuilder(indent);
        sb.append( "while ");
        sb.append( doConvert( "", whileStmt.getCondition() ) );
        sb.append( ":\n" );
        sb.append( doConvert( indent, whileStmt.getBody() ) );
        return sb.toString();
    }

    private String doConvertArrayInitializerExpr(String indent, ArrayInitializerExpr arrayInitializerExpr) {
        return indent + "[" + utilFormatExprList( arrayInitializerExpr.getValues() ) + "]";
    }

    
    private String doSwitchStmt(String indent, SwitchStmt switchStmt) {
        String selector= doConvert( "",switchStmt.getSelector() );
        StringBuilder sb= new StringBuilder();
        boolean iff= true;
        int nses= switchStmt.getEntries().size();
        List<Expression> labels= new ArrayList<>();
        for ( int ises = 0; ises<nses; ises++ ) {
            SwitchEntryStmt ses = switchStmt.getEntries().get(ises);
            List<Statement> statements= ses.getStmts();
            if ( statements==null ) {
                // fall-through not supported
                //sb.append("# fall through not supported, need or in if test\n");
                labels.add(ses.getLabel());
                continue;
            }
            
            if ( iff ) {
                StringBuilder cb= new StringBuilder();
                for ( Expression l : labels ) {
                    cb.append(selector).append("==").append(doConvert("",l)).append(" or ");
                }
                cb.append(selector).append("==").append(ses.getLabel());
                sb.append(indent).append("if ").append(cb.toString()).append(":\n");
                iff=false;
            } else {
                StringBuilder cb= new StringBuilder();
                for ( Expression l : labels ) {
                    cb.append(selector).append("==").append(doConvert("",l)).append(" or ");
                }
                if ( ses.getLabel()!=null ) {
                    cb.append(selector).append("==").append(ses.getLabel());
                    sb.append(indent).append("elif ").append(cb.toString()).append(":\n");
                } else {
                    if ( labels.size()>0 ) {
                        sb.append(indent).append("elif ").append(cb.substring(0,cb.length()-4)).append(":\n");
                    } else {
                        sb.append(indent).append("else:\n");
                    }
                }   
            }
            
            labels.clear();
            
            if ( ses.getLabel()==null && ises!=(nses-1) ) {
                throw new IllegalArgumentException("default must be last of switch statement");
            }
            if ( ses.getLabel()!=null && !( ( statements.get(statements.size()-1) instanceof BreakStmt ) ||
                    ( statements.get(statements.size()-1) instanceof ReturnStmt ) ||
                    ( statements.get(statements.size()-1) instanceof ThrowStmt ) ) ) {
                sb.append(indent).append(s4).append("### Switch Fall Through Not Implemented ###\n");
                for ( Statement s: statements ) {
                    sb.append("#").append(doConvert(s4+indent, s )).append("\n");
                }
            } else {
                if ( statements.get(statements.size()-1) instanceof BreakStmt ) {
                    for ( Statement s: statements.subList(0,statements.size()-1) ) {
                        sb.append(doConvert(indent + s4, s )).append("\n");
                    }
                } else {
                    for ( Statement s: statements ) {
                        sb.append(doConvert(indent+s4, s )).append("\n");
                    }
                }

            }
        }
        return sb.toString();
    }

    private static String utilRewriteComments(String indent, Comment comments) {
        if ( comments==null ) return "";
        StringBuilder b= new StringBuilder();
        String[] ss= comments.getContent().split("\n");
        if ( ss[0].trim().length()==0 ) {
            ss= Arrays.copyOfRange( ss, 1, ss.length );
        }
        if ( ss[ss.length-1].trim().length()==0 ) {
            ss= Arrays.copyOfRange( ss, 0, ss.length-1 );
        }
        for ( String s : ss ) {
            int i= s.indexOf("*");
            if ( i>-1 && s.substring(0,i).trim().length()==0 ) {
                s= s.substring(i+1);
            }
            b.append(indent).append("#").append(s).append("\n");
        }
        return b.toString();
    }

    private String doConvertClassOrInterfaceType(String indent, ClassOrInterfaceType classOrInterfaceType) {
        //classOrInterfaceType
        return indent + classOrInterfaceType.getName();
    }

    private String utilQualifyClassName( ClassOrInterfaceType clas ) {
        if ( getCurrentScope().containsKey(clas.getName())) {
            return ((ClassOrInterfaceType)getCurrentScope().get("this")).getName() +"." + clas.getName();
        } else {
            return null;
        }
    }
    
    private String doConvertObjectCreationExpr(String indent, ObjectCreationExpr objectCreationExpr) {
        if ( objectCreationExpr.getType().toString().equals("StringBuilder") ) {
            if ( objectCreationExpr.getArgs()!=null ) {
                if ( objectCreationExpr.getArgs().size()==1 ) {
                    Expression e= objectCreationExpr.getArgs().get(0);
                    return indent + utilAssertStr(e);
                } else {
                    return indent + "\"\""; // TODO: are there any stringbuilder methods which take more than one arg?
                }
            } else {
                return indent + "\"\"";
            }
        } else {
            String qualifiedName= utilQualifyClassName(objectCreationExpr.getType());
            if ( qualifiedName!=null ) {
                return indent + qualifiedName + "("+ utilFormatExprList(objectCreationExpr.getArgs())+ ")";
            } else {
                if ( objectCreationExpr.getAnonymousClassBody()!=null ) {
                    StringBuilder sb= new StringBuilder();
                    sb.append(indent).append(objectCreationExpr.getType()).append("(").append(utilFormatExprList(objectCreationExpr.getArgs())).append(")"); 
                    return sb.toString();
                } else {
                    return indent + objectCreationExpr.getType() + "("+ utilFormatExprList(objectCreationExpr.getArgs())+ ")";
                }
            }
        }
    }

    private String doConvertConstructorDeclaration(String indent, ConstructorDeclaration constructorDeclaration) {
        StringBuilder sb= new StringBuilder();
        String params= utilFormatParameterList( constructorDeclaration.getParameters() );
        sb.append(indent).append("def __init__(self");
        if ( params.trim().length()>0 ) sb.append(",").append(params);
        sb.append("):\n");
        sb.append( doConvert(indent,constructorDeclaration.getBlock()) );
        return sb.toString();
    }

    private String doConvertConditionalExpr(String indent, ConditionalExpr conditionalExpr) {
        //StringBuilder sb= new StringBuilder();
        //sb.append(indent).append("# ConditionalExpr: ").append(conditionalExpr.toString());
        //sb.append(indent).append("if ").append( doConvert("",conditionalExpr.getCondition()) ).append(": ");
        //sb.append(s4).append(indent).append("ce_=").append(doConvert("",conditionalExpr.getThenExpr()));
        //sb.append(indent).append("if ").append( doConvert("",conditionalExpr.getCondition()) ).append(": ");
        //sb.append(s4).append(indent).append("ce_=").append(doConvert("",conditionalExpr.getElseExpr()));
        return indent + doConvert("",conditionalExpr.getThenExpr())
                + " if " + doConvert("",conditionalExpr.getCondition())
                + " else " +  doConvert("",conditionalExpr.getElseExpr());
    }

    private String doConvertCastExpr(String indent, CastExpr castExpr) {
        String type= castExpr.getType().toString();
        if ( type.equals("String") ) {
            type= "str";
        } else if ( type.equals("char") ) {
            type= "str";
        }
        return type + "(" + doConvert("", castExpr.getExpr() ) + ")";
    }

    private String doConvertTryStmt(String indent, TryStmt tryStmt) {
        StringBuilder sb= new StringBuilder();
        sb.append( indent ).append( "try:\n");
        sb.append( doConvert( indent, tryStmt.getTryBlock() ) );
        for ( CatchClause cc: tryStmt.getCatchs() ) {
            sb.append(indent).append("except ").append(doConvert( "",cc.getExcept() )).append(":\n");
            sb.append( doConvert( indent, cc.getCatchBlock() ) );
        }
        if ( tryStmt.getFinallyBlock()!=null ) {
            sb.append( indent ).append( "finally:\n");
            sb.append( doConvert( indent, tryStmt.getFinallyBlock() ) );
        }
        return sb.toString();
    }

    private String doMultiTypeParameter(String indent, MultiTypeParameter multiTypeParameter) {
        return utilFormatTypeList( multiTypeParameter.getTypes() );
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

    public static void main(String[] args ) throws ParseException {
        Convert c= new Convert();
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
        String p= "private static int parseInt(String s) {\n" +
"        int result;\n" +
"        int len= s.length();\n" +
"        for (int i = 0; i < len; i++) {\n" +
"            char c = s.charAt(i);\n" +
"            if (c < 48 || c >= 58) {\n" +
"                throw new IllegalArgumentException(\"only digits are allowed in string\");\n" +
"            }\n" +
"        }\n" +
"        switch (len) {\n" +
"            case 2:\n" +
"                result = 10 * (s.charAt(0) - 48) + (s.charAt(1) - 48);\n" +
"                return result;\n" +
"            case 3:\n" +
"                result = 100 * (s.charAt(0) - 48) + 10 * (s.charAt(1) - 48) + (s.charAt(2) - 48);\n" +
"                return result;\n" +
"            default:\n" +
"                result = 0;\n" +
"                for (int i = 0; i < s.length(); i++) {\n" +
"                    result = 10 * result + (s.charAt(i) - 48);\n" +
"                }\n" +
"                return result;\n" +
"        }\n" +
"    }";
        System.err.println( c.doConvert(p) );
    }

    /**
     * wrap this with "str()" if this is not a string already.
     * @param e
     * @return doConvert(e) or "str(" + doConvert(e) + ")"
     */
    private String utilAssertStr( Expression e ) {
        if ( STRING_TYPE.equals(guessType(e)) ) {
            return doConvert( "", e );
        } else {
            return "str("+doConvert("",e)+")";
        }
        
    }

    private static String utilImplicitDeclaration( Type t ) {
        switch ( t.toString() ) {
            case "byte": 
            case "short":
            case "int":
                return "0";
            case "long":
                return "0L";
            case "float":
            case "double":
                return "0.0";
            case "String":
                return "None";
            case "String[]":
                return "None";
            default:
                return null;
        }
    }
    
    /**
     * take index and length and return "2:5" type notation.
     * @param targetIdx
     * @param targetLen
     * @return 
     */
    private String utilCreateIndexs(Expression targetIdx, Expression targetLen) {
        String targetIndex = doConvert("",targetIdx);
        String targetLength = doConvert("",targetLen);
        if ( targetIndex.equals("0") ) {
            return targetIndex + ":"+targetLength; // it's only one character, so don't bother removing the 0 in 0:
        } else {
            if ( targetIndex.equals(targetLength) && 
                    ( targetIdx instanceof FieldAccessExpr || targetIdx instanceof NameExpr ) ) {
                return targetIndex + ":2*"+targetLength;
            } else {
                return targetIndex + ":" + targetIndex + "+" + targetLength;
            }
        }
    }

    private String doConvertEnumDeclaration(String indent, EnumDeclaration enumDeclaration) { 
        StringBuilder builder= new StringBuilder();
        if ( true ) {        
            builder.append(indent).append("class ").append(enumDeclaration.getName()).append(":\n");
            List<EnumConstantDeclaration> ll = enumDeclaration.getEntries();
            
            for ( Node n: enumDeclaration.getChildrenNodes() ) {
                if ( n instanceof ConstructorDeclaration ) {
                    builder.append( doConvert( indent + s4, n ) );
                }
            }

            for ( EnumConstantDeclaration l : ll ) {
                String args = utilFormatExprList(l.getArgs());
                builder.append(indent).append(enumDeclaration.getName()).append(".").append(l.getName()).append(" = ")
                        .append(enumDeclaration.getName()).append("(").append(args).append(")") .append("\n");
                
            }
            
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
        return builder.toString();
    }
    
}
