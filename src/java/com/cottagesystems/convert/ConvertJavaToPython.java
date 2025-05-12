
package com.cottagesystems.convert;

import com.cottagesystems.convert.ConversionUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.REMAINDER;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.ReferenceType;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Class for converting Java to Jython using an AST.  This also
 * should format using PEP-8 (https://peps.python.org/pep-0008/)
 * but this is a goal that will be difficult to meet.
 *
 * @author jbf
 */
public class ConvertJavaToPython {

    public static final String VERSION = "20240403b";

    public ConvertJavaToPython() {
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

    private JavaParser javaParser = new JavaParser();

    /**
     * the indent level.
     */
    private static final String s4="    ";

    private PythonTarget pythonTarget = PythonTarget.python_3_6;

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

    private boolean camelToSnake = false;

    /**
     * true indicates that variables should be converted to snake_case from camelCase.
     * @return true indicates that variables should be converted to snake_case from camelCase.
     */
    public boolean isCamelToSnake() {
        return camelToSnake;
    }

    /**
     * true indicates that variables should be converted to snake_case from camelCase.
     * @param camelToSnake
     */
    public void setCamelToSnake(boolean camelToSnake) {
        this.camelToSnake = camelToSnake;
    }

    private boolean hasMain= false;

    // Constants
    private static final ReferenceType STRING_TYPE = new ClassOrInterfaceType( "String");

    private static final AnnotationExpr DEPRECATED = new MarkerAnnotationExpr("Deprecated");

    /*** internal parsing state ***/

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

    /*** end, internal parsing state ***/

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
        ParseException throwMe;
        try {
            ByteArrayInputStream ins= new ByteArrayInputStream( javasrc.getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= ConversionUtils.extractParseResult(javaParser.parse(ins,Charset.forName("UTF-8")));
            String src= doConvert( "", unit );
            if ( additionalImports!=null ) {
                StringBuilder sb= new StringBuilder();

                if ( unittest && pythonTarget==PythonTarget.python_3_6 ) {
                    additionalImports.put("import unittest\n", true);
                }

                for ( Entry<String,Boolean> e: additionalImports.entrySet() ) {
                    if ( e.getValue() ) {
                        sb.append( e.getKey() );
                    }
                }

                for (  Entry<String,Boolean> e: additionalClasses.entrySet() ) {
                    if ( e.getValue() ) {
                        sb.append( e.getKey() );
                    }
                }

                boolean first= true;
                for ( Entry<String,Boolean> e: javaImports.entrySet() ) {
                    if ( e.getValue() ) {
                        if ( first ) {
                            sb.append("\n");
                            first= false;
                        }
                        ImportDeclaration id= javaImportDeclarations.get(e.getKey());
                        sb.append( doConvert( "", id ) ).append("\n");
                    }
                }

                sb.append(src);

                if ( unittest && pythonTarget==PythonTarget.python_3_6 ) {
                    sb.append( "if __name__ == '__main__':\n" +
                            "    unittest.main()\n" );
                } else {
                    if ( hasMain ) {
                        if ( isOnlyStatic() ) {
                            sb.append( "if __name__ == '__main__':\n" );
                            sb.append( "    main([])\n\n");
                        } else {
                            //TODO: not sure about this.
                            sb.append( "if __name__ == '__main__':\n" );
                            sb.append( "    " ).append( javaNameToPythonName(theClassName) ).append(".main([])\n\n");
                        }
                    }
                }

                src= sb.toString();


            }
            return src;
        } catch ( ParseException ex ) {
            throwMe= ex;
        }


        int numLinesIn = 0;

        try {
                String[] lines= javasrc.split("\n");
                numLinesIn= lines.length;

                int offset=0;
                Expression parseExpression = ConversionUtils.extractParseResult(javaParser.parseExpression(javasrc));
                StringBuilder bb= new StringBuilder( doConvert( "", parseExpression ) );
                int linesHandled=0;
                while ( (linesHandled+ConversionUtils.getEndLine(parseExpression))<lines.length ) {
                    int additionalLinesHandled=0;
                    for ( int i=0; i<ConversionUtils.getEndLine(parseExpression); i++ ) {
                        offset += lines[i].length() ;
                        offset += 1;
                        additionalLinesHandled++;
                    }
                    if ( offset>javasrc.length() ) {
                        // something went wrong...
                        break;
                    }
                    linesHandled+= additionalLinesHandled;
                    parseExpression = ConversionUtils.extractParseResult(javaParser.parseExpression(javasrc.substring(offset)));
                    bb.append( "\n" ).append( doConvert( "", parseExpression ) );
                }
                String src= bb.toString();

                StringBuilder sb= new StringBuilder();
                for ( Entry<String,Boolean> e: additionalImports.entrySet() ) {
                    if ( e.getValue() ) {
                        sb.append( e.getKey() );
                    }
                }

                for (  Entry<String,Boolean> e: additionalClasses.entrySet() ) {
                    if ( e.getValue() ) {
                        sb.append( e.getKey() );
                    }
                }
                sb.append(src);
                src= sb.toString();

                return src;

        } catch (ParseException ex1) {
            throwMe= ex1;
        }

        try {
            if ( numLinesIn < 2 ) {
                Statement parsed = ConversionUtils.extractParseResult(javaParser.parseStatement(javasrc));
                return doConvert("", parsed);
            }
        } catch (ParseException ex2) {
            throwMe= ex2;
        }

        try {
            if ( numLinesIn < 2 ) {
                BodyDeclaration parsed = ConversionUtils.extractParseResult(javaParser.parseBodyDeclaration(javasrc));
                return doConvert("", parsed);
            }
        } catch (ParseException ex3 ) {
            throwMe= ex3;
        }
        try {
            Statement parsed = ConversionUtils.extractParseResult(javaParser.parseBlock(javasrc));
            return doConvert("", parsed);
        } catch ( ParseException ex ) {
            throwMe =ex;
        }

        try {
            String ssrc= utilMakeClass(javasrc);
            ByteArrayInputStream ins= new ByteArrayInputStream( ssrc.getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= ConversionUtils.extractParseResult(javaParser.parse(ins,Charset.forName("UTF-8")));
            String src= doConvert( "", unit );
            src= utilUnMakeClass(src);

            if ( additionalImports!=null ) {
                StringBuilder sb= new StringBuilder();

                for ( Entry<String,Boolean> e: additionalImports.entrySet() ) {
                    if ( e.getValue() ) {
                        sb.append( e.getKey() );
                    }
                }

                for (  Entry<String,Boolean> e: additionalClasses.entrySet() ) {
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

    private String doConvertBinaryExpr(String indent,BinaryExpr b) {
        String left= doConvert(indent,b.getLeft());
        String right= doConvert(indent,b.getRight());
        Type leftType = guessType(b.getLeft());
        Type rightType = guessType(b.getRight());

        if ( b.getLeft() instanceof MethodCallExpr && b.getRight() instanceof IntegerLiteralExpr ) {
            MethodCallExpr mce= (MethodCallExpr)b.getLeft();
            if ( mce.getName().asString().equals("compareTo")
                    && ((IntegerLiteralExpr)b.getRight()).toString().equals("0") ) {
                if ( null!=b.getOperator() ) {
                    // TODO: Should we avoid dying when scope isn't available?
                    // TODO: avoid repeated code below.
                    Expression scope = mce.getScope().get();
                    switch (b.getOperator()) {
                        case GREATER:
                            return doConvert("",mce.getScope().get()) + " > " + doConvert("",mce.getArguments().get(0));
                        case GREATER_EQUALS:
                            return doConvert("",mce.getScope().get()) + " >= " + doConvert("",mce.getArguments().get(0));
                        case LESS:
                            return doConvert("",mce.getScope().get()) + " < " + doConvert("",mce.getArguments().get(0));
                        case LESS_EQUALS:
                            return doConvert("",mce.getScope().get()) + " <= " + doConvert("",mce.getArguments().get(0));
                        case EQUALS:
                            return doConvert("",mce.getScope().get()) + " == " + doConvert("",mce.getArguments().get(0));
                        default:
                            break;
                    }
                }
            }
        }
        BinaryExpr.Operator op= b.getOperator();
        if (  rightType!=null && rightType.equals(new PrimitiveType(Primitive.CHAR)) && left.startsWith("ord(") && left.endsWith(")") ) {
            left= left.substring(4,left.length()-1);
        }

        if ( leftType!=null && rightType!=null ) {
            if ( leftType.equals(new PrimitiveType(Primitive.CHAR)) && rightType.equals(new PrimitiveType(Primitive.INT)) ) {
                left= "ord("+left+")";
            }
            if ( leftType.equals(new PrimitiveType(Primitive.INT)) && rightType.equals(new PrimitiveType(Primitive.CHAR)) ) {
                left= "ord("+left+")";
            }
            if ( rightType.equals(STRING_TYPE)
                    && leftType instanceof PrimitiveType
                    && !leftType.equals(new PrimitiveType(Primitive.CHAR)) ) {
                left= "str("+left+")";
            }
            if ( leftType.equals(STRING_TYPE)
                    && rightType instanceof PrimitiveType
                    && !rightType.equals(new PrimitiveType(Primitive.CHAR)) ) {
                right= "str("+right+")";
            }
        }

        if ( leftType!=null && !( b.getRight() instanceof NullLiteralExpr )
                && leftType.equals(new ClassOrInterfaceType("String")) && rightType==null ) {
            right= "str("+right+")";
        }

        switch (op) {
            case PLUS:
                return left + " + " + right;
            case MINUS:
                return left + " - " + right;
            case DIVIDE:
                if ( pythonTarget==PythonTarget.python_3_6 && ConversionUtils.isIntegerType( leftType ) && ConversionUtils.isIntegerType( rightType ) ) {
                    return left + " // " + right;
                } else {
                    return left + " / " + right;
                }
            case MULTIPLY:
                return left + " * " + right;
            case GREATER:
                return left + " > " + right;
            case LESS:
                return left + " < " + right;
            case GREATER_EQUALS:
                return left + " >= " + right;
            case LESS_EQUALS:
                return left + " <= " + right;
            case AND:
                return left + " and " + right;
            case OR:
                return left + " or " + right;
            case EQUALS:
                if ( b.getRight() instanceof NullLiteralExpr ) {
                    return left + " is " + right;
                } else {
                    return left + " == " + right;
                }
            case NOT_EQUALS:
                return left + " != " + right;
            case REMAINDER:
                return left + " % " + right;
            case LEFT_SHIFT:
                return left + " << " + right;
            default:
                throw new IllegalArgumentException("not supported: "+op);
        }
    }

    /**
     * guess the class type, and return null if there is no guess.
     * @param clas
     * @return
     */
    private Type guessType( Expression clas ) {
        ReferenceType STRING = new ClassOrInterfaceType("String");
        if ( clas instanceof NameExpr ) {
            String clasName= ((NameExpr)clas).getName().asString();
            if ( Character.isUpperCase(clasName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                new ClassOrInterfaceType( clasName );
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
            return guessType(ue.getExpression());
        } else if ( clas instanceof BinaryExpr ) {
            BinaryExpr be= ((BinaryExpr)clas);
            Type leftType= guessType( be.getLeft() );
            Type rightType= guessType( be.getRight() );
            if ( be.getOperator()==BinaryExpr.Operator.AND ||
                    be.getOperator()==BinaryExpr.Operator.OR ||
                    be.getOperator()==BinaryExpr.Operator.EQUALS ||
                    be.getOperator()==BinaryExpr.Operator.NOT_EQUALS ||
                    be.getOperator()==BinaryExpr.Operator.GREATER ||
                    be.getOperator()==BinaryExpr.Operator.GREATER_EQUALS ||
                    be.getOperator()==BinaryExpr.Operator.LESS ||
                    be.getOperator()==BinaryExpr.Operator.LESS_EQUALS ) {
                return new PrimitiveType(Primitive.BOOLEAN);
            }
            if ( be.getOperator()==BinaryExpr.Operator.PLUS ) { // we automatically convert ints to strings.
                if ( ( leftType!=null && leftType.equals(STRING) ) ||
                        ( rightType!=null && rightType.equals(STRING) ) ) {
                    return STRING;
                }
            }

            return ConversionUtils.findBinaryExprType( leftType, rightType );

        } else if ( clas instanceof FieldAccessExpr ) {
            String fieldName= ((FieldAccessExpr)clas).getNameAsString();
            return getCurrentScope().get(fieldName);
        } else if ( clas instanceof ArrayAccessExpr ) {
            ArrayAccessExpr aae= (ArrayAccessExpr)clas;
            Type arrayType=  guessType( aae.getName() );
            if ( arrayType==null ) {
                return null;
            } else if ( arrayType instanceof ArrayType && ((ArrayType)arrayType).getArrayLevel()==1 ) {
                return ((ArrayType) arrayType).getElementType();
            } else {
                return null;
            }
        } else if ( clas instanceof IntegerLiteralExpr ) {
            return new PrimitiveType(Primitive.INT);
        } else if ( clas instanceof CharLiteralExpr ) {
            return new PrimitiveType(Primitive.CHAR);
        } else if ( clas instanceof LongLiteralExpr ) {
            return new PrimitiveType(Primitive.LONG);
        } else if ( clas instanceof DoubleLiteralExpr ) {
            return new PrimitiveType(Primitive.DOUBLE);
        } else if ( clas instanceof StringLiteralExpr ) {
            return STRING_TYPE;
        } else if ( clas instanceof MethodCallExpr ) {
            MethodCallExpr mce= (MethodCallExpr)clas;
            Type scopeType=null;
            if ( mce.getScope().isPresent() ) scopeType= guessType(mce.getScope().get());
            MethodDeclaration md= getCurrentScopeMethods().get(mce.getName());
            if ( md!=null ) {
                return md.getType();
            }
            if ( scopeType!=null ) {
                if (  scopeType.toString().equals("Pattern") ) {
                    if ( mce.getName().asString().equals("matcher") ) {
                        return new ClassOrInterfaceType("Matcher");
                    }
                } else if ( scopeType.toString().equals("StringBuilder") ) {
                    switch ( mce.getNameAsString() ) {
                        case "toString":
                            return new ClassOrInterfaceType("String");
                    }
                } else if ( scopeType.toString().equals("String") ) {
                    switch ( mce.getNameAsString() ) {
                        case "substring":
                        case "trim":
                        case "valueOf":
                        case "concat":
                        case "replace":
                        case "replaceAll":
                        case "toLowerCase":
                        case "toUpperCase":
                            return scopeType;
                        case "length":
                            return new PrimitiveType(Primitive.INT);
                        case "charAt":
                            return new PrimitiveType(Primitive.CHAR);
                    }
                } else if ( scopeType.toString().equals("Arrays") ) {
                    switch ( mce.getNameAsString() ) {
                        case "copyOfRange":
                            return guessType( mce.getArguments().get(0) );
                    }
                }
            }
            switch ( mce.getNameAsString() ) { // TODO: consider t
                case "charAt": return new PrimitiveType(Primitive.CHAR);
                case "copyOfRange": return guessType( mce.getArguments().get(0) );
            }
        }
        return null;
    }

    static final HashSet stringMethods= new HashSet();

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

    static final HashSet characterMethods= new HashSet();
    static {
        characterMethods.add("isDigit");
        characterMethods.add("isSpace");
        characterMethods.add("isWhitespace");
        characterMethods.add("isLetter");
    }

    private String utilRemoveEscapes( String regex ) {
        if ( regex.contains("\\") ) {
            regex= regex.replaceAll("\\\\","");
        }
        return regex;
    }

    /**
     * This is somewhat the "heart" of this converter, where Java library use is translated to Python use.
     * @param indent
     * @param methodCallExpr
     * @return
     */
    private String doConvertMethodCallExpr(String indent,MethodCallExpr methodCallExpr) {
        // TODO: what if scope is missing.
        Expression clas= methodCallExpr.getScope().get();
        String name= methodCallExpr.getName().asString();
        List<Expression> args= methodCallExpr.getArguments();

        if ( name==null ) {
            name=""; // I don't think this happens
        }

        /**
         * try to identify the class of the scope, which could be either a static or non-static method.
         */
        String clasType="";
        if ( clas instanceof NameExpr ) {
            String contextName= ((NameExpr)clas).getName().asString(); // sb in sb.append, or String in String.format.
            Type contextType= localVariablesStack.peek().get(contextName); // allow local variables to override class variables.
            if ( contextType==null ) contextType= getCurrentScope().get(contextName);
            if ( Character.isUpperCase(contextName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                clasType= contextName;
            } else if ( stringMethods.contains(name)
                    && ( contextType==null || contextType.equals(new ClassOrInterfaceType("String") )) ) {
                clasType= "String";
            } else if ( characterMethods.contains(name)
                    && ( contextType==null || contextType.equals(new ClassOrInterfaceType("String") )) ) {
                clasType= "Character";
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
                if ( new ClassOrInterfaceType("String").equals(guessType( clas )) ) {
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

        // remove diamond typing (Map<String,String> -> Map)
        int i= clasType.indexOf("<");
        if ( i>=0 ) {
            clasType= clasType.substring(0,i);
        }

        if ( clasType.equals("StringBuilder") ) {
            if ( name.equals("append") ) {
                return indent + doConvert("",clas) + "+= " + utilAssertStr(args.get(0)) ;
            } else if ( name.equals("toString") ) {
                return indent + doConvert("",clas);
            } else if ( name.equals("insert") ) {
                String n= doConvert("",clas);
                String i0= doConvert("",args.get(0));
                String ins= doConvert("",args.get(1));
                return indent + n + " = ''.join( ( " + n + "[0:"+ i0 + "], "+ins+", " + n + "["+i0+":] ) )  # J2J expr -> assignment"; // expr becomes assignment, this will cause problems
            }
        }
        if ( clasType.equals("Collections") ) {
            switch (name) {
                case "emptyMap":
                    return indent + "{}";
                case "emptySet":
                    return indent + "{}";  // Jython 2.2 has no set type
                case "emptyList":
                    return indent + "[]";
                case "singletonList":
                    return indent + "["+doConvert("",args.get(0)) + "]";
                default:
                    break;
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
                case "floorDiv":
                    return doConvert(indent,args.get(0)) + "//" + doConvert(indent,args.get(1));
                case "floorMod":
                    String x= doConvert(indent,args.get(0));
                    String y= doConvert(indent,args.get(1));
                    return "(" + x + " - " + y + " * ( "+x+"//"+y + ") )";
                case "floor":
                case "ceil":
                case "cos":
                case "sin":
                case "tan":
                case "acos":
                case "asin":
                case "atan":
                case "sqrt":
                case "log":
                    additionalImports.put("import math\n", true);
                    return "math." + name + "(" + doConvert(indent,args.get(0)) + ")";
                case "log10":
                    additionalImports.put("import math\n", true);
                    return "math.log(" + doConvert(indent,args.get(0)) + ",10)";
                case "round":
                    return name + "(" + doConvert(indent,args.get(0)) + ")";
                case "toRadians":
                    additionalImports.put("import math\n", true);
                    return "math.radians(" + doConvert(indent,args.get(0)) + ")";
                case "toDegrees":
                    additionalImports.put("import math\n", true);
                    return "math.degrees(" + doConvert(indent,args.get(0)) + ")";
                default:
                    break;
            }
        }
        if ( clasType.equals("HashMap") || clasType.equals("Map") ) {

            switch (name) {
                case "put":
                    return indent + doConvert("",clas) + "["+doConvert("",args.get(0))+"] = "+doConvert("",args.get(1));
                case "get":
                    //String map= doConvert("",clas);
                    //String key= doConvert("",args.get(0));
                    //return indent + "(" + map + "["+key+"] if "+key +" in "+ map + " else None)";
                    return indent + doConvert("",clas) + "["+doConvert("",args.get(0))+"]";
                case "containsKey": // Note that unlike Java, getting a key which doesn't exist is a runtime error.
                    return indent + doConvert("",args.get(0)) + " in "+ doConvert("",clas);
                case "remove":
                    return indent + doConvert("",clas) + ".pop(" + doConvert("",args.get(0)) + ")";
                case "addAll":
                    return indent + doConvert("",clas) + ".update(" + doConvert("",args.get(0)) + ")";
                case "size":
                    return indent + "len(" + doConvert("",clas) + ")";

                default:
                    break;
            }
        }

        if ( clasType.equals("HashSet") || clasType.equals("Set") ) {
            switch (name) {
                case "contains":
                    return indent + doConvert("",args.get(0)) + " in " + doConvert("",clas);
                case "add":
                    return indent + doConvert("",clas) + "["+doConvert("",args.get(0))+"] = "+doConvert("",args.get(0)); // Jython 2.2 no sets
                case "remove":
                    return indent + doConvert("",clas) + ".pop(" + doConvert("",args.get(0)) + ")";
                case "addAll":
                    String a= doConvert("",args.get(0));
                    String d= "dict( zip( " + a + ","+ a +") )";
                    return indent + doConvert("",clas) + ".update( " + d + ")";
                case "size":
                    return indent + "len(" + doConvert("",clas) + ")";
            }
        }

        if ( clasType.equals("ArrayList") || clasType.equals("List") ) {
            switch (name) {
                case "size":
                    return indent + "len(" + doConvert("",clas) + ")";
                case "add":
                    if ( args.size()==2 ) {
                        return indent + doConvert("",clas) + ".insert("+doConvert("",args.get(0))+","+doConvert("",args.get(1))+")";
                    } else {
                        return indent + doConvert("",clas) + ".append("+doConvert("",args.get(0))+")";
                    }
                case "remove":
                    if ( guessType(args.get(0)).equals(new PrimitiveType(Primitive.INT)) ) {
                        String item = doConvert("",clas) + "["+doConvert("",args.get(0))+"]";
                        return indent + doConvert("",clas) + ".remove(" + item + ")";
                    } else {
                        return indent + doConvert("",clas) + ".remove(" + doConvert("",args.get(0)) + ")";
                    }
                case "get":
                    return indent + doConvert("",clas) + "["+doConvert("",args.get(0))+"]";
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

        if ( clasType.equals("Logger") ) {
            return indent + "# J2J (logger) "+methodCallExpr.toString();
        }
        if ( clasType.equals("String") ) {
            switch (name) {
                case "format":
                    if ( clas.toString().equals("String") ) {
                        StringBuilder sb= new StringBuilder();
                        sb.append(indent).append( doConvert("",args.get(0)) ).append(" % ");
                        if ( args.size()==2 ) {
                            if ( args.get(1) instanceof BinaryExpr ) {
                                sb.append("(").append(doConvert( "", args.get(1) )).append(")");
                            } else {
                                sb.append( doConvert( "", args.get(1) ) );
                            }
                        } else {
                            sb.append("(");
                            sb.append( utilFormatExprList( args.subList(1, args.size() ) ) );
                            sb.append(")");
                        }
                        return sb.toString();
                    } else {
                        break;
                    }
                case "substring":
                    if ( args.size()==2 ) {
                        return doConvert(indent,clas)+"["+ doConvert("",args.get(0)) + ":"+ doConvert("",args.get(1)) +"]";
                    } else {
                        return doConvert(indent,clas)+"["+ doConvert("",args.get(0)) +":]";
                    }
                case "indexOf":
                    if ( args.size()==1 ) {
                        return doConvert(indent,clas)+".find("+ doConvert("",args.get(0)) + ")";
                    } else {
                        return doConvert(indent,clas)+".find("+ doConvert("",args.get(0)) + "," + doConvert("",args.get(1))+")";
                    }
                case "lastIndexOf":
                    if ( args.size()==1 ) {
                        return doConvert(indent,clas)+".rfind("+ doConvert("",args.get(0)) + ")";
                    } else {
                        return doConvert(indent,clas)+".rfind("+ doConvert("",args.get(0)) + ",0," + doConvert("",args.get(1))+")";
                    }
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
                    return doConvert(indent,clas)+".lower() == "+ utilFormatExprList(args) +".lower()";
                case "trim":
                    return doConvert(indent,clas)+".strip()";
                case "replace":
                    String search = doConvert("",args.get(0));
                    String replac = doConvert("",args.get(1));
                    return doConvert(indent,clas)+".replace("+search+", "+replac+")";
                case "replaceAll":
                    additionalImports.put("import re\n",Boolean.TRUE);
                    search= doConvert("",args.get(0));
                    replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                    return indent + "re.sub("+search+", "+replac+", "+doConvert("",clas)+")";
                case "replaceFirst":
                    search= doConvert("",args.get(0));
                    replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                    additionalImports.put("import re\n",Boolean.TRUE);
                    return indent + "re.sub("+search+", "+replac+", "+doConvert("",clas)+",1)";
                case "valueOf":
                    return indent + "str("+doConvert("",args.get(0)) +")";
                case "split":
                    String arg= utilUnquoteReplacement( doConvert("",args.get(0)) );
                    return indent + doConvert(indent,clas) + ".split("+arg+")";
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
                    return s + ".isdigit()";
                case "isSpace":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isspace()";
                case "isWhitespace":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isspace()";
                case "isLetter":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isalpha()";
                case "isAlphabetic":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("ord(") && s.endsWith(")") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return s + ".isalpha()";
                default:
                    break;
            }
        }
        if ( clasType.equals("Arrays") ) {
            switch (name) {
                case "copyOfRange": {
                    StringBuilder sb= new StringBuilder();
                    sb.append(indent).append(args.get(0)).append("[");
                    sb.append(doConvert("",args.get(1))).append(":").append(doConvert("",args.get(2)));
                    sb.append("]");
                    return sb.toString();
                }
                case "equals": {
                    // if they are not objects, then Python == can be used.
                    StringBuilder sb= new StringBuilder();
                    Type t1= guessType(args.get(0)); // are both arrays containing primative objects (int,float,etc)
                    Type t2= guessType(args.get(1));
                    if ( t1!=null && t1.equals(new ArrayType(new PrimitiveType(Primitive.INT)))
                            && t2!=null && t2.equals(new ArrayType(new PrimitiveType(Primitive.INT))) ) {
                        sb.append(indent).append(doConvert("",args.get(0))).append(" == ");
                        sb.append(doConvert("",args.get(1)));
                        return sb.toString();
                    }
                }
                case "toString": {
                    Type t= guessType(args.get(0));
                    String js;
                    if ( t instanceof ArrayType && ((ArrayType)t).getArrayLevel()==1 && ConversionUtils.isIntegerType(((ArrayType)t).getElementType()) ) {
                        if ( pythonTarget==PythonTarget.jython_2_2 ) {
                            js= "', '.join( map( str, "+doConvert("",args.get(0))+" ) )";
                        } else {
                            js= "', '.join( str(x) for x in "+doConvert("",args.get(0))+" )";
                        }
                    } else {
                        js= "', '.join("+doConvert("",args.get(0))+")";
                    }
                    return "('['+"+js + "+']')";
                }
                case "asList":
                    // since in Python we are treating lists and arrays as the same thing, do nothing.
                    return doConvert("",args.get(0));

            }
        }
        if ( clasType.equals("Double") ) {
            switch ( name ) {
                case "parseDouble":
                    return "float("+doConvert( "", args.get(0))+")";
            }
        }
        if ( clasType.equals("Float") ) {
            switch ( name ) {
                case "parseFloat":
                    return "float("+doConvert( "", args.get(0))+")";
            }
        }
        if ( clasType.equals("Integer") ) {
            switch ( name ) {
                case "parseInt":
                    return "int("+ doConvert( "", args.get(0) ) +")";
            }
        }
        if ( clasType.equals("Pattern") ) {
            additionalImports.put("import re\n",Boolean.TRUE);
            switch ( name ) {
                case "compile":
                    return "re.compile("+doConvert("",args.get(0))+")";
                case "quote":
                    return "re.escape("+doConvert("",args.get(0))+")";
            }
        }
        if ( clasType.equals("Pattern")
                && name.equals("matcher") ) {
            return doConvert("",clas) + ".match(" + doConvert("",args.get(0) ) + ")";
        }
        if ( clasType.equals("Matcher") ) {
            if ( name.equals("matches") ) {
                return doConvert("",clas) + " != None";
            } else if ( name.equals("find") ) {
                if ( clas instanceof MethodCallExpr ) {
                    return doConvert("",clas).replaceAll("match", "search") + "!=None";
                } else {
                    return doConvert("",clas) + "!=None  #j2j: USE search not match above";
                }
            }
        }

        if ( pythonTarget==PythonTarget.python_3_6 && unittest && clas==null ) {
            if ( name.equals("assertEquals") || name.equals("assertArrayEquals") ) {
                StringBuilder sb= new StringBuilder();
                sb.append(indent).append( "self.assertEqual(" );
                sb.append(doConvert("",args.get(0))).append(",").append(doConvert("",args.get(1)));
                sb.append(")");
                return sb.toString();
            }
        }


        if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getNameAsString().equals("err") ) {
            StringBuilder sb= new StringBuilder();
            additionalImports.put("import sys\n",Boolean.TRUE);
            if (  methodCallExpr.getArguments().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArguments().get(0) );
                String sWithNewLine= s.substring(0,s.length()-1) + "\\n'";
                sb.append(indent).append( "sys.stderr.write(" ).append(sWithNewLine).append(")");
            } else {
                String strss;
                if ( !isStringType( guessType(methodCallExpr.getArguments().get(0)) ) ) {
                    strss= "str( "+ doConvert( "", methodCallExpr.getArguments().get(0) ) + ")";
                } else {
                    strss= doConvert( "", methodCallExpr.getArguments().get(0) );
                }
                sb.append(indent).append( "sys.stderr.write(" ).append( strss ).append( "+'\\n')" );
            }
            return sb.toString();
        } else if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getNameAsString().equals("out") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArguments().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArguments().get(0) );
                sb.append(indent).append( "print(" ).append( s ).append( ")" );
            } else {
                sb.append(indent).append( "print(" ).append( doConvert( "", methodCallExpr.getArguments().get(0) ) ).append( ")" );
            }
            return sb.toString();
        } else if ( name.equals("print") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getNameAsString().equals("err") ) {
            additionalImports.put("import sys\n",Boolean.TRUE);
            StringBuilder sb= new StringBuilder();
            String s= doConvert( "", methodCallExpr.getArguments().get(0) );
            sb.append(indent).append( "sys.stderr.write(" ).append(s).append(")");
            return sb.toString();
        } else if ( name.equals("print") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getNameAsString().equals("out") ) {
            StringBuilder sb= new StringBuilder();
            if ( pythonTarget==PythonTarget.jython_2_2 ) {
                sb.append(indent).append("print ");
                if ( methodCallExpr.getArguments().get(0) instanceof StringLiteralExpr ) {
                    String s= doConvert( "", methodCallExpr.getArguments().get(0) );
                    sb.append( s ).append(",");
                } else {
                    sb.append( doConvert( "", methodCallExpr.getArguments().get(0) ) );
                }

                sb.append( "," );
            } else {
                additionalImports.put("import sys\n",Boolean.TRUE);
                if (  methodCallExpr.getArguments().get(0) instanceof StringLiteralExpr ) {
                    String s= doConvert( "", methodCallExpr.getArguments().get(0) );
                    sb.append(indent).append( "sys.stdout.write(" ).append( s ).append( ")" );
                } else {
                    sb.append(indent).append( "sys.stdout.write(" ).append( doConvert( "", methodCallExpr.getArguments().get(0) ) ).append( ")" );
                }
            }
            return sb.toString();
        } else if ( name.equals("length") && args==null ) {
            return indent + "len("+ doConvert("",clas)+")";
        } else if ( name.equals("equals") && args.size()==1 ) {
            return indent + doConvert(indent,clas)+" == "+ utilFormatExprList(args);
        } else if ( name.equals("arraycopy") && clasType.equals("System") ) {
            String target = doConvert( "", methodCallExpr.getArguments().get(2) );
            String source = doConvert( "", methodCallExpr.getArguments().get(0) );
            Expression sourceIdx = methodCallExpr.getArguments().get(1);
            Expression targetIdx = methodCallExpr.getArguments().get(3);
            Expression length= methodCallExpr.getArguments().get(4);
            String targetIndexs= utilCreateIndexs(targetIdx, length);
            String sourceIndexs= utilCreateIndexs(sourceIdx, length);
            String j= String.format( "%s[%s]=%s[%s]",
                    target, targetIndexs, source, sourceIndexs );
            return indent + j;
        } else if ( name.equals("exit") && clasType.equals("System") ) {
            additionalImports.put( "import sys\n", Boolean.TRUE );
            return indent + "sys.exit("+ doConvert("",methodCallExpr.getArguments().get(0)) + ")";

        } else {
            if ( clasType.equals("System") && name.equals("currentTimeMillis") ) {
                additionalImports.put("from java.lang import System\n",Boolean.FALSE);
            } else if ( clasType.equals("Double") ) {
                additionalImports.put("from java.lang import Double\n",Boolean.FALSE);
            } else if ( clasType.equals("Integer") ) {
                additionalImports.put("from java.lang import Integer\n",Boolean.FALSE);
            } else if ( clasType.equals("Short") ) {
                additionalImports.put("from java.lang import Short\n",Boolean.FALSE);
            } else if ( clasType.equals("Character") ) {
                additionalImports.put("from java.lang import Character\n",Boolean.FALSE);
            } else if ( clasType.equals("Byte") ) {
                additionalImports.put("from java.lang import Byte\n",Boolean.FALSE);
            } else if ( clasType.equals("IllegalArgumentException") ) {
                additionalImports.put("from java.lang import IllegalArgumentException\n",Boolean.FALSE);
            }

            if ( javaImports.keySet().contains(clasType) ) {
                javaImports.put(clasType,true);
            }

            if ( clas==null ) {
                ClassOrInterfaceDeclaration m= classMethods.get(name);
                if ( m!=null ) {
                    MethodDeclaration mm= this.getCurrentScopeMethods().get(name);
                    if ( mm==null ) {
                        return indent + m.getName() + "." + name + "("+ utilFormatExprList(args) +")";
                    } else {
                        boolean isStatic= ConversionUtils.isStaticMethod(mm);
                        if ( isStatic ) {
                            return indent + javaNameToPythonName( m.getNameAsString() ) + "." + javaNameToPythonName( name ) + "("+ utilFormatExprList(args) +")";
                        } else {
                            return indent + "self." + javaNameToPythonName( name ) + "("+ utilFormatExprList(args) +")";
                        }
                    }
                } else {
                    return indent + javaNameToPythonName( name ) + "("+ utilFormatExprList(args) +")";
                }
            } else {
                String clasName = doConvert("",clas);
                if ( name.equals("append") && clas instanceof MethodCallExpr && args.size()==1 ) {
                    return indent + clasName + " + " + utilAssertStr( args.get(0));

                } else {
                    if ( onlyStatic && clasName.equals(theClassName) )  {
                        return indent            + javaNameToPythonName( name ) + "("+ utilFormatExprList(args) +")";
                    } else {
                        return indent + clasName +"."+javaNameToPythonName( name )+ "("+ utilFormatExprList(args) +")";
                    }
                }
            }
        }
    }

    private String doConvertAssignExpr(String indent, AssignExpr assign ) {
        String target= doConvert( "", assign.getTarget() );
        Operator operator= assign.getOperator();

        switch (operator) {
            case MINUS:
                return indent +target + " -= " + doConvert( "", assign.getValue() );
            case PLUS:
                return indent + target + " += " + doConvert( "", assign.getValue() );
            case MULTIPLY:
                return indent + target + " *= " + doConvert( "", assign.getValue() );
            case DIVIDE:
                return indent + target + " /= " + doConvert( "", assign.getValue() );
            case BINARY_OR:
                return indent + target + " |= " + doConvert( "", assign.getValue() );
            case BINARY_AND:
                return indent + target + " &= " + doConvert( "", assign.getValue() );
            case ASSIGN:
                return indent + target + " = " + doConvert( "", assign.getValue() );
            default:
                return indent + target + " ??? " + doConvert( "", assign.getValue() ) + " (J2J AssignExpr not supported)";
        }
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

        //if ( n.getBeginLine()>260 && simpleName.equals("FieldAccessExpr") ) {
        //    if ( n.toString().contains("VersioningType") ) {
        //        System.err.println("here is the thing you were looking for: "+ n); //switching to parsing end time
        //    }
        //}

        String result;

        switch ( simpleName ) {
            case "foo":
                result= "foo";
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
                result= indent + "None";
                break;
            case "BooleanLiteralExpr":
                result= indent + ( ((BooleanLiteralExpr)n).getValue() ? "True" : "False" );
                break;
            case "LongLiteralExpr":
                if ( pythonTarget==PythonTarget.jython_2_2 ) {
                    result= indent + ((LongLiteralExpr)n).getValue();
                } else if ( pythonTarget==PythonTarget.python_3_6 ) {
                    result= indent + ((LongLiteralExpr)n).getValue().replace("L",""); // all ints are longs.
                } else {
                    throw new IllegalStateException("unsupported python target "+pythonTarget);
                }
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
                result= doConvertBlockStmt(indent+s4,(BlockStmt)n);
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
                result= "self";
                break;
            case "ImportDeclaration":
                result= doConvertImportDeclaration(indent,(ImportDeclaration)n);
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
                result= doConvertClassOrInterfaceType(indent,(ClassOrInterfaceType)n); // TODO: this looks suspicious
                break;
            case "ConstructorDeclaration":
                result= doConvertConstructorDeclaration(indent,(ConstructorDeclaration)n);
                break;
            case "EnumDeclaration":
                result= doConvertEnumDeclaration(indent,(EnumDeclaration)n);
                break;
            case "InitializerDeclaration":
                result= doConvertInitializerDeclaration(indent,(InitializerDeclaration)n);
                break;
            case "ObjectCreationExpr":
                result= doConvertObjectCreationExpr(indent,(ObjectCreationExpr)n);
                break;
            case "ClassOrInterfaceType":
                result= doConvertClassOrInterfaceType(indent,(ClassOrInterfaceType)n);
                break;
            case "Parameter":
                result= indent + ((Parameter)n).getNameAsString(); // TODO: varargs, etc
                break;
            case "ForEachStmt":
                result= doConvertForEachStmt(indent,(ForEachStmt)n);
                break;
            case "EmptyStmt":
                result= doConvertEmptyStmt(indent,(EmptyStmt)n);
                break;
            case "Modifier":
                result= "";
                break;
            case "SimpleName":
                result= "";
                break;
            case "SuperExpr":
                SuperExpr se= (SuperExpr)n;
                if ( pythonTarget==PythonTarget.jython_2_2 ) {
                    result= indent + "*** "+simpleName + "*** " + n.toString() + "*** end "+simpleName + "****";
                } else {
                    result= indent + "super()";
                }
                break;
            case "LineComment":
                result= "#" + ((Comment)n).getContent().replace("\n", "");
                break;
            case "BlockComment":
                result= utilRewriteComments(indent, Optional.of((Comment)n));
                break;
            default:
                result= indent + "*** "+simpleName + "*** " + n.toString() + "*** end "+simpleName + "****";
                break;
        }
        if ( result.contains("(int(round(nn)) )") ) {
            System.err.println("here stop after convert");
        }
        return result;
    }


    private String doConvertStringLiteralExpr(String indent,StringLiteralExpr stringLiteralExpr) {
        String s= stringLiteralExpr.getValue();
        s= s.replaceAll("'","\\\\'");
        return "'" + s + "'";
    }

    private String doConvertBlockStmt(String indent,BlockStmt blockStmt) {

        pushScopeStack(true);

        StringBuilder result= new StringBuilder();
        List<Statement> statements= blockStmt.getStatements();
        if ( statements==null ) {
            popScopeStack();
            return indent + "pass\n";
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
        if ( lines==0 ) {
            result.append(indent).append("pass\n");
        }
        popScopeStack();
        return result.toString();
    }

    private String doConvertExpressionStmt(String indent, ExpressionStmt expressionStmt) {
        StringBuilder sb= new StringBuilder();
        if ( expressionStmt.getComment().isPresent() ) {
            sb.append( utilRewriteComments(indent, expressionStmt.getComment() ) );
        }
        sb.append( doConvert( indent, expressionStmt.getExpression() ) );
        return sb.toString();
    }

    private String doConvertVariableDeclarationExpr(String indent, VariableDeclarationExpr variableDeclarationExpr) {
        StringBuilder b= new StringBuilder();

        for ( int i=0; i<variableDeclarationExpr.getVariables().size(); i++ ) {
            if ( i>0 ) b.append("\n");
            VariableDeclarator v= variableDeclarationExpr.getVariables().get(i);
            String s= v.getNameAsString();
            if ( v.getInitializer().isPresent() && v.getInitializer().get().toString().startsWith("Logger.getLogger") ) {
                //addLogger();
                localVariablesStack.peek().put(s,new ClassOrInterfaceType("Logger") );
                return indent + "# J2J: "+variableDeclarationExpr.toString().trim();
            }
            if ( camelToSnake ) {
                String news= camelToSnakeAndRegister(s);
                s= news;
            }
            if ( s.equals("len") ) {
                String news= "lenJ2J";
                nameMapForward.put( s, news );
                nameMapReverse.put( news, s );
                s= news;
            }
            if ( v.getInitializer().isPresent()
                    && ( v.getInitializer().get() instanceof ArrayInitializerExpr )
                    && ( v.getType() instanceof PrimitiveType ) ) {
                Type t= new ArrayType( ((PrimitiveType)v.getType()));
                localVariablesStack.peek().put( s, t );
            } else {
                localVariablesStack.peek().put( s, v.getType() );
            }
            if ( v.getInitializer().isPresent() ) {
                if ( v.getInitializer().get() instanceof ConditionalExpr ) { // avoid conditional expression by rewriting
                    ConditionalExpr cc  = (ConditionalExpr)v.getInitializer().get();
                    b.append( indent ).append("if ").append(doConvert("",cc.getCondition())).append(":\n");
                    b.append(indent).append(s4).append( s );
                    b.append(" = ").append(doConvert("",cc.getThenExpr() )).append("\n");
                    b.append( indent ).append("else:\n" );
                    b.append(indent).append(s4).append( s );
                    b.append(" = ").append(doConvert("",cc.getElseExpr() )).append("\n");
                } else {
                    if ( v.getInitializer().get() instanceof ObjectCreationExpr && ((ObjectCreationExpr)v.getInitializer().get()).getAnonymousClassBody().isPresent() ) {
                        for ( BodyDeclaration bd: ((ObjectCreationExpr)v.getInitializer().get()).getAnonymousClassBody().get() ) {
                            b.append(doConvert( indent+"# J2J:", bd ) );
                        }
                    }
                    b.append( indent ).append(s).append(" = ").append(doConvert("",v.getInitializer().get()) );
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
        if ( ifStmt.getElseStmt().isPresent()) {
            Statement elseStmt = ifStmt.getElseStmt().get();
            if ( elseStmt instanceof IfStmt ) {
                b.append( specialConvertElifStmt( indent, (IfStmt)elseStmt ) );
            } else {
                b.append(indent).append("else:\n");
                b.append( doConvert(indent,elseStmt) );
            }
        }
        return b.toString();
    }

    private String doConvertIfStmt(String indent, IfStmt ifStmt) {
        StringBuilder b= new StringBuilder();
        //if ( ifStmt.getBeginLine()>1000 ) {
        //    System.err.println("here13331");
        //}
        if ( ifStmt.getCondition() instanceof MethodCallExpr &&
                ((MethodCallExpr)ifStmt.getCondition()).getName().asString().equals("isLoggable") ) {
            return indent + "# J2J: if "+ifStmt.getCondition() + " ... removed";
        }
        b.append(indent).append("if ");
        b.append( doConvert("", ifStmt.getCondition() ) );
        if ( ifStmt.getThenStmt() instanceof BlockStmt ) {
            b.append(":\n");
            b.append( doConvert(indent,ifStmt.getThenStmt() ) );
        } else {
            b.append(":\n");
            b.append( doConvert(indent+s4,ifStmt.getThenStmt() ) ).append("\n");
        }
        if ( ifStmt.getElseStmt().isPresent() ) {
            Statement elseStmt = ifStmt.getElseStmt().get();
            if ( elseStmt instanceof IfStmt ) {
                b.append( specialConvertElifStmt( indent, (IfStmt)elseStmt ) );
            } else {
                b.append(indent).append("else");
                if ( elseStmt instanceof BlockStmt ) {
                    b.append(":\n");
                    b.append( doConvert(indent,elseStmt) );
                } else {
                    b.append(": ");
                    b.append( doConvert("",elseStmt ) ).append("\n");
                }
            }
        }
        return b.toString();
    }

    private String doConvertForStmt(String indent, ForStmt forStmt) {
        StringBuilder b= new StringBuilder();
        localVariablesStack.push( new HashMap<>(localVariablesStack.peek()) );

        List<Expression> init = forStmt.getInitialization();
        VariableDeclarationExpr init1= null;
        String variableName = "";
        if ( init!=null && init.size()==1 && init.get(0) instanceof VariableDeclarationExpr ) {
            init1= (VariableDeclarationExpr)init.get(0);
            if (init1.getVariables().size()!=1 ) {
                init1= null;
            } else {
                variableName= (init1.getVariables().get(0)).getName().asString();
            }
        }
        BinaryExpr compare= (forStmt.getCompare().isPresent() && forStmt.getCompare().get() instanceof BinaryExpr) ? ((BinaryExpr)forStmt.getCompare().get()) : null;
        List<Expression> update= forStmt.getUpdate();
        UnaryExpr update1= null;
        if ( update.size()==1 && update.get(0) instanceof UnaryExpr ) {
            update1= (UnaryExpr)update.get(0);
            if ( !(update1.getOperator()==UnaryExpr.Operator.POSTFIX_INCREMENT
                    || update1.getOperator()==UnaryExpr.Operator.PREFIX_INCREMENT
                    || update1.getOperator()==UnaryExpr.Operator.POSTFIX_DECREMENT
                    || update1.getOperator()==UnaryExpr.Operator.PREFIX_DECREMENT  ) ) {
                update1= null;
            }
        }
        boolean initOkay= init1!=null;
        boolean compareOkay= compare!=null
                && ( compare.getOperator()==BinaryExpr.Operator.LESS|| compare.getOperator()==BinaryExpr.Operator.LESS_EQUALS
                || compare.getOperator()==BinaryExpr.Operator.GREATER || compare.getOperator()==BinaryExpr.Operator.GREATER_EQUALS );
        String compareTo= doConvert("",compare.getRight());
        if ( compare.getOperator()==BinaryExpr.Operator.LESS_EQUALS) {
            compareTo = compareTo + " + 1";
        } else if ( compare.getOperator()==BinaryExpr.Operator.GREATER_EQUALS ) {
            compareTo = compareTo + " - 1";
        }

        boolean updateOkay= update1!=null;

        boolean bodyOkay= false;
        if ( initOkay && compareOkay && updateOkay ) { // check that the loop variable isn't modified within loop
            bodyOkay= utilCheckNoVariableModification( forStmt.getBody(), variableName );
        }

        if ( initOkay && compareOkay && updateOkay && bodyOkay ) {
            b.append(indent).append( "for " ).append( variableName ).append(" in ");
            if ( pythonTarget==PythonTarget.jython_2_2 ) {
                b.append("xrange(");
            } else if ( pythonTarget==PythonTarget.python_3_6 ) {
                b.append("range(");
            } else {
                throw new IllegalArgumentException("not implemented");
            }
            b.append( doConvert("",init1.getVariables().get(0).getInitializer().get()) ).append(", ");
            b.append( compareTo );
            if (  update1.getOperator()==UnaryExpr.Operator.POSTFIX_DECREMENT
                    || update1.getOperator()==UnaryExpr.Operator.PREFIX_DECREMENT ) {
                b.append(", -1");
            }
            b.append("):\n");
        } else {
            if ( init!=null ) {
                init.forEach((e) -> {
                    b.append(indent).append( doConvert( "", e ) ).append( "\n" );
                });
            }
            b.append( indent ).append("while ").append(doConvert( "", forStmt.getCompare().get() )).append(":  # J2J for loop\n");
        }
        if ( forStmt.getBody() instanceof ExpressionStmt ) {
            b.append(indent).append(s4).append( doConvert( "", forStmt.getBody() ) ).append("\n");
        } else {
            b.append( doConvert( indent, forStmt.getBody() ) );
        }
        if ( !( initOkay && compareOkay && updateOkay && bodyOkay ) ) {
            forStmt.getUpdate().forEach((e) -> {
                b.append(indent).append(s4).append( doConvert( "", e ) ).append( "\n" );
            });
        }
        localVariablesStack.pop();
        return b.toString();
    }

    private String doConvertForEachStmt(String indent, ForEachStmt foreachStmt) {
        StringBuilder b= new StringBuilder();
        if ( foreachStmt.getVariable().getVariables().size()!=1 ) {
            throw new IllegalArgumentException("expected only one variable in foreach statement");
        }
        String variableName = foreachStmt.getVariableDeclarator().getName().asString();
        Type variableType= foreachStmt.getVariableDeclarator().getType();
        localVariablesStack.push( new HashMap<>(localVariablesStack.peek()) );
        localVariablesStack.peek().put( variableName,variableType );
        b.append( indent ).append("for ").append(variableName).append(" in ").append(doConvert("",foreachStmt.getIterable() )).append(":\n");
        if ( foreachStmt.getBody() instanceof ExpressionStmt ) {
            b.append(indent).append(s4).append( doConvert( "", foreachStmt.getBody() ) ).append("\n");
        } else {
            b.append( doConvert( indent, foreachStmt.getBody() ) );
        }
        localVariablesStack.pop( );
        return b.toString();
    }


    private String doConvertImportDeclaration( String indent, ImportDeclaration d ) {
        StringBuilder sb= new StringBuilder();
        Name n= d.getName();
        if (n.getQualifier().isPresent()) {
            Name qualifier = n.getQualifier().get();
            sb.append( "from " ).append(qualifier.asString()).append( " ");
        }
        sb.append("import ").append(n.getIdentifier());
        return sb.toString();
    }

    private String doConvertCompilationUnit(String indent, CompilationUnit compilationUnit) {

        pushScopeStack(false);

        StringBuilder sb= new StringBuilder();

        List<Node> nodes= compilationUnit.getChildNodes();
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
                // TODO: should we get the full import path or just the identifier (last segment)?
                String importName = ((ImportDeclaration)n).getName().asString();
                javaImports.put(importName, false );
                javaImportDeclarations.put(importName, (ImportDeclaration)n );
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

    private String doConvertReturnStmt(String indent, ReturnStmt returnStmt) {
        if ( returnStmt.getExpression().isPresent() ) {
            return indent + "return " + doConvert("", returnStmt.getExpression().get());
        } else {
            return indent + "return";
        }
    }

    private String doConvertArrayCreationExpr(String indent, ArrayCreationExpr arrayCreationExpr) {
        if ( arrayCreationExpr.getInitializer().isPresent() ) {
            ArrayInitializerExpr ap= arrayCreationExpr.getInitializer().get();
            return "[" + utilFormatExprList( ap.getValues() ) + "]";
        } else {
            String item;
            Type elementType = arrayCreationExpr.getElementType();
            if ( elementType.equals( new PrimitiveType(Primitive.BYTE) ) ||
                    elementType.equals( new PrimitiveType(Primitive.SHORT) ) ||
                    elementType.equals( new PrimitiveType(Primitive.INT) ) ||
                    elementType.equals( new PrimitiveType(Primitive.LONG) ) ) {
                item="0";
            } else {
                item= "None";
            }
            // TODO: what if levels is empty or first dimension is missing?
            Expression dimension = arrayCreationExpr.getLevels().get(0).getDimension().get();
            String dimensionString = doConvert( "", dimension );
            if ( dimension instanceof BinaryExpr ) {
                return "["+item+"] * (" + dimensionString + ")"; //TODO: might not be necessary
            } else {
                return "["+item+"] * " + dimensionString + ""; //TODO: might not be necessary
            }

        }
    }

    private String doConvertFieldAccessExpr(String indent, FieldAccessExpr fieldAccessExpr) {
        String s= doConvert( "", fieldAccessExpr.getScope() );

        if ( this.getCurrentScopeClasses().containsKey(s) ) {
            if ( !this.theClassName.equals(s) ) {
                s= javaNameToPythonName( this.theClassName ) + "." + s;
            }
        }

        // test to see if this is an array and "length" of the array is accessed.
        if ( fieldAccessExpr.getName().asString().equals("length") ) {
            String inContext= s;
            if ( inContext.startsWith("self.") ) {
                inContext= inContext.substring(5);
            }
            Type t= localVariablesStack.peek().get(inContext);
            if (t==null ) {
                t= getCurrentScope().get(inContext);
            }
            if ( t!=null && t instanceof ArrayType && ((ArrayType)t).getArrayLevel()>0 ) {
                return indent + "len("+ s + ")";
            }
        }
        if ( onlyStatic && s.equals(classNameStack.peek()) ) {
            return fieldAccessExpr.getName().asString();
        } else {
            if ( s.equals("Collections") ) {
                String f= fieldAccessExpr.getName().asString();
                switch (f) {
                    case "EMPTY_MAP":
                        return indent + "{}";
                    case "EMPTY_SET":
                        return indent + "{}"; // Jython 2.2 does not have sets.
                    case "EMPTY_LIST":
                        return indent + "[]";
                    default:
                        break;
                }
            } else if ( s.equals("Math") ) {
                String f= fieldAccessExpr.getName().asString();
                switch (f) {
                    case "PI":
                        additionalImports.put("import math\n", true);
                        return "math.pi"; // there is a math.tau!
                    case "E":
                        additionalImports.put("import math\n", true);
                        return "math.e";                
                }
            }
            return indent + s + "." + fieldAccessExpr.getName().asString();
        }
    }

    private String doConvertArrayAccessExpr(String indent, ArrayAccessExpr arrayAccessExpr) {
        return doConvert( indent,arrayAccessExpr.getName()) + "["+doConvert("",arrayAccessExpr.getIndex())+"]";
    }

    private String doConvertUnaryExpr(String indent, UnaryExpr unaryExpr) {
         switch (unaryExpr.getOperator()) {
            case PREFIX_INCREMENT: {
                additionalClasses.put("# J2J: increment used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpression());
                return indent + n + " = " + n + " + 1";
            }
            case PREFIX_DECREMENT: {
                additionalClasses.put("# J2J: decrement used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpression());
                return indent + n + " = " + n + " - 1";
            }
            case POSTFIX_INCREMENT: {
                additionalClasses.put("# J2J: increment used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpression());
                return indent + n + " = " + n + " + 1";
            }
            case POSTFIX_DECREMENT: {
                additionalClasses.put("# J2J: decrement used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpression());
                return indent + n + " = " + n + " - 1";
            }
            case PLUS: {
                String n= doConvert("",unaryExpr.getExpression());
                return indent + "+"+n;
            }
            case MINUS: {
                String n= doConvert("",unaryExpr.getExpression());
                return indent + "-"+n;
            }
            case LOGICAL_COMPLEMENT: {
                String n= doConvert("",unaryExpr.getExpression());
                return indent + "not "+n;
            }
            default:
                throw new IllegalArgumentException("not supported: "+unaryExpr);
        }
    }

    private String doConvertInitializerDeclaration(String indent, InitializerDeclaration initializerDeclaration) {
        return doConvert(indent,initializerDeclaration.getBody());
    }

    private String doConvertClassOrInterfaceDeclaration(String indent, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        StringBuilder sb= new StringBuilder();

        String name = classOrInterfaceDeclaration.getName().asString();
        if ( classNameStack.isEmpty() ) {
            theClassName= name;
        }
        classNameStack.push(name);

        getCurrentScopeClasses().put( name, classOrInterfaceDeclaration );

        pushScopeStack(false);
        getCurrentScope().put( "this", new ClassOrInterfaceType(name) );

        if ( onlyStatic ) {
            List<Node> methods= classOrInterfaceDeclaration.getChildNodes();
            //TODO: can we detect that two methods can be folded together?
            String[] ss= overloadedMethodCheck(methods);
            for ( int i=0; i<methods.size(); i++ ) {
                sb.append("\n");
                Node m1= methods.get(i);
                if ( ss[i].length()>0 ) {
                    sb.append(indent).append("# J2J: ").append(ss[i]).append("\n");
                }
                sb.append( doConvert(indent,m1) ).append("\n");
            };
        } else {

            if ( unittest && pythonTarget==PythonTarget.jython_2_2 ) {
                sb.append( "\n# cheesy unittest temporary\n");
                sb.append( "def assertEquals(a,b):\n"
                        + "    if ( not a==b ): raise Exception('a!=b')\n");
                sb.append( "def assertArrayEquals(a,b):\n");
                sb.append( "    if ( len(a)==len(b) ): \n");
                if ( pythonTarget==PythonTarget.python_3_6 ) {
                    sb.append( "        for i in range(len(a)): \n");
                } else {
                    sb.append( "        for i in xrange(len(a)): \n");
                }
                sb.append( "            if ( a[i]!=b[i] ): raise Exception('a[%d]!=b[%d]'%(i,i))\n" );
                sb.append( "def fail(msg):\n"
                        + "    print(msg)\n"
                        + "    raise Exception('fail: '+msg)\n");
                sb.append( "\n" );
            }
            String comments= utilRewriteComments(indent, classOrInterfaceDeclaration.getComment() );
            sb.append( comments );

            String pythonName;
            if ( camelToSnake ) {
                pythonName= camelToSnakeAndRegister(name);
            } else {
                pythonName= name;
            }

            if ( classOrInterfaceDeclaration.getExtendedTypes().size()==1 ) {
                String extendName= doConvert( "", classOrInterfaceDeclaration.getExtendedTypes().get(0) );
                sb.append( indent ).append("class " ).append( pythonName ).append("(" ).append(extendName).append(")").append(":\n");
            } else if ( !classOrInterfaceDeclaration.getImplementedTypes().isEmpty() ) {
                List<ClassOrInterfaceType> impls= classOrInterfaceDeclaration.getImplementedTypes();
                StringBuilder implementsName= new StringBuilder( doConvert( "", impls.get(0) ) );
                for ( int i=1; i<impls.size(); i++ ) {
                    implementsName.append(",").append( doConvert( "", impls.get(i) ) );
                }
                sb.append( indent ).append("class " ).append( pythonName ).append("(" ).append(implementsName).append(")").append(":\n");
            } else {
                if ( unittest && pythonTarget==PythonTarget.python_3_6 ) {
                    String extendName= "unittest.TestCase";
                    sb.append( indent ).append("class " ).append( pythonName ).append("(" ).append(extendName).append(")").append(":\n");
                } else {
                    sb.append( indent ).append("class " ).append( pythonName ).append(":\n");
                }
            }

            // check to see if any two methods can be combined.
            // https://github.com/jbfaden/JavaJythonConverter/issues/5
            classOrInterfaceDeclaration.getChildNodes().forEach((n) -> {
                if ( n instanceof MethodDeclaration ) {
                    classMethods.put( ((MethodDeclaration) n).getNameAsString(), classOrInterfaceDeclaration );
                    getCurrentScopeMethods().put(((MethodDeclaration) n).getNameAsString(),(MethodDeclaration)n );
                } else if ( n instanceof ClassOrInterfaceDeclaration ) {
                    ClassOrInterfaceDeclaration coid= (ClassOrInterfaceDeclaration)n;
                    getCurrentScope().put( coid.getNameAsString(), new ClassOrInterfaceType(coid.getNameAsString()) );
                }
            });

            // check for unique names
            Map<String,Node> nn= new HashMap<>();
            for ( Node n: classOrInterfaceDeclaration.getChildNodes() ) {
                if ( n instanceof ClassOrInterfaceType ) {
                    String name1= ((ClassOrInterfaceType)n).getName().asString();
                    if ( nn.containsKey(name1) ) {
                        sb.append(indent).append(s4).append("# J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                    }
                    nn.put( name1, n );
                } else if ( n instanceof FieldDeclaration ) {
                    for ( VariableDeclarator vd : ((FieldDeclaration)n).getVariables() ) {
                        String name1= vd.getName().asString();
                        if ( nn.containsKey(name1) ) {
                            sb.append(indent).append(s4).append("# J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
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
                    String name1= md.getName().asString();
                    if ( nn.containsKey(name1) ) {
                            sb.append(indent).append(s4).append("# J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                    }
                    nn.put( name1, n );
                } else {
                    System.err.println("Not supported: "+n);
                }
            }

            for ( Node n : classOrInterfaceDeclaration.getChildNodes() ) {
                if ( n instanceof ClassOrInterfaceType ) {
                    // skip this strange node
                } else if ( n instanceof ConstructorDeclaration ) {
                    if ( unittest && pythonTarget==PythonTarget.python_3_6 ) {
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
                    String s= doConvert( indent+s4, n );
                    if ( s.trim().length()>0 ) {
                        sb.append( s ).append("\n");
                    }
                }
            };

            if ( unittest && pythonTarget==PythonTarget.jython_2_2 ) {
                sb.append("test = ").append(classOrInterfaceDeclaration.getName()).append("()\n");
                for ( Node n : classOrInterfaceDeclaration.getChildNodes() ) {
                    if ( n instanceof MethodDeclaration
                            && ((MethodDeclaration)n).getNameAsString().startsWith("test")
                            && ((MethodDeclaration)n).getParameters()==null ) {
                        sb.append("test.").append(((MethodDeclaration) n).getName()).append("()\n");
                    }
                }
            }


        }

        popScopeStack();
        classNameStack.pop();

        return sb.toString();
    }

    private String doConvertMethodDeclaration(String indent, MethodDeclaration methodDeclaration) {
        boolean isStatic= ConversionUtils.isStaticMethod(methodDeclaration);

        if ( onlyStatic && !isStatic ) {
            return "";
        } else if ( isStatic ) {
            if ( methodDeclaration.getName().asString().equals("main") ) {
                hasMain= true;
            }
        }

        if ( methodDeclaration.getAnnotations()!=null ) {
            for ( AnnotationExpr a : methodDeclaration.getAnnotations() ) {
                if ( a.getNameAsString().equals("Deprecated") ) {
                    return "";
                }
            }
        }

        StringBuilder sb= new StringBuilder();
        String comments= utilRewriteComments( indent, methodDeclaration.getComment() );
        sb.append( comments );
        if ( pythonTarget==PythonTarget.python_3_6 && isStatic  && !onlyStatic ) {
            sb.append( indent ).append( "@staticmethod\n" );
        }

        String methodName= methodDeclaration.getName().asString();
        String pythonName;
        if ( camelToSnake ) {
            pythonName= camelToSnakeAndRegister(methodName);
        } else {
            pythonName= methodName;
        }
        sb.append( indent ).append( "def " ).append( pythonName ) .append("(");
        boolean comma;

        if ( !isStatic ) {
            sb.append("self");
            comma= true;
        } else {
            comma = false;
        }

        pushScopeStack(false);

        if ( methodDeclaration.getParameters()!=null ) {
            for ( Parameter p: methodDeclaration.getParameters() ) {
                String name= p.getName().asString();
                String pythonParameterName;
                if ( camelToSnake ) {
                    pythonParameterName= camelToSnakeAndRegister(name);
                } else {
                    pythonParameterName= name;
                }
                if ( comma ) {
                    sb.append(", ");
                } else {
                    comma = true;
                }
                sb.append( pythonParameterName );
                localVariablesStack.peek().put( name, p.getType() );
            }
        }
        sb.append( "):\n" );

        if ( methodDeclaration.getBody().isPresent() ) {
            sb.append( doConvert( indent, methodDeclaration.getBody().get() ) );
        } else {
            sb.append(indent).append(s4).append("pass");
        }
        popScopeStack();

        if ( pythonTarget==PythonTarget.jython_2_2 && isStatic && !onlyStatic ) {
            sb.append(indent).append(pythonName).append(" = staticmethod(").append(pythonName).append(")");
            sb.append(indent).append("\n");
        }
        return sb.toString();
    }

    private String doConvertFieldDeclaration(String indent, FieldDeclaration fieldDeclaration) {
        boolean s= ConversionUtils.isStaticField(fieldDeclaration); // TODO: static fields

        if ( onlyStatic && !s ) {
            return "";
        }

        StringBuilder sb= new StringBuilder();

        List<VariableDeclarator> vv= fieldDeclaration.getVariables();
        sb.append( utilRewriteComments( indent, fieldDeclaration.getComment() ) );
        if ( vv!=null ) {
            for ( VariableDeclarator v: vv ) {
                String name= v.getName().asString();
                String pythonName;
                if ( camelToSnake ) {
                    pythonName= camelToSnakeAndRegister(name);
                } else {
                    pythonName= name;
                }

                if ( v.getInitializer().isPresent() && v.getInitializer().get().toString().startsWith("Logger.getLogger") ) {
                    getCurrentScope().put( name, new ClassOrInterfaceType("Logger") );
                    //addLogger();
                    sb.append( indent ).append("# J2J: ").append(fieldDeclaration.toString());
                    continue;
                }

                getCurrentScope().put( name,ConversionUtils.getFieldType(fieldDeclaration) );
                getCurrentScopeFields().put( name,fieldDeclaration);

                if ( !v.getInitializer().isPresent() ) {
                    String implicitDeclaration = utilImplicitDeclaration( ConversionUtils.getFieldType(fieldDeclaration) );
                    if ( implicitDeclaration!=null ) {
                        sb.append( indent ).append( pythonName ).append(" = ").append( implicitDeclaration ).append("\n");
                    } else {
                        sb.append( indent ).append( pythonName ).append(" = ").append( "None  # J2J added" ).append("\n");
                    }
                } else if ( v.getInitializer().get() instanceof ConditionalExpr ) {
                    ConditionalExpr ce= (ConditionalExpr)v.getInitializer().get();
                    sb.append( indent ).append("if ").append(doConvert( "",ce.getCondition() )).append(":\n");
                    sb.append( indent ).append(s4).append(pythonName).append(" = ").append( doConvert( "",ce.getThenExpr() ) ).append("\n");
                    sb.append( indent ).append( "else:\n");
                    sb.append( indent ).append(s4).append(pythonName).append(" = ").append( doConvert( "",ce.getElseExpr() ) ).append("\n");

                } else {
                    sb.append( indent ).append(pythonName).append(" = ").append( doConvert( "",v.getInitializer().get() ) ).append("\n");

                }
            }
        }
        return sb.toString();
    }

    private String doConvertThrowStmt(String indent, ThrowStmt throwStmt) {
        return indent + "raise "+ doConvert("",throwStmt.getExpression());
    }

    private String doConvertWhileStmt(String indent, WhileStmt whileStmt) {
        StringBuilder sb= new StringBuilder(indent);
        sb.append( "while ");
        sb.append( doConvert( "", whileStmt.getCondition() ) );
        sb.append( ":\n" );
        if ( whileStmt.getBody() instanceof ExpressionStmt ) {
            sb.append( doConvert( indent+s4, whileStmt.getBody() ) );
        } else {
            sb.append( doConvert( indent, whileStmt.getBody() ) );
        }
        return sb.toString();
    }

    private String doConvertArrayInitializerExpr(String indent, ArrayInitializerExpr arrayInitializerExpr) {
        return indent + "[" + utilFormatExprList( arrayInitializerExpr.getValues() ) + "]";
    }


    private String doConvertSwitchStmt(String indent, SwitchStmt switchStmt) {
        String selector= doConvert( "",switchStmt.getSelector() );
        StringBuilder sb= new StringBuilder();
        boolean iff= true;
        int nses= switchStmt.getEntries().size();
        List<Expression> labels= new ArrayList<>();
        for ( int ises = 0; ises<nses; ises++ ) {
            SwitchEntry ses = switchStmt.getEntries().get(ises);
            List<Statement> statements= ses.getStatements();
            if ( statements.isEmpty() ) {
                // fall-through not supported
                //sb.append("# fall through not supported, need or in if test\n");
                labels.addAll(ses.getLabels());
                continue;
            }

            if ( iff ) {
                StringBuilder cb= new StringBuilder();
                for ( Expression l : labels ) {
                    if (cb.length() > 0) {
                        cb.append(" or ");
                    }
                    cb.append(selector).append(" == ").append(doConvert("",l));
                }
                for ( Expression otherLabel : ses.getLabels() ) {
                    if (cb.length() > 0) {
                        cb.append(" or ");
                    }
                    cb.append(selector).append(" == ").append(doConvert("", otherLabel));
                }
                sb.append(indent).append("if ").append(cb.toString()).append(":\n");
                iff=false;
            } else {
                StringBuilder cb= new StringBuilder();
                for ( Expression l : labels ) {
                    if (cb.length() > 0) {
                        cb.append(" or ");
                    }
                    cb.append(selector).append(" == ").append(doConvert("",l));
                }
                if ( !ses.getLabels().isEmpty() ) {
                    for ( Expression otherLabel : ses.getLabels() ) {
                        if (cb.length() > 0) {
                            cb.append(" or ");
                        }
                        cb.append(selector).append(" == ").append(doConvert("",otherLabel));
                    }
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

            if ( ses.getLabels().isEmpty() && ises!=(nses-1) ) {
                throw new IllegalArgumentException("default must be last of switch statement");
            }
            if ( !ses.getLabels().isEmpty() && !( ( statements.get(statements.size()-1) instanceof BreakStmt ) ||
                    ( statements.get(statements.size()-1) instanceof ReturnStmt ) ||
                    ( statements.get(statements.size()-1) instanceof ThrowStmt ) ) ) {
                sb.append(indent).append(s4).append("### Switch Fall Through Not Implemented ###\n");
                for ( Statement s: statements ) {
                    sb.append("#").append(doConvert(s4+indent, s )).append("\n");
                }
            } else {
                if ( statements.get(statements.size()-1) instanceof BreakStmt ) {
                    if ( statements.size()==1 ) {
                        sb.append(indent).append(s4).append("pass\n");
                    }
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

    private static String utilRewriteComments(String indent, Optional<Comment> commentsOptional) {
        if ( commentsOptional==null || !commentsOptional.isPresent() ) return "";
        Comment comments = commentsOptional.get();
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
            if ( objectCreationExpr.getArguments()!=null ) {
                if ( objectCreationExpr.getArguments().size()==1 ) {
                    Expression e= objectCreationExpr.getArguments().get(0);
                    if ( new PrimitiveType(Primitive.INT).equals(guessType(e)) ) { // new StringBuilder(100);
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
            if ( objectCreationExpr.getArguments()==null ) {
                return indent + "Exception()";
            } else {
                return indent + "Exception("+ doConvert("",objectCreationExpr.getArguments().get(0))+")";
            }
        } else {
            String qualifiedName= utilQualifyClassName(objectCreationExpr.getType());
            if ( qualifiedName!=null ) {
                return indent + qualifiedName + "("+ utilFormatExprList(objectCreationExpr.getArguments())+ ")";
            } else {
                if ( objectCreationExpr.getAnonymousClassBody().isPresent() ) {
                    StringBuilder sb= new StringBuilder();
                    String body= doConvert( indent, objectCreationExpr.getAnonymousClassBody().get().get(0) );
                    sb.append(indent).append(objectCreationExpr.getType()).append("(").append(utilFormatExprList(objectCreationExpr.getArguments())).append(")");
                    sb.append("*** # J2J: This is extended in an anonymous inner class ***");
                    return sb.toString();
                } else {
                    if ( objectCreationExpr.getType().getName().asString().equals("HashMap") ) {
                        return indent + "{}";
                    } else if ( objectCreationExpr.getType().getName().asString().equals("ArrayList") ) {
                        return indent + "[]";
                    } else if ( objectCreationExpr.getType().getName().asString().equals("HashSet") ) {
                        return indent + "{}"; // to support Jython 2.2, use dictionary for now
                    } else {
                        String typeName = objectCreationExpr.getType().getName().asString();
                        if ( javaImports.keySet().contains( typeName ) ) {
                            javaImports.put( typeName, true );
                        }
                        if ( typeName.equals("String") ) {
                            if ( objectCreationExpr.getArguments().size()==1 ) {
                                Expression e= objectCreationExpr.getArguments().get(0);
                                Type t= guessType(e);
                                if ( t instanceof ReferenceType
                                        && t.equals(new ArrayType(new PrimitiveType(Primitive.CHAR)) ) ) {
                                    return indent + "''.join( "+ doConvert("",e) +")";
                                } else if ( t.equals(new ClassOrInterfaceType("StringBuilder") ) ) {
                                    return  doConvert("",e); // these are just strings.
                                }
                                System.err.println("here "+t);
                            }
                        }
                        return indent + objectCreationExpr.getType() + "("+ utilFormatExprList(objectCreationExpr.getArguments())+ ")";
                    }
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
        for ( Parameter p: constructorDeclaration.getParameters() ) {
                String name= p.getName().asString();
                localVariablesStack.peek().put( name, p.getType() );
        }
        sb.append( doConvert(indent,constructorDeclaration.getBody()) );
        return sb.toString();
    }

    private String doConvertConditionalExpr(String indent, ConditionalExpr conditionalExpr) {
        if ( pythonTarget==PythonTarget.jython_2_2 ) {
            String ce= "def cej2j( condition, a, b ):\n" +
"    if condition:\n" +
"        return a\n" +
"    else:\n" +
"        return b\n";
            additionalClasses.put( ce, true );
            StringBuilder sb= new StringBuilder();
            sb.append(indent).append("cej2j(")
                    .append( doConvert("",conditionalExpr.getCondition())).append(", ")
                    .append( doConvert("",conditionalExpr.getThenExpr())).append(", ")
                    .append( doConvert("",conditionalExpr.getElseExpr())).append( ")" );
            return sb.toString();
        } else {
            return indent + doConvert("",conditionalExpr.getThenExpr())
                    + " if " + doConvert("",conditionalExpr.getCondition())
                    + " else " +  doConvert("",conditionalExpr.getElseExpr());
        }
    }

    private String doConvertCastExpr(String indent, CastExpr castExpr) {
        String type= castExpr.getType().toString();
        switch (type) {
            case "String":
                type= "str";
                break;
            case "char":
                if ( ConversionUtils.isIntegerType(guessType(castExpr.getExpression())) ) {
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
        return type + "(" + doConvert("", castExpr.getExpression() ) + ")";
    }

    private String doConvertTryStmt(String indent, TryStmt tryStmt) {
        StringBuilder sb= new StringBuilder();
        sb.append( indent ).append( "try:\n");
        sb.append( doConvert( indent, tryStmt.getTryBlock() ) );
        for ( CatchClause cc: tryStmt.getCatchClauses() ) {
            String id= cc.getParameter().getNameAsString();
            if ( pythonTarget==PythonTarget.python_3_6 ) {
                sb.append(indent).append("except Exception as ").append( id ).append(":  # J2J: exceptions\n");
            } else {
                // TODO: do we want getParameter or just the type name for the parameter?
                sb.append(indent).append("except ").append(doConvert( "",cc.getParameter() )).append( ", ").append(id).append(":\n");
            }
            sb.append( doConvert( indent, cc.getBody() ) );
        }
        if ( tryStmt.getFinallyBlock().isPresent() ) {
            sb.append( indent ).append( "finally:\n");
            sb.append( doConvert( indent, tryStmt.getFinallyBlock().get() ) );
        }
        return sb.toString();
    }

    private String doConvertReferenceType(String indent, ReferenceType referenceType) {
        // TODO: what should we return for non-ArrayTypes here?
        if (!(referenceType instanceof ArrayType)) {
            return "***J2J" + referenceType.toString() +"***J2J";
        }
        ArrayType arrayType = (ArrayType) referenceType;
        String typeName = referenceType.getElementType().toString();
        switch (arrayType.getArrayLevel()) {
            case 0:
                return typeName;
            case 1:
                return typeName+"[]";
            case 2:
                return typeName+"[][]";
            case 3:
                return typeName+"[][][]";
            default:
                return "***J2J" + referenceType.toString() +"***J2J";
        }

    }

    public static void main(String[] args ) throws ParseException, FileNotFoundException {
        ConvertJavaToPython c= new ConvertJavaToPython();
        c.setOnlyStatic(false);
        c.setUnittest(false);
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

        InputStream ins= new FileInputStream("/net/spot8/home/jbf/ct/hapi/git/uri-templates/UriTemplatesJava/src/org/hapiserver/URITemplate.java");
        String text = new BufferedReader(
            new InputStreamReader(ins, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

        System.err.println( c.doConvert(text) );
    }

    /**
     * return the converted name, if it's been converted, otherwise return the name.
     * @param str
     * @return
     */
    private String javaNameToPythonName( String str ) {
        String cov= this.nameMapForward.get(str);
        if ( cov==null ) {
            return str;
        } else {
            return cov;
        }
    }

    /**
     * convert the name and register it in the nameMap.
     * @param str
     * @return
     */
    private String camelToSnakeAndRegister(String str) {

        String snake = camelToSnake(str);
        if ( nameMapForward.containsKey(str) ) {
            System.err.println("name map results in collision: "+str);
        }
        if ( nameMapReverse.containsKey(snake) ) {
            System.err.println("name map results in collision: "+snake);
        }
        nameMapForward.put( str, snake );
        nameMapReverse.put( snake, str );

        return snake;
    }

   /**
    * Function to convert camel case
    * string to snake case string
    * from https://www.geeksforgeeks.org/convert-camel-case-string-to-snake-case-in-java/ with modifications
    */
    public static String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        char c = str.charAt(0);
        result.append( Character.toLowerCase(c) );
        boolean lastLower= Character.isLowerCase(c);
        boolean inUpperCaseWord = false;
        for (int i = 1; i < str.length(); i++) {

            c = str.charAt(i);

            if (Character.isUpperCase(c)) {
                if ( lastLower ) {
                    result.append('_');
                } else {
                    inUpperCaseWord=true;
                }
                c = Character.toLowerCase(c);
            } else if ( Character.isLowerCase(c) ) {
                if ( inUpperCaseWord ) { // "URITemplate" -> uri_template
                    result.insert( i-1, '_' );
                }
                inUpperCaseWord= false;
            }
            result.append(c);
        }
        String snake= result.toString();
        return snake;
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

    /**
     * turn a quoted replacement string into an raw string.  This
     * is beyond my mental capacity because it's code not interpretted
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

            getCurrentScopeClasses().put( enumDeclaration.getNameAsString(), enumDeclaration );

            builder.append(indent).append("class ").append(enumDeclaration.getName()).append(":\n");
            List<EnumConstantDeclaration> ll = enumDeclaration.getEntries();

            for ( Node n: enumDeclaration.getChildNodes() ) {
                if ( n instanceof ConstructorDeclaration ) {
                    String params= utilFormatParameterList( ((ConstructorDeclaration)n).getParameters() );
                    builder.append(indent).append(s4 + "def compare( self, o1, o2 ):\n");
                    builder.append(indent).append(s4 + "    raise Exception('Implement me')\n");
                }
            }

            for ( EnumConstantDeclaration l : ll ) {
                String args = utilFormatExprList(l.getArguments());
                args= "";// we drop the args
                //TODO find anonymous extension  l.getArguments().get(0).getChildNodes()
                builder.append(indent).append(enumDeclaration.getName()).append(".").append(l.getName()).append(" = ")
                        .append(enumDeclaration.getName()).append("(").append(args).append(")") .append("\n");
                String methodName=null;
                if ( l.getArguments().get(0).getChildNodes()!=null ) {
                    for ( Node n: l.getArguments().get(0).getChildNodes() ) {
                        if ( n instanceof MethodDeclaration ) {
                            methodName= ((MethodDeclaration)n).getNameAsString();
                            builder.append( doConvert( indent, n ) );
                        }
                    }
                }
                if (methodName!=null) {
                    builder.append(indent).append(enumDeclaration.getName()).append(".").append(l.getName()).append(".")
                            .append(methodName).append('=').append(methodName).append("\n");
                }

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

    private String doConvertEmptyStmt(String indent, EmptyStmt emptyStmt) {
        return indent + "pass";
    }

    private void addLogger() {
//        additionalClasses.add( "class Logger:\n" +
//                    "    def getLogger(name):\n" +
//                    "        return Logger()\n" +
//                    "    def info( self, mesg, e=None ):\n" +
//                    "        print mesg\n" +
//                    "    def fine( self, mesg, e=None ):\n" +
//                    "        print mesg\n" +
//                    "    def log( self, level, mesg, e=None ):\n" +
//                    "        print mesg\n" +
//                "");
    }

    private String doConvertNameExpr(String indent, NameExpr nameExpr) {
        String s= nameExpr.getName().asString();
        String scope;
        if ( localVariablesStack.peek().containsKey(s) ) {
            scope = ""; // local variable
        } else if ( getCurrentScopeFields().containsKey(s) ) {
            FieldDeclaration ss= getCurrentScopeFields().get(s);
            boolean isStatic= ConversionUtils.isStaticField(ss);
            if ( isStatic ) {
                scope = javaNameToPythonName( theClassName );
            } else {
                scope = "self";
            }
        } else {
            scope = ""; // local variable //TODO: review this
        }
        return indent + scope + (scope.length()==0 ? "" : ".") + javaNameToPythonName(s);

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
        return exprType.equals(STRING_TYPE) || exprType.equals(new PrimitiveType(Primitive.CHAR));
    }

    private boolean utilCheckNoVariableModification(BlockStmt body, String name ) {
        for ( Statement s: body.getStatements() ) {
            if ( !utilCheckNoVariableModification( s, name ) ) {
                return false;
            }
        }
        return true;
    }

    private boolean utilCheckNoVariableModification(Statement body, String name ) {
        for ( Node n: body.getChildNodes() ) {
            if ( n instanceof BlockStmt ) {
                return utilCheckNoVariableModification((BlockStmt)n, name );
            } else if ( n instanceof ExpressionStmt ) {
                if ( ((ExpressionStmt) n).getExpression() instanceof AssignExpr ) {
                    AssignExpr ae= (AssignExpr)((ExpressionStmt) n).getExpression();
                    Expression t= ae.getTarget();
                    if ( t instanceof NameExpr ) {
                        if ( ((NameExpr)t).getName().equals(name) ) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * look through the methods to see if one two are overloaded methods which could
     * be folded into one python method using default keywords.
     * @param methods
     * @return
     */
    private String[] overloadedMethodCheck(List<Node> methods) {
        String[] result= new String[methods.size()];
        for ( int i=0; i<methods.size(); i++ ) {
            result[i]="";
            Node n1= methods.get(i);
            if ( !( n1 instanceof MethodDeclaration ) ) {
                // nothing to be done
            } else {
                MethodDeclaration m1= (MethodDeclaration)n1;
                for ( int j=0; j<methods.size(); j++ ) {
                    if ( i==j ) continue;
                    Node n2= methods.get(j);
                    if ( n2 instanceof MethodDeclaration ) {
                        MethodDeclaration m2= (MethodDeclaration)n2;
                        if ( m2.getName().equals(m1.getName()) ) {
                            if ( m2.getParameters().size()==m1.getParameters().size()+1 ) {
                                boolean okay=true;
                                for ( int k=0; k<m1.getParameters().size(); k++ ) {
                                    Parameter parm1= m1.getParameters().get(k);
                                    Parameter parm2= m2.getParameters().get(k);

                                    if ( parm1.getName().equals( parm2.getName())  &&
                                        parm1.getType().equals(parm2.getType() ) ) {

                                    } else {
                                        okay=false;
                                    }
                                }
                                Parameter extraParameter= m2.getParameters().get(m2.getParameters().size()-1);
                                // is there one call to the other method which adds an argument?
                                if ( m1.getBody().get().getStatements().size()==1 ) {
                                    Statement s= m1.getBody().get().getStatements().get(0);
                                    if ( s instanceof ExpressionStmt ) {
                                        ExpressionStmt es1= (ExpressionStmt)s;
                                        Expression e= es1.getExpression();
                                        if ( e instanceof MethodCallExpr ) {
                                            MethodCallExpr mce= (MethodCallExpr)e;
                                            if ( mce.getName().equals(m2.getName()) ) {
                                                List<Node> arguments= es1.getExpression().getChildNodes();
                                                if ( arguments.size()==m2.getParameters().size() ) {
                                                    Node n= arguments.get(arguments.size()-1);
                                                    String name= extraParameter.getNameAsString();
                                                    result[i]= "can be combined with " + name + "="+ n;
                                                }
                                            }
                                        }

                                    }
                                }
                                if ( !okay ) {
                                    result[i]="";
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

}
