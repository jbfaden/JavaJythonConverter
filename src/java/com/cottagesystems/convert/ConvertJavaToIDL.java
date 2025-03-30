
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
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
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

// Used by doConvertImportDeclaration, which is a stub method now
// waiting for full implementation.
// import com.github.javaparser.ast.expr.QualifiedNameExpr;


import com.github.javaparser.ast.expr.UnaryExpr;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.ArrayType;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Class for converting Java to IDL using an AST.  The web page
 * https://hesperia.gsfc.nasa.gov/rhessidatacenter/complementary_data/objects/objects.html
 * has been useful to remind me how objects in IDL work.
 *
 * @author jbf
 */
public class ConvertJavaToIDL {

    public static final String VERSION = "20240217b";

    private JavaParser javaParser = new JavaParser();

    public ConvertJavaToIDL() {
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

    /**
     * the indent level.
     */
    private static final String s4="    ";

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
    private static final ReferenceType STRING_TYPE = new ClassOrInterfaceType("String");

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

    private String commonForStaticVariables= "";

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

    /**
     * the name in snake case, if we are using this mode
     */
    private String the_class_name;

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
        return utilFormatExprList("",l);
    }

    private String utilFormatExprList( String prefix, List<Expression> l ) {
        if ( l==null ) return "";
        if ( l.isEmpty() ) return "";
        if ( prefix==null ) prefix="";
        StringBuilder b= new StringBuilder( prefix );
        b.append( doConvert("",l.get(0)) );
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
     * see if the two values are just integer constants which can be differenced.
     * @param l2 an expression, e.g. "9"
     * @param l1 an expression, e.g. "4"
     * @return "" if we can't, the number otherwise, with "9" and "4" this would return "5".
     *
     */
    private String maybeGetLength( Expression l2, Expression l1 ) {
        if ( l1 instanceof IntegerLiteralExpr ) {
            IntegerLiteralExpr ex1= (IntegerLiteralExpr)l1;
            if ( l2 instanceof IntegerLiteralExpr ) {
                IntegerLiteralExpr ex2= (IntegerLiteralExpr)l2;
                return String.valueOf( Integer.parseInt(ex2.getValue())-Integer.parseInt(ex1.getValue()) );
            } else if ( Integer.parseInt(ex1.getValue())==0 ) {
                return doConvert("",l2);
            }
        }
        return "";
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
     */
    public String utilMakeClass( String javasrc ) {
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

                if ( hasMain ) {
                    if ( !isOnlyStatic() ) {
                        sb.append( javaNameToIdlName(theClassName) ).append(".main([])\n");
                    } else {
                        sb.append("main([])\n");
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
                int endLine = ConversionUtils.getEndLine(parseExpression);
                while ( (linesHandled+endLine)<lines.length ) {
                    int additionalLinesHandled=0;
                    for ( int i=0; i<endLine; i++ ) {
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
                    endLine = ConversionUtils.getEndLine(parseExpression);
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
            src= src.trim();

            if ( src.length()>0 && !src.startsWith("function ") && !src.startsWith("pro ") ) {
                src= utilUnMakeClass(src);
            }

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
                if ( null!=b.getOperator() ) switch (b.getOperator()) {
                    case GREATER:
                        return doConvert("",mce.getScope().get()) + " gt " + doConvert("",mce.getArguments().get(0));
                    case GREATER_EQUALS:
                        return doConvert("",mce.getScope().get()) + " ge " + doConvert("",mce.getArguments().get(0));
                    case LESS:
                        return doConvert("",mce.getScope().get()) + " lt " + doConvert("",mce.getArguments().get(0));
                    case LESS_EQUALS:
                        return doConvert("",mce.getScope().get()) + " le " + doConvert("",mce.getArguments().get(0));
                    case EQUALS:
                        return doConvert("",mce.getScope().get()) + " eq " + doConvert("",mce.getArguments().get(0));
                    case NOT_EQUALS:
                        return doConvert("",mce.getScope().get()) + " ne " + doConvert("",mce.getArguments().get(0));
                    default:
                        break;
                }
            }
        }
        BinaryExpr.Operator op= b.getOperator();
        if (  rightType!=null && rightType.equals(Primitive.CHAR) && left.startsWith("string(byte(") && left.endsWith("))") ) {
            left= left.substring(4,left.length()-1);
        }

        if ( leftType!=null && rightType!=null ) {
            if ( leftType.equals(Primitive.CHAR) && rightType.equals(Primitive.INT) ) {
                left= "long(byte("+left+"))";
            }
            if ( leftType.equals(Primitive.INT) && rightType.equals(Primitive.CHAR) ) {
                right= "long(byte("+right+"))"; // TODO: check Python for this bug
            }
            if ( rightType.equals(STRING_TYPE)
                    && leftType instanceof PrimitiveType
                    && !leftType.equals(Primitive.CHAR) ) {
                left= "strtrim("+left+",2)";
            }
            if ( leftType.equals(STRING_TYPE)
                    && rightType instanceof PrimitiveType
                    && !rightType.equals(Primitive.CHAR) ) {
                right= "strtrim("+right+",2)";
            }
        }

        if ( leftType!=null && !( b.getRight() instanceof NullLiteralExpr )
                && leftType.equals(STRING_TYPE) && rightType==null ) {
            right= "strtrim("+right+",2)";
        }

        switch (op) {
            case PLUS:
                return left + " + " + right;
            case MINUS:
                return left + " - " + right;
            case DIVIDE:
                    return left + " / " + right;
            case MULTIPLY:
                return left + " * " + right;
            case GREATER:
                return left + " gt " + right;
            case LESS:
                return left + " lt " + right;
            case GREATER_EQUALS:
                return left + " ge " + right;
            case LESS_EQUALS:
                return left + " le " + right;
            case AND:
                return left + " and " + right;
            case OR:
                return left + " or " + right;
            case EQUALS:
                if ( b.getRight() instanceof NullLiteralExpr ) {
                    return "n_elements(" + left + ") eq 0";
                } else {
                    return left + " eq " + right;
                }
            case NOT_EQUALS:
                return left + " ne " + right;
            case REMAINDER:
                return left + " mod " + right;
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
        if ( clas instanceof NameExpr ) {
            String clasName= ((NameExpr)clas).getName().toString();
            if ( Character.isUpperCase(clasName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                return new ClassOrInterfaceType( clasName.toString());
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
                if ( ( leftType!=null && leftType.equals(STRING_TYPE) ) ||
                        ( rightType!=null && rightType.equals(STRING_TYPE) ) ) {
                    return STRING_TYPE;
                }
            }

            return ConversionUtils.findBinaryExprType( leftType, rightType );

        } else if ( clas instanceof FieldAccessExpr ) {
            String fieldName= ((FieldAccessExpr)clas).getName().toString();
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
                        return new ClassOrInterfaceType( "Matcher");
                    }
                } else if ( scopeType.toString().equals("StringBuilder") ) {
                    if (mce.getName().toString().equals("toString")) {
                            return new ClassOrInterfaceType( "String");
                    }
                } else if ( scopeType.toString().equals("String") ) {
                    switch ( mce.getName().toString() ) {
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
                    if ( mce.getName().toString().equals("copyOfRange") ) {
                            return guessType( mce.getArguments().get(0) );
                    }
                }
            }
            switch ( mce.getName().toString() ) { // TODO: consider t
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
     * IDL classes sometimes need parenthesis around them, if it is an
     * expression and not a name which is called.
     * @param clas the object
     * @param call the call
     * @return the expression, possibly with needed parenthesis added.
     */
    private String safeMethodCall( Expression clas, String call ) {
        if ( clas instanceof NameExpr ) {
            return doConvert("",clas) + "." + call;
        } else {
            return "(" + doConvert("",clas) + ")." + call;
        }
    }

    /**
     * IDL classes sometimes need parenthesis around them, if it is an
     * expression and not a name which is called.
     * @param target the string for the object
     * @param call the call
     * @return the expression, possibly with needed parenthesis added.
     */
    private String safeMethodCall( String target, String call ) {
        if ( !(target.contains(".") || target.contains("(") ) ) {
            return target + "." + call;
        } else {
            return "(" + target + ")." + call;
        }
    }

    /**
     * This is somewhat the "heart" of this converter, where Java library use is translated to IDL use.
     * @param indent
     * @param methodCallExpr
     * @return
     */
    private String doConvertMethodCallExpr(String indent,MethodCallExpr methodCallExpr) {
        // The following will raise an exception if the scope is missing.
        // TODO: what should we do if the scope is missing.
        Expression clas= methodCallExpr.getScope().get();
        String name= methodCallExpr.getName().toString();
        List<Expression> args= methodCallExpr.getArguments();

        if ( name==null ) {
            name=""; // I don't think this happens
        }

        /**
         * try to identify the class of the scope, which could be either a static or non-static method.
         */
        String clasType="";
        System.err.println( "doConvertMethodCallExpr "+clas+ " " +name);
        if ( name.equals("fromWeekOfYear") ) {
            System.err.println("here stop");
        }
        if ( clas instanceof NameExpr ) {
            String contextName= ((NameExpr)clas).getName().toString(); // sb in sb.append, or String in String.format.
            Type contextType= localVariablesStack.peek().get(contextName); // allow local variables to override class variables.
            if ( contextType==null ) contextType= getCurrentScope().get(contextName);
            if ( Character.isUpperCase(contextName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                clasType= contextName;
            } else if ( stringMethods.contains(name)
                    && ( contextType==null || contextType.equals(STRING_TYPE)) ) {
                clasType= "String";
            } else if ( characterMethods.contains(name)
                    && ( contextType==null || contextType.equals(STRING_TYPE)) ) {
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
                if ( STRING_TYPE.equals(guessType( clas )) ) {
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

        if ( clasType.equals("StringBuilder") ) { //TODO: https://github.com/jbfaden/JavaJythonConverter/issues/27
            if ( name.equals("append") ) {
                return indent + doConvert("",clas) + " = " + doConvert("",clas) + " + " + utilAssertStr(args.get(0)) ;
            } else if ( name.equals("toString") ) {
                return indent + doConvert("",clas);
            } else if ( name.equals("insert") ) {
                String n= doConvert("",clas);
                String i0= doConvert("",args.get(0));
                String ins= doConvert("",args.get(1));
                return indent + n + " = strmid( "+n+",0," + i0 + ") + " + ins + " + strmid( "+n+","+i0+") ; J2J expr -> assignment"; // expr becomes assignment, this will cause problems
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
                    return indent + "[ "+doConvert("",args.get(0)) + " ]";
                default:
                    break;
            }

        }
        if ( clasType.equals("Math") ) {
            switch (name) {
                case "pow":
                    if ( args.get(1) instanceof IntegerLiteralExpr ) {
                        return doConvert(indent,args.get(0)) + "^"+ doConvert(indent,args.get(1));
                    } else {
                        return doConvert(indent,args.get(0)) + "^("+ doConvert(indent,args.get(1))+")";
                    }
                case "max":
                    return "(" + doConvert(indent,args.get(0)) + ">"+ doConvert(indent,args.get(1))+")";
                case "min":
                    return "(" + doConvert(indent,args.get(0)) + "<"+ doConvert(indent,args.get(1))+")";
                case "floorDiv":
                    this.additionalClasses.put("function floorDiv, m, n\n"+
                            "  return, floor(m/double(n))\n"+
                            "end\n",Boolean.TRUE);
                    return "floorDiv(" + doConvert(indent,args.get(0)) + ", " + doConvert(indent,args.get(1))+")";
                case "floorMod":
                    this.additionalClasses.put("function floorMod, m, n\n"+
                            "  return, m-n*floor(m/double(n))\n"+
                            "end\n",Boolean.TRUE);
                    return "floorMod(" + doConvert(indent,args.get(0)) + ", " + doConvert(indent,args.get(1))+")";
                case "floor":
                case "ceil":
                case "round":
                    return name + "(" + doConvert(indent,args.get(0)) + ")";
                case "atan2":
                    return "atan" + "(" + doConvert(indent,args.get(0)) + ","+doConvert(indent,args.get(1)) + ")";
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
                    return indent + doConvert("",clas) + ".HasKey(" + doConvert("",args.get(0)) + ")" ;
                case "remove":
                    return indent + doConvert("",clas) + ".Remove," + doConvert("",args.get(0));
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
                    return indent + doConvert("",clas) + ".HasKey(" + doConvert("",args.get(0)) + ")" ;
                case "add":
                    return indent + doConvert("",clas) + "["+doConvert("",args.get(0))+"] = "+doConvert("",args.get(0));
                case "remove":
                    return indent + doConvert("",clas) + ".Remove(" + doConvert("",args.get(0)) + ")";
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
                    return indent + "n_elements(" + doConvert("",clas) + ")";
                case "add":
                    if ( args.size()==2 ) {
                        return indent + doConvert("",clas) + ".Add, "+doConvert("",args.get(0))+","+doConvert("",args.get(1));
                    } else {
                        return indent + doConvert("",clas) + ".Add, "+doConvert("",args.get(0))+"";
                    }
                case "remove":
                    if ( guessType(args.get(0)).equals(Primitive.INT) ) {
                        return indent + doConvert("",clas) + ".Remove," + doConvert("",args.get(0));
                    } else {
                        additionalClasses.put("function indexOf, l, e\n  r= l.Where(e,count=c)\n  if c eq 0 then return, -1 else return, r[0]\n",true);
                        return indent + doConvert("",clas) + ".Remove, indexOf(" + doConvert("",clas)+","+ doConvert("",args.get(0)) + ")";
                    }
                case "get":
                    return indent + doConvert("",clas) + "["+doConvert("",args.get(0))+"]";
                case "contains":
                    return indent + "(" + doConvert("",clas) + ".Where("+ doConvert("",args.get(0))+") ne !NULL )";
                case "indexOf":
                    additionalClasses.put("function indexOf, l, e\n  r= l.Where(e,count=c)\n  if c eq 0 then return, -1 else return, r[0]\n",true);
                    return indent + "indexOf(" + doConvert("",clas) + "," + doConvert("",args.get(0)) + ")";
                case "toArray":
                    return indent + doConvert("",clas); // It's already an array (a list really).
                default:
                    break;
            }
        }

        if ( clasType.equals("Logger") ) {
            return indent + "; J2J (logger) "+methodCallExpr.toString();
        }
        if ( clasType.equals("String") ) {
            switch (name) {
                case "format":
                    if ( clas.toString().equals("String") ) {
                        StringBuilder sb= new StringBuilder();
                        sb.append(indent).append("string(format=").append(doConvert("",args.get(0)));
                        sb.append( utilFormatExprList( ",", args.subList(1, args.size() ) ) );
                        sb.append(")");
                        return sb.toString();
                    } else {
                        break;
                    }
                case "substring":
                    if ( args.size()==1 ) {
                        return "strmid("+doConvert(indent,clas)+","+ doConvert("",args.get(0))+")";
                    } else if ( args.size()==2 ) {
                        String slen= maybeGetLength( args.get(1), args.get(0) );
                        if ( slen.length()>0 ) {
                            return "strmid("+doConvert(indent,clas)+","+ doConvert("",args.get(0)) +","+ slen + ")";
                        } else {
                            String diff= doConvert("",args.get(1)) + "-" + doConvert("",args.get(0));
                            return "strmid("+doConvert(indent,clas)+","+ doConvert("",args.get(0)) +","+ diff +")";
                        }
                    }
                case "length":
                    return "strlen("+doConvert(indent,clas)+")";
                case "indexOf":
                    if ( args.size()==1 ) {
                        return "strpos(" + doConvert(indent,clas)+","+ doConvert("",args.get(0)) + ")";
                    } else {
                        return "strpos(" + doConvert(indent,clas)+","+ doConvert("",args.get(0)) + "," + doConvert("",args.get(1))+")";
                    }
                case "lastIndexOf":
                    if ( args.size()==1 ) {
                        return "strpos(" + doConvert(indent,clas)+","+ doConvert("",args.get(0)) + ",/REVERSE_SEARCH)";
                    } else {
                        return "strpos(" + doConvert(indent,clas)+","+ doConvert("",args.get(0)) + "," + doConvert("",args.get(1))+",/REVERSE_SEARCH)";
                    }
                case "contains":
                    return "(strpos("+ doConvert(indent,clas)+","+ doConvert("",args.get(0)) + ") ne -1)";
                case "toUpperCase":
                    return "strupcase( "+doConvert(indent,clas) + ")";
                case "toLowerCase":
                    return "strlowcase( "+doConvert(indent,clas) + " )";
                case "charAt":
                    return "strmid("+doConvert(indent,clas)+","+ doConvert("",args.get(0)) +",1)";
                case "startsWith":
                    return "("+doConvert(indent,clas)+")"+".startswith("+ utilFormatExprList("",args) +")";
                case "endsWith":
                    return doConvert(indent,clas)+".endswith("+ utilFormatExprList("",args) +")";
                case "equalsIgnoreCase":
                    return "strcmp(" + doConvert(indent,clas)+ utilFormatExprList(",",args) +",/FOLD_CASE)";

                case "trim":
                    return "strtrim("+doConvert(indent,clas)+",2)";
                case "replace":
                    String search = doConvert("",args.get(0));
                    String replac = doConvert("",args.get(1));
                    return doConvert(indent,clas)+".replace("+search+", "+replac+")";
                case "replaceAll":
                    search= doConvert("",args.get(0));
                    replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                    return indent + "StrJoin( StrSplit( "+ doConvert(indent,clas) + "," + search + ",/extract,/preserve_null ), "+replac + ")";
                case "replaceFirst":
                    search= doConvert("",args.get(0));
                    additionalClasses.put("function replaceFirst, input_str, target, replacement\n" +
"    pos = STRPOS(input_str, target)\n" +
"    IF pos EQ -1 THEN RETURN, input_str\n" +
"    new_str = strmid( input_str, 0, pos ) + replacement + strmid(input_str,pos+STRLEN(target))\n" +
"    RETURN, new_str\n" +
"END",true);
                    replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                    return indent + "replaceFirst("+doConvert("",clas) + "," + search+", "+replac + ")";
                case "valueOf":
                    return indent + "str("+doConvert("",args.get(0)) +")";
                case "split":
                    String arg= utilUnquoteReplacement( doConvert("",args.get(0)) );
                    return indent + "strsplit(" + doConvert(indent,clas) + "," +arg+",/extract)";
                case "join":
                    String arg1= utilUnquoteReplacement( doConvert("",args.get(0)) );
                    String arg2= utilUnquoteReplacement( doConvert("",args.get(1)) );
                    return indent + "strjoin(" + arg2 + "," +arg1+")";
                default:
                    break;
            }
        }
        if ( clasType.equals("Character") ) {
            String s;

            switch ( name ) {
                case "isDigit":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("string(byte(") && s.endsWith("))") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    this.additionalClasses.put("function isDigit, char\n"+
                            "  char_code = BYTE(char)\n" +
                            "  return, (char_code GE 48 AND char_code LE 58)\n"+
                            "end\n",Boolean.TRUE);
                    return "isDigit(" + s + ")";

                case "isSpace":
                case "isWhitespace":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("string(byte(") && s.endsWith("))") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    return safeMethodCall(s,"isspace()");

                case "isLetter":
                case "isAlphabetic":
                    s= doConvert( "",args.get(0) );
                    if ( s.startsWith("string(byte(") && s.endsWith("))") ) {
                        s= s.substring(4,s.length()-1);
                    }
                    this.additionalClasses.put("function isAlpha, char\n"+
                            "  char_code = BYTE(char)\n" +
                            "  return, (char_code GE 65 AND char_code LE 90) OR " +
                            " (char_code GE 97 AND char_code LE 122)\n"+
                            "end\n",Boolean.TRUE);
                    return "isAlpha(" + s + ")";
                default:
                    break;
            }
        }
        if ( clasType.equals("Arrays") ) {
            switch (name) {
                case "copyOfRange": {
                    StringBuilder sb= new StringBuilder();
                    sb.append(indent).append(args.get(0)).append("[");
                    sb.append(doConvert("",args.get(1))).append(":").append(doConvert("",args.get(2))).append("-1");
                    sb.append("]");
                    return sb.toString();
                }
                case "equals": {
                    // if they are not objects, then Python == can be used.
                    StringBuilder sb= new StringBuilder();
                    Type t1= guessType(args.get(0)); // are both arrays containing primative objects (int,float,etc)
                    Type t2= guessType(args.get(1));
                    ArrayType intArrayType = new ArrayType(new PrimitiveType(Primitive.INT));
                    if ( t1!=null && t1.equals(intArrayType)
                            && t2!=null && t2.equals(intArrayType) ) {
                        sb.append(indent).append(doConvert("",args.get(0))).append(" eq ");
                        sb.append(doConvert("",args.get(1)));
                        return sb.toString();
                    }
                }
                case "toString": {
                    String js;
                    js= "strjoin( "+doConvert("",args.get(0)) +",', ')";
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
        if ( clasType.equals("Integer") ) {
            switch ( name ) {
                case "parseInt":
                    return "long("+ doConvert( "", args.get(0) ) +")";
            }
        }
        if ( clasType.equals("Pattern") ) {
            switch ( name ) {
                case "compile":
                    return "(obj_new('IDLJavaObject$Static$Pattern','java.util.regex.Pattern')).compile("+doConvert("",args.get(0))+")";
                case "quote":
                    return "(obj_new('IDLJavaObject$Static$Pattern','java.util.regex.Pattern')).quote("+doConvert("",args.get(0))+")";
            }
        }
        if ( clasType.equals("Pattern")
                && name.equals("matcher") ) {
            return safeMethodCall( clas, "matcher(" + doConvert("",args.get(0) ) + ")" );
        }
        if ( clasType.equals("Matcher") ) {
            if ( name.equals("matches") ) {
                return safeMethodCall( clas, "matches()" );
            } else if ( name.equals("find") ) {
                if ( clas instanceof MethodCallExpr ) {
                    return safeMethodCall( clas, "find()" );
                } else {
                    return safeMethodCall( clas, "find()" );
                }
            }
        }
        if ( clasType.equals("Thread")
                && name.equals("sleep") ) {
            return indent + "wait, "+ "(" + doConvert("",args.get(0) ) + "/1000.)";
        }

        if ( unittest && clas==null ) {
            if ( name.equals("assertEquals") || name.equals("assertArrayEquals") || name.equals("fail") ) {
                StringBuilder sb= new StringBuilder();
                sb.append(indent).append("self.").append(name).append(", ");
                sb.append(doConvert("",args.get(0))).append(", ").append(doConvert("",args.get(1)));
                return sb.toString();
            }
        }


        if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getName().asString().equals("err") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArguments().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArguments().get(0) );
                sb.append(indent).append( "printf, -2, " ).append(s).append("");
            } else {
                String strss;
                if ( !isStringType( guessType(methodCallExpr.getArguments().get(0)) ) ) {
                    strss= "strtrim("+ doConvert( "", methodCallExpr.getArguments().get(0) ) + ",2)";
                } else {
                    strss= doConvert( "", methodCallExpr.getArguments().get(0) );
                }
                sb.append(indent).append( "printf, -2, " ).append( strss );
            }
            return sb.toString();
        } else if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getName().asString().equals("out") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArguments().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArguments().get(0) );
                sb.append(indent).append( "print," ).append( s );
            } else {
                sb.append(indent).append( "print," ).append( doConvert( "", methodCallExpr.getArguments().get(0) ) );
            }
            return sb.toString();
        } else if ( name.equals("print") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getName().asString().equals("err") ) {
            StringBuilder sb= new StringBuilder();
            String s= doConvert( "", methodCallExpr.getArguments().get(0) );
            sb.append(indent).append( "printf, -2, " ).append(s);
            return sb.toString();
        } else if ( name.equals("print") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getName().asString().equals("out") ) {
            StringBuilder sb= new StringBuilder();

            if (  methodCallExpr.getArguments().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArguments().get(0) );
                sb.append(indent).append( "printf, -1, " ).append( s );
            } else {
                sb.append(indent).append( "printf, -1, " ).append( doConvert( "", methodCallExpr.getArguments().get(0) ) );
            }
            return sb.toString();
        } else if ( name.equals("length") && args==null ) {
            return indent + "strlen("+ doConvert("",clas)+")";
        } else if ( name.equals("equals") && args.size()==1 ) {
            return indent + doConvert(indent,clas)+" eq "+ utilFormatExprList(args);
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
            return indent + "exit, status="+ doConvert("",methodCallExpr.getArguments().get(0));

        } else {
            if ( clasType.equals("System") && name.equals("currentTimeMillis") ) {
                return indent + "( systime(1)*1000. )";
            } else if ( clasType.equals("Double") ) {
                //additionalImports.put("from java.lang import Double\n",Boolean.FALSE);
            } else if ( clasType.equals("Integer") ) {
                //additionalImports.put("from java.lang import Integer\n",Boolean.FALSE);
            } else if ( clasType.equals("Short") ) {
                //additionalImports.put("from java.lang import Short\n",Boolean.FALSE);
            } else if ( clasType.equals("Character") ) {
                //additionalImports.put("from java.lang import Character\n",Boolean.FALSE);
            } else if ( clasType.equals("Byte") ) {
                //additionalImports.put("from java.lang import Byte\n",Boolean.FALSE);
            } else if ( clasType.equals("IllegalArgumentException") ) {
                //additionalImports.put("from java.lang import IllegalArgumentException\n",Boolean.FALSE);
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
                            if ( mm.getType() instanceof com.github.javaparser.ast.type.VoidType ) {
                                return indent + javaNameToIdlName( m.getName().toString() ) + "." + javaNameToIdlName( name ) + utilFormatExprList(",",args);
                            } else {
                                return indent + javaNameToIdlName( m.getName().toString() ) + "." + javaNameToIdlName( name ) + "("+ utilFormatExprList(args) +")";
                            }
                        } else {
                            if ( mm.getType() instanceof com.github.javaparser.ast.type.VoidType ) {
                                return indent + "self." + javaNameToIdlName( name ) + utilFormatExprList(",",args);
                            } else {
                                return indent + "self." + javaNameToIdlName( name ) + "("+ utilFormatExprList(args) +")";
                            }
                        }
                    }
                } else {
                    return indent + javaNameToIdlName( name ) + "("+ utilFormatExprList(args) +")";
                }
            } else {
                String clasName = doConvert("",clas);
                if ( name.equals("append") && clas instanceof MethodCallExpr && args.size()==1 ) {
                    return indent + clasName + " + " + utilAssertStr( args.get(0));

                } else {
                    MethodDeclaration mm= this.getCurrentScopeMethods().get(name);
                    if ( mm==null ) {
                        // is this a ExpressionStmt?
                        StackTraceElement[] ss= new Exception().getStackTrace();
                        boolean exprStmt=ss[2].getMethodName().equals("doConvertExpressionStmt");
                        if ( exprStmt ) {
                            if ( onlyStatic && clasName.equals(theClassName) )  {
                                return indent            + javaNameToIdlName( name ) + utilFormatExprList(",",args);
                            } else {
                                return indent + clasName +"."+javaNameToIdlName( name )+ utilFormatExprList(",",args);
                            }
                        } else {
                            if ( onlyStatic && clasName.equals(theClassName) )  {
                                return indent            + javaNameToIdlName( name ) + "("+ utilFormatExprList("",args)+")";
                            } else {
                                return indent + clasName +"."+javaNameToIdlName( name )+ "("+ utilFormatExprList("",args)+")";
                            }
                        }
                    } else {
                        if ( onlyStatic && clasName.equals(theClassName) )  {
                            if ( mm.getType() instanceof com.github.javaparser.ast.type.VoidType ) {
                                return indent            + javaNameToIdlName( name ) + utilFormatExprList(",",args);
                            } else {
                                return indent            + javaNameToIdlName( name ) + "("+ utilFormatExprList("",args) + ")";
                            }
                        } else {
                            String nn= clasName.replace("()","");
                            //TODO: mm is not the class, what is the class of clasName, and what is this return type?
                            if ( mm.getType() instanceof com.github.javaparser.ast.type.VoidType ) {
                                return indent + nn +"."+javaNameToIdlName( name )+ utilFormatExprList(",",args) ;
                            } else {
                                return indent + nn +"."+javaNameToIdlName( name )+ "("+ utilFormatExprList("",args) +")";
                            }
                        }
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
                result= indent + "obj_new()";
                break;
            case "BooleanLiteralExpr":
                result= indent + ( ((BooleanLiteralExpr)n).getValue() ? "1" : "0" );
                break;
            case "LongLiteralExpr":
                result= indent + ((LongLiteralExpr)n).getValue().replace("L",""); // all ints are longs.
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
            // TODO: replace MultiTypeParameters, which disappeared in the newer JavaParser version.
            // case "MultiTypeParameter":
            //     result= doConvertMultiTypeParameter(indent,(MultiTypeParameter)n);
            //     break;
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
                result= indent + ((Parameter)n).getName().asString(); // TODO: varargs, etc
                break;
            case "ForEachStmt":
                result= doConvertForEachStmt(indent,(ForEachStmt)n);
                break;
            case "EmptyStmt":
                result= doConvertEmptyStmt(indent,(EmptyStmt)n);
                break;
            case "VariableDeclarator":
                result= indent + ((VariableDeclarator)n).getName().toString();
                break;
            case "SuperExpr":
                result= indent + "super()";
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
        s= s.replaceAll("\\\\\\\\","\\\\");
        s= s.replaceAll("'","\\\\'");
        return "'" + s + "'";
    }

    private String doConvertBlockStmt(String indent,BlockStmt blockStmt) {

        pushScopeStack(true);

        StringBuilder result= new StringBuilder();
        List<Statement> statements= blockStmt.getStatements();
        if ( statements==null ) {
            popScopeStack();
            return "; pass\n";
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
                if ( !l.trim().startsWith(";") ) lines++;
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
        boolean afterFirst = false;
        for (VariableDeclarator v : variableDeclarationExpr.getVariables()) {
            if (afterFirst) b.append("\n");
            String s= v.getName().toString();
            // TODO: avoid using null when initializer is missing.
            Expression initializer = v.getInitializer().orElse(null);
            if ( initializer!=null && initializer.toString().startsWith("Logger.getLogger") ) {
                //addLogger();
                localVariablesStack.peek().put(s,new ClassOrInterfaceType("Logger") );
                return indent + "; J2J: "+variableDeclarationExpr.toString().trim();
            }
            if ( camelToSnake ) {
                String news= camelToSnakeAndRegister(s);
                s= news;
            }
            if ( s.equals("gt") ) {
                String news= "gt_";
                nameMapForward.put( s, news );
                nameMapReverse.put( news, s );
                s= news;
            } else if ( s.equals("lt") ) {
                String news= "lt_";
                nameMapForward.put( s, news );
                nameMapReverse.put( news, s );
                s= news;
            }

            // TODO: we should guard against the many reserved words in IDL, like switch
            if ( initializer!=null
                    && ( initializer instanceof ArrayInitializerExpr )
                    && ( v.getType() instanceof PrimitiveType ) ) {
                Type t= new ArrayType( ((PrimitiveType)v.getType()));
                localVariablesStack.peek().put( s, t );
            } else {
                localVariablesStack.peek().put( s, v.getType() );
            }
            if ( initializer!=null ) {
                if ( initializer instanceof ObjectCreationExpr && ((ObjectCreationExpr)initializer).getAnonymousClassBody().isPresent() ) {
                    for ( BodyDeclaration bd: ((ObjectCreationExpr)initializer).getAnonymousClassBody().get() ) {
                        b.append(doConvert( indent+"; J2J:", bd ) );
                    }
                }
                b.append( indent ).append(s).append(" = ").append(doConvert("",initializer) );
            }
            afterFirst = true;
        }
        return b.toString();
    }

    /**
     * returns the "endif else if" stuff without indenting
     * @param indent
     * @param ifStmt
     * @return
     */
    private String specialConvertElifStmt( String indent, IfStmt ifStmt ) {
        StringBuilder b= new StringBuilder();
        b.append(indent).append("endif else if ");
        b.append( doConvert("", ifStmt.getCondition() ) );
        b.append(" then begin\n");
        b.append( doConvert(indent,ifStmt.getThenStmt() ) );
        if ( ifStmt.getElseStmt().isPresent() ) {
            Statement elseStmt = ifStmt.getElseStmt().get();
            if ( elseStmt instanceof IfStmt ) {
                b.append( specialConvertElifStmt( indent, (IfStmt)elseStmt ) );
            } else {
                b.append(indent).append("endif else begin\n");
                b.append( doConvert(indent, elseStmt) );
                b.append(indent).append("endelse\n");
            }
        } else {
            b.append(indent).append("endif\n");
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
            return indent + "; J2J: if "+ifStmt.getCondition() + " ... removed";
        }
        b.append(indent).append("if ");
        b.append( doConvert("", ifStmt.getCondition() ) );
        if ( ifStmt.getThenStmt() instanceof BlockStmt ) {
            b.append(" then begin\n");
            b.append( doConvert(indent,ifStmt.getThenStmt() ) );
            b.append( indent ) .append( "endif ");
        } else {
            b.append(" then begin\n");
            b.append( doConvert(indent+s4,ifStmt.getThenStmt() ) ).append("\n");
            b.append( indent ) .append( "endif ");
        }
        if ( ifStmt.getElseStmt().isPresent() ) {
            Statement elseStmt = ifStmt.getElseStmt().get();
            if ( elseStmt instanceof IfStmt ) {
                String ssss= specialConvertElifStmt( indent, (IfStmt)elseStmt ) ;
                int i= ssss.indexOf("endif ");
                b.append( ssss.substring(i+6) );
            } else {
                b.append("else");
                if ( elseStmt instanceof BlockStmt ) {
                    b.append(" begin\n");
                    b.append( doConvert(indent,elseStmt) );
                    b.append(indent).append( "endelse\n" );
                } else {
                    b.append(" begin\n");
                    b.append( doConvert(indent+s4,elseStmt ) ).append("\n");
                    b.append(indent).append("endelse\n ");
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
        VariableDeclarator v=null;
        if ( init!=null && init.size()==1 && init.get(0) instanceof VariableDeclarationExpr ) {
            init1= (VariableDeclarationExpr)init.get(0);
            List<VariableDeclarator> variables = init1.getVariables();
            if (variables.size()!=1 ) {
                init1= null;
            } else {
                v= variables.get(0);
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
                && ( compare.getOperator()==BinaryExpr.Operator.LESS || compare.getOperator()==BinaryExpr.Operator.LESS_EQUALS
                || compare.getOperator()==BinaryExpr.Operator.GREATER || compare.getOperator()==BinaryExpr.Operator.GREATER_EQUALS );
        String compareTo= doConvert("",compare.getRight());
        if ( compare.getOperator()==BinaryExpr.Operator.LESS ) {
            if ( compare.getRight() instanceof IntegerLiteralExpr ) {
                compareTo= String.valueOf(Integer.parseInt(((IntegerLiteralExpr)compare.getRight()).getValue())-1);
            } else {
                compareTo = compareTo + "-1";
            }
        } else if ( compare.getOperator()==BinaryExpr.Operator.GREATER ) {
            if ( compare.getRight() instanceof IntegerLiteralExpr ) {
                compareTo= String.valueOf(Integer.parseInt(((IntegerLiteralExpr)compare.getRight()).getValue())+1);
            } else {
                compareTo = compareTo + "+1";
            }
        }

        boolean updateOkay= update1!=null;

        boolean bodyOkay= false;
        if ( initOkay && compareOkay && updateOkay ) { // check that the loop variable isn't modified within loop
            bodyOkay= utilCheckNoVariableModification( forStmt.getBody(), v.getName().toString() );
        }

        boolean usedWhile= false;
        if ( initOkay && compareOkay && updateOkay && bodyOkay ) {
            b.append(indent).append( "for " ).append( v.getName() ).append("=");
            b.append("");
            // The follow will raise an exception if the initiliazer is missing.
            // TODO: what should we do if the initializer is missing.
            b.append( doConvert("",v.getInitializer().get()) ).append(",");
            b.append( compareTo );
            if (  update1.getOperator()==UnaryExpr.Operator.POSTFIX_DECREMENT
                    || update1.getOperator()==UnaryExpr.Operator.PREFIX_DECREMENT ) {
                b.append(",-1");
            }
            b.append(" do begin\n");
        } else {
            if ( init!=null ) {
                init.forEach((e) -> {
                    b.append(indent).append( doConvert( "", e ) ).append( "\n" );
                });
            }
            // forStmt.getCompare().get() will throw an exception if the comparison is missing.
            // TODO: handle a missing comparison.
            b.append( indent ).append("while ").append(doConvert( "", forStmt.getCompare().get() )).append(" do begin  ; J2J for loop\n");
            usedWhile=true;
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
        if ( usedWhile ) {
            b.append(indent).append("endwhile\n");
        } else {
            b.append(indent).append("endfor\n");
        }
        return b.toString();
    }

    private String doConvertForEachStmt(String indent, ForEachStmt forEachStmt) {
        StringBuilder b= new StringBuilder();
        List<VariableDeclarator> variables = forEachStmt.getVariable().getVariables();
        if ( variables.size()!=1 ) {
            throw new IllegalArgumentException("expected only one variable in foreach statement");
        }
        String variableName = variables.get(0).getName().toString();
        Type variableType= variables.get(0).getType();
        localVariablesStack.push( new HashMap<>(localVariablesStack.peek()) );
        localVariablesStack.peek().put( variableName,variableType );
        b.append( indent ).append("foreach ").append(variableName).append(", ").append(doConvert("",forEachStmt.getIterable() )).append(" do begin\n");
        if ( forEachStmt.getBody() instanceof ExpressionStmt ) {
            b.append(indent).append(s4).append( doConvert( "", forEachStmt.getBody() ) ).append("\n");
        } else {
            b.append( doConvert( indent, forEachStmt.getBody() ) );
        }
        b.append( indent ).append( "end\n" );
        localVariablesStack.pop( );
        return b.toString();
    }


    private String doConvertImportDeclaration( String indent, ImportDeclaration d ) {
        return "";
        // TODO: implement this method. As is, it was always returning empty string.
        // QualifiedNameExpr is not present in newer versions of JavaParser.
//         StringBuilder sb= new StringBuilder();
//         NameExpr n= d.getName();
//         if ( n instanceof QualifiedNameExpr ) {
//             QualifiedNameExpr qn= (QualifiedNameExpr)n;
// //            sb.append( "; from " ).append( qn.getQualifier() ).append( " import " ).append( n.getName() );
//         } else {
//             String nn= n.getName().asString();
//             int i= nn.lastIndexOf(".");
// //            sb.append( "; from " ).append( nn.substring(0,i) ).append( " import " ).append( nn.substring(i));
//         }
//         return sb.toString();
    }

    private String doConvertCompilationUnit(String indent, CompilationUnit compilationUnit) {

        pushScopeStack(false);

        StringBuilder sb= new StringBuilder();

        // TODO: could we use CompilationUnit.getImports() here?
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
        if ( !returnStmt.getExpression().isPresent() ) {
            return indent + "return";
        } else {
            return indent + "return, " + doConvert("", returnStmt.getExpression().get());
        }
    }

    private String doConvertArrayCreationExpr(String indent, ArrayCreationExpr arrayCreationExpr) {
        if ( arrayCreationExpr.getInitializer().isPresent()) {
            ArrayInitializerExpr ap= arrayCreationExpr.getInitializer().get();
            StringBuilder sb= new StringBuilder();
            return "[" + utilFormatExprList( "",ap.getValues() ) + "]";
        } else {
            String item;
            if ( arrayCreationExpr.getElementType().equals( new PrimitiveType(Primitive.BYTE) ) ||
                    arrayCreationExpr.getElementType().equals( new PrimitiveType(Primitive.SHORT) ) ||
                    arrayCreationExpr.getElementType().equals( new PrimitiveType(Primitive.INT) ) ||
                    arrayCreationExpr.getElementType().equals( new PrimitiveType(Primitive.LONG) ) ) {
                item="0";
            } else if ( arrayCreationExpr.getElementType().equals( new PrimitiveType(Primitive.CHAR) )) {
                item= "''";
            } else if ( arrayCreationExpr.getElementType().toString().equals( "String" ) ) { // TODO: no kludgey
                item= "''";
            } else {
                item= "None";
            }
            if ( arrayCreationExpr.getLevels().size()>1 ) {
                throw new IllegalStateException("unable to handle multi-dimensional arrays");
            }
            // TODO: is it possible for levels to be empty?
            return "replicate("+item+ ","+doConvert( "", arrayCreationExpr.getLevels().get(0) ) + ")" ;

        }
    }

    private String doConvertFieldAccessExpr(String indent, FieldAccessExpr fieldAccessExpr) {
        String s= doConvert( "", fieldAccessExpr.getScope() );

        if ( this.getCurrentScopeClasses().containsKey(s) ) {
            if ( !this.theClassName.equals(s) ) {
                s= the_class_name + "." + s;
            }
        }

        String fieldName = fieldAccessExpr.getName().asString();
        // test to see if this is an array and "length" of the array is accessed.
        if ( fieldName.equals("length") ) {
            String inContext= s;
            if ( inContext.startsWith("self.") ) {
                inContext= inContext.substring(5);
            }
            Type t= localVariablesStack.peek().get(inContext);
            if (t==null ) {
                t= getCurrentScope().get(inContext);
            }
            if ( t!=null && t instanceof ArrayType && ((ArrayType)t).getArrayLevel()>0 ) {
                return indent + "n_elements("+ s + ")";
            }
        }
        if ( onlyStatic && s.equals(classNameStack.peek()) ) {
            return fieldName;
        } else {
            if ( s.equals("Collections") ) {
                switch (fieldName) {
                    case "EMPTY_MAP":
                        return indent + "HASH()";
                    case "EMPTY_SET":
                        return indent + "HASH()"; // Jython 2.2 does not have sets.
                    case "EMPTY_LIST":
                        return indent + "LIST()";
                    default:
                        break;
                }
            }

            FieldDeclaration field= stackFields.peek().get(fieldName);
            if (ConversionUtils.isStaticField(field)) {
                return indent + s + "_" + fieldName;
            } else {
                return indent + s + "." + fieldName;
            }
        }
    }

    private String doConvertArrayAccessExpr(String indent, ArrayAccessExpr arrayAccessExpr) {
        if ( arrayAccessExpr.getName() instanceof ArrayAccessExpr ) { // double array indeces are backwards in IDL
            String idx1= doConvert("",arrayAccessExpr.getIndex());
            String idx2= doConvert("",((ArrayAccessExpr)arrayAccessExpr.getName()).getIndex());
            String s= doConvert("",((ArrayAccessExpr)arrayAccessExpr.getName()).getName());
            return indent + s + "[" + idx1 + "," + idx2 + "]";
        } else {
            return doConvert( indent,arrayAccessExpr.getName()) + "["+doConvert("",arrayAccessExpr.getIndex())+"]";
        }
    }

    private String doConvertUnaryExpr(String indent, UnaryExpr unaryExpr) {
         switch (unaryExpr.getOperator()) {
            case PREFIX_INCREMENT: {
                additionalClasses.put("; J2J: increment used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpression());
                return indent + n + " = " + n + " + 1";
            }
            case PREFIX_DECREMENT: {
                additionalClasses.put("; J2J: decrement used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpression());
                return indent + n + " = " + n + " - 1";
            }
            case POSTFIX_INCREMENT: {
                additionalClasses.put("; J2J: increment used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpression());
                return indent + n + " = " + n + " + 1";
            }
            case POSTFIX_DECREMENT: {
                additionalClasses.put("; J2J: decrement used at line "+ConversionUtils.getBeginLine(unaryExpr)+", which needs human study.\n",true );
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
                return indent + "not" +"("+n+")" ;
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
            if ( camelToSnake ) {
                the_class_name= camelToSnake(name);
            } else {
                the_class_name= name;
            }
        }
        classNameStack.push(name);

        nameMapForward.put("gt","gt_");
        nameMapForward.put("lt","lt_");
        nameMapForward.put("eq","eq_");

        for ( Entry<String,String> e: nameMapForward.entrySet() ) {
            nameMapReverse.put(e.getValue(),e.getKey());
        }

        getCurrentScopeClasses().put( name, classOrInterfaceDeclaration );

        pushScopeStack(false);
        getCurrentScope().put( "this", new ClassOrInterfaceType(name) );

        classOrInterfaceDeclaration.getChildNodes().forEach((n) -> {
            if ( n instanceof MethodDeclaration ) {
                classMethods.put( ((MethodDeclaration) n).getName().asString(), classOrInterfaceDeclaration );
                getCurrentScopeMethods().put(((MethodDeclaration) n).getName().asString(),(MethodDeclaration)n );
            }
        });

        if ( onlyStatic ) {
            classOrInterfaceDeclaration.getChildNodes().forEach((n) -> {
                sb.append( doConvert(indent,n) ).append("\n");
                sb.append( "\n" );
            });
        } else {

            if ( unittest ) {
                sb.append( "\n; cheesy unittest temporary\n");
                sb.append("pro ").append(the_class_name).append("::assertEquals, a, b \n");
                sb.append( "    if not ( a eq b ) then stop, 'a ne b'\n");
                sb.append( "end\n" );
                sb.append( "\n" );
                sb.append("pro ").append(the_class_name).append("::assertArrayEquals, a, b\n");
                sb.append( "    if n_elements(a) eq n_elements(b) then begin\n");
                sb.append( "        for i=0,n_elements(a)-1 do begin\n");
                sb.append( "            if a[i] ne b[i] then stop, string(format='a[%d] ne [%d]',i,i)\n" );
                sb.append( "        endfor\n" );
                sb.append( "    endif else begin\n" );
                sb.append( "        stop, 'arrays are different lengths'\n");
                sb.append( "    endelse\n" );
                sb.append( "end\n" );
                sb.append( "\n" );
                sb.append("pro ").append(the_class_name).append("::fail, msg\n");
                sb.append( "    print, msg\n" );
                sb.append( "    stop, 'fail: '+msg\n" );
                sb.append( "end\n" );
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
                if ( unittest ) {
                    //String extendName= "unittest.TestCase";
                    //sb.append( indent ).append("class " ).append( pythonName ).append("(" ).append(extendName).append(")").append(":\n");
                } else {
                    //sb.append( indent ).append("class " ).append( pythonName ).append(":\n");
                }
            }

            // check to see if any two methods can be combined.
            // https://github.com/jbfaden/JavaJythonConverter/issues/5
            classOrInterfaceDeclaration.getChildNodes().forEach((n) -> {
                if ( n instanceof MethodDeclaration ) {
                    classMethods.put( ((MethodDeclaration) n).getName().asString(), classOrInterfaceDeclaration );
                    getCurrentScopeMethods().put(((MethodDeclaration) n).getName().asString(),(MethodDeclaration)n );
                } else if ( n instanceof ClassOrInterfaceDeclaration ) {
                    ClassOrInterfaceDeclaration coid= (ClassOrInterfaceDeclaration)n;
                    getCurrentScope().put( coid.getName().asString(), new ClassOrInterfaceType(coid.getName().asString()) );
                }
            });

            // object data goes into structure with the same name
            StringBuilder structureDefinition= new StringBuilder();

            // check for unique names
            Map<String,Node> nn= new HashMap<>();
            for ( Node n: classOrInterfaceDeclaration.getChildNodes() ) {

                if ( n instanceof ClassOrInterfaceType ) {
                    String name1= ((ClassOrInterfaceType)n).getName().asString();
                    if ( nn.containsKey(name1) ) {
                        sb.append(indent).append("; J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                    }
                    nn.put( name1, n );

                } else if ( n instanceof FieldDeclaration ) {

                    for ( VariableDeclarator vd : ((FieldDeclaration)n).getVariables() ) {
                        String name1= vd.getName().asString();
                        if ( nn.containsKey(name1) ) {
                            sb.append(indent).append("; J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                        }
                        getCurrentScope().put( name1, (ConversionUtils.getFieldType((FieldDeclaration)n) )); //TODO: Does Python and JavaScript have this?
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
                            sb.append(indent).append("; J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                    }
                    nn.put( name + "_" + name1, n );
                } else {
                    System.err.println("Not supported: "+n);
                }
            }

            StringBuilder commons= new StringBuilder();
            StringBuilder commonsInit= new StringBuilder(); // the part which will go in the common block

            ConstructorDeclaration constructor= null;

            for ( Node n : classOrInterfaceDeclaration.getChildNodes() ) {
                if ( n instanceof FieldDeclaration ) {
                    boolean isStatic= ConversionUtils.isStaticField((FieldDeclaration)n);
                    for ( VariableDeclarator vd : ((FieldDeclaration)n).getVariables() ) {
                        String vname= vd.getName().asString();

                        getCurrentScopeFields().put( vname,(FieldDeclaration)n);

                        if ( isStatic ) {
                            commons.append(", ").append(name).append("_").append(vname);
                            commonForStaticVariables= "common "+the_class_name;
                            if ( vd.getInitializer().isPresent() ) {
                                String v= doConvert("",vd.getInitializer().get());
                                commonsInit.append(indent).append(s4).append(the_class_name).append("_").append(vname).append("=").append(v).append("\n");
                            }
                        } else {
                            if ( vd.getInitializer().isPresent() ) {
                                structureDefinition.append(",").append(vname).append(":").append(doConvert("",vd.getInitializer().get()));
                            } else {
                                structureDefinition.append(",").append(vname).append(":ptr_new()");
                            }
                        }
                    }
                }

            }

            if ( commonForStaticVariables.length()>0 ) {
                commonForStaticVariables= commonForStaticVariables + commons;
            }

            for ( Node n : classOrInterfaceDeclaration.getChildNodes() ) {
                sb.append("\n");
                if ( n instanceof ClassOrInterfaceType ) {
                    // skip this strange node
                    sb.append("; J2J: inner class skipped: ").append(((ClassOrInterfaceType)n).getName()).append("\n");
                } else if ( n instanceof ConstructorDeclaration ) {
                    if ( constructor!=null ) {
                        sb.append("; J2J: multiple constructors cannot be converted\n");
                    }
                    constructor= (ConstructorDeclaration)n;
                } else if ( n instanceof MethodDeclaration ) {
                    MethodDeclaration md=  (MethodDeclaration)n;
                    if ( md.getAnnotations()!=null ) {
                        if ( md.getAnnotations().contains( DEPRECATED ) ) {
                            continue;
                        }
                    }
                    sb.append( doConvert( indent, n ) ).append("\n");
                }
            }

            // write the define block which identifies common block with static variables.
            if ( !onlyStatic ) {

                //stackFields;

                sb.append("\n");
                sb.append( indent ).append("pro ").append(the_class_name ). append("__define\n") ;
                if ( structureDefinition.length()>0 ) {
                    sb.append( indent ).append( s4 ).append( "dummy={").append(the_class_name).append(",").append(structureDefinition.substring(1)).append("}\n");
                } else {
                    sb.append( indent ).append( s4 ).append("dummy={").append(the_class_name ).append(",dummy:0}\n");

                }
                if ( constructor!=null ) {
                    String ss= doConvert( indent, constructor.getBody() );
                    sb.append(ss);
                }
                if ( commons.length()>0 ) {
                    String c= commons.substring(1);
                    sb.append( indent ).append( s4 ).append( "common " ).append(the_class_name ).append(",").append(c).append("\n");
                }
                if ( commonsInit.length()>0 ) {
                    sb.append( indent ).append( commonsInit ).append("\n");
                }

                sb.append( indent ).append( s4 ).append( "return\n" );
                sb.append( indent ).append( "end\n" );
            }

            if ( unittest ) {
                sb.append("; Run the following code on the command line:\n");
                sb.append("; o=obj_new('TimeUtilTest')    \n");
                sb.append("; o.runtests                   \n");
                sb.append("pro ").append(the_class_name).append("::RunTests\n");
                sb.append("    Test = obj_new(\'").append(classOrInterfaceDeclaration.getName()).append("\')\n");
                for ( Node n : classOrInterfaceDeclaration.getChildNodes() ) {
                    if ( n instanceof MethodDeclaration
                            && ((MethodDeclaration)n).getName().asString().startsWith("test")
                            && ((MethodDeclaration)n).getParameters().isEmpty() ) {
                        sb.append("    test.").append(((MethodDeclaration) n).getName()).append("\n");
                    }
                }
                sb.append("end\n");
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
                if ( a.getName().asString().equals("Deprecated") ) {
                    return "";
                }
            }
        }

        StringBuilder sb= new StringBuilder();
        String comments= utilRewriteComments( indent, methodDeclaration.getComment() );
        sb.append( comments );

        String methodName= methodDeclaration.getName().asString();
        String idlName;
        if ( camelToSnake ) {
            idlName= camelToSnakeAndRegister(methodName);
        } else {
            idlName= methodName;
        }

        // some are already registered, like "gt"
        if ( nameMapForward.containsKey(methodName) ) {
            idlName= nameMapForward.get(methodName);
        }

        String classNameQualifier= onlyStatic ? "" : the_class_name + "::";

        if ( methodDeclaration.getType() instanceof com.github.javaparser.ast.type.VoidType ) {
            sb.append( indent ).append( "pro " ).append(classNameQualifier).append( idlName );
        } else {
            sb.append( indent ).append( "function " ).append(classNameQualifier).append( idlName );
        }

        boolean comma;

        comma = true;

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
        sb.append( "\n" );

        if ( methodDeclaration.getBody().isPresent() ) {
            if ( isStatic && !onlyStatic ) {
                sb.append(indent).append(s4).append("compile_opt idl2, static\n");
                if ( this.commonForStaticVariables.length()>0 ) {
                    sb.append(indent).append(s4).append(this.commonForStaticVariables).append("\n");
                }
            }
            sb.append( doConvert( indent, methodDeclaration.getBody().get() ) );
        } else {
            sb.append(indent).append(s4).append("pass");
        }
        popScopeStack();

        sb.append("end");
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
                    getCurrentScope().put( name, new ClassOrInterfaceType( "Logger") );
                    //addLogger();
                    sb.append( indent ).append("; J2J: ").append(fieldDeclaration.toString());
                    continue;
                }

                getCurrentScope().put( name,ConversionUtils.getFieldType(fieldDeclaration) );
                getCurrentScopeFields().put( name,fieldDeclaration);

                if ( !v.getInitializer().isPresent() ) {
                    String implicitDeclaration = utilImplicitDeclaration( ConversionUtils.getFieldType(fieldDeclaration) );
                    if ( implicitDeclaration!=null ) {
                        sb.append( indent ).append( pythonName ).append(" = ").append( implicitDeclaration ).append("\n");
                    } else {
                        sb.append( indent ).append( pythonName ).append(" = ").append( "None  ; J2J added" ).append("\n");
                    }
                } else if ( v.getInitializer().get() instanceof ConditionalExpr ) {
                    ConditionalExpr ce= (ConditionalExpr)v.getInitializer().get();
                    sb.append( indent ).append("if ").append(doConvert( "",ce.getCondition() )).append(":\n");
                    sb.append( indent ).append(s4).append(pythonName).append(" = ").append( doConvert( "",ce.getThenExpr() ) ).append("\n");
                    sb.append( indent ).append( "else:\n");
                    sb.append( indent ).append(s4).append(pythonName).append(" = ").append( doConvert( "",ce.getElseExpr() ) ).append("\n");

                } else {

                }
            }
        }
        return sb.toString();
    }

    private String doConvertThrowStmt(String indent, ThrowStmt throwStmt) {
        Expression msg= throwStmt.getExpression();
        if ( throwStmt.getExpression() instanceof ObjectCreationExpr ) {
            List<Expression> args= ((ObjectCreationExpr)throwStmt.getExpression()).getArguments();
            if ( args.size()==1 ) {
                msg= args.get(0);
            }
        }
        return indent + "stop, !error_state.msg";
    }

    private String doConvertWhileStmt(String indent, WhileStmt whileStmt) {
        StringBuilder sb= new StringBuilder(indent);
        sb.append( "WHILE ");
        sb.append( doConvert( "", whileStmt.getCondition() ) );

        if ( whileStmt.getBody() instanceof ExpressionStmt ) {
            sb.append( " DO " );
            sb.append( doConvert( "", whileStmt.getBody() ) );
            sb.append("\n");
        } else {
            sb.append( " DO BEGIN\n" );
            sb.append( doConvert( indent, whileStmt.getBody() ) );
            sb.append(indent).append("ENDWHILE\n");
        }

        return sb.toString();
    }

    private String doConvertArrayInitializerExpr(String indent, ArrayInitializerExpr arrayInitializerExpr) {
        return indent + "[" + utilFormatExprList( "", arrayInitializerExpr.getValues() ) + "]";
    }

    private String doConvertSwitchStmtBodyEnd(String indent, List<Statement> statements) {
        StringBuilder b= new StringBuilder();
        for ( Statement s : statements ) {
            b.append( doConvert( indent,s) );
            b.append( "\n" );
        }
        b.append( indent ).append("END\n");
        return b.toString();
    }

    private String doConvertSwitchStmt(String indent, SwitchStmt switchStmt) {
         StringBuilder b= new StringBuilder();
        b.append( indent ).append( "SWITCH " );
        b.append( doConvert("",switchStmt.getSelector()) ).append(" OF\n");
        String nextIndent= indent + s4;
        String nextNextIndent = nextIndent + s4;
        for ( SwitchEntry ses: switchStmt.getEntries() ) {
            List<Expression> labels= ses.getLabels();
            List<Statement> statements= ses.getStatements();
            // If there are no labels, that indicates this is a `default` entry.
            // Otherwise, there may be multiple labels. We handle that currently
            // by repeating the entry's body.
            if ( labels.isEmpty() ) {
                b.append( nextIndent ).append( "ELSE: BEGIN\n");
                b.append(doConvertSwitchStmtBodyEnd(indent, statements));
            } else {
                // TODO: Does IDL support multiple labels for a single switch entry?
                for ( Expression label : labels ) {
                    String slabel= doConvert("",label);
                    b.append( nextIndent ).append( "").append( slabel ).append(": BEGIN\n");
                    doConvertSwitchStmtBodyEnd(indent, statements);
                }
            }
        }
        b.append( indent ).append("ENDSWITCH\n");
        return b.toString();
    }

    private static String utilRewriteComments(String indent, Optional<Comment> commentsOption) {
        if (!commentsOption.isPresent()) {
            return "";
        }
        Comment comments = commentsOption.get();
        StringBuilder b= new StringBuilder();
        String[] ss= comments.getContent().split("\n");
        if ( ss[0].trim().length()==0 ) {
            ss= Arrays.copyOfRange( ss, 1, ss.length );
        }
        if ( ss[ss.length-1].trim().length()==0 ) {
            ss= Arrays.copyOfRange( ss, 0, ss.length-1 );
        }

        StringBuilder paramsBuilder= new StringBuilder();
        String returns="";
        StringBuilder seeBuilder= new StringBuilder();

        b.append(indent).append(";+\n");
        for ( String s : ss ) {
            int i= s.indexOf("*");
            if ( i>-1 && s.substring(0,i).trim().length()==0 ) {
                s= s.substring(i+1);
            }
            s= s.trim();
            if ( s.startsWith("@param") ) {
                s= s.substring(7);
                int j= s.indexOf(" ");
                String paramName;
                if ( j>-1 ) {
                    paramName= s.substring(0,j).trim();
                } else {
                    paramName= s;
                }
                if ( paramName.charAt(paramName.length()-1)==',' ) {
                    paramName= paramName.substring(0,paramName.length()-1);
                }
                String paramDescription;
                if ( j>-1 ) {
                    paramDescription= s.substring(j).trim();
                    paramsBuilder.append(indent).append(";   ").append(paramName).append(" - ").append(paramDescription).append("\n");
                } else {
                    paramsBuilder.append(indent).append(";   ").append(paramName);
                }
            } else if ( s.startsWith("@return") ) {
                returns= s.substring(7);
            } else if ( s.startsWith("@see") ) {
                seeBuilder.append( indent ) .append( ";   ").append( s.substring(4) ).append("\n");
            } else {
                b.append(indent).append(";").append(s).append("\n");
            }
        }
        if ( paramsBuilder.length()>0 ) {
            b.append(";\n; Parameters:\n");
            b.append(paramsBuilder);
        }

        if ( returns.length()>0 ) {
            b.append(";\n; Returns:\n");
            b.append(";  ").append(returns).append("\n");
        }

        if ( seeBuilder.length()>0 ) {
            b.append(";\n; See:\n");
            b.append(seeBuilder).append("\n");
        }

        b.append(indent).append(";-\n");
        return b.toString();
    }

    private String doConvertClassOrInterfaceType(String indent, ClassOrInterfaceType classOrInterfaceType) {
        return indent + classOrInterfaceType.getName().asString();
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
                    if ( Primitive.INT.equals(guessType(e)) ) { // new StringBuilder(100);
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
            if ( objectCreationExpr.getArguments().isEmpty() ) {
                return indent + "Exception()";
            } else {
                return indent + "Exception("+ doConvert("",objectCreationExpr.getArguments().get(0))+")";
            }
        } else {
            String qualifiedName= utilQualifyClassName(objectCreationExpr.getType());
            if ( qualifiedName!=null ) {
                return indent + qualifiedName + "("+ utilFormatExprList(objectCreationExpr.getArguments())+ ")";
            } else {
                if ( objectCreationExpr.getAnonymousClassBody().isPresent()) {
                    StringBuilder sb= new StringBuilder();
                    String body= doConvert( indent, objectCreationExpr.getAnonymousClassBody().get().get(0) );
                    sb.append(indent).append(objectCreationExpr.getType()).append("(").append(utilFormatExprList(objectCreationExpr.getArguments())).append(")");
                    sb.append("*** ; J2J: This is extended in an anonymous inner class ***");
                    return sb.toString();
                } else {
                    if ( objectCreationExpr.getType().getName().asString().equals("HashMap") ) {
                        return indent + "HASH()";
                    } else if ( objectCreationExpr.getType().getName().asString().equals("ArrayList") ) {
                        return indent + "LIST()";
                    } else if ( objectCreationExpr.getType().getName().asString().equals("HashSet") ) {
                        return indent + "HASH()";
                    } else {
                        if ( javaImports.keySet().contains( objectCreationExpr.getType().getName() ) ) {
                            javaImports.put( objectCreationExpr.getType().getName().asString(), true );
                        }
                        if ( objectCreationExpr.getType().getName().asString().equals("String") ) {
                            if ( objectCreationExpr.getArguments().size()==1 ) {
                                Expression e= objectCreationExpr.getArguments().get(0);
                                Type t= guessType(e);
                                if ( t instanceof ReferenceType
                                        && t.equals(new ArrayType(new PrimitiveType(Primitive.CHAR)) ) ) {
                                    return indent + "strjoin( "+ doConvert("",e) +")";
                                } else if ( t!=null && t.equals(new ClassOrInterfaceType( "StringBuilder") ) ) {
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
        return indent + "(" + doConvert("",conditionalExpr.getCondition()) + ") ? " +
                doConvert("",conditionalExpr.getThenExpr()) + " : "
                    + doConvert("",conditionalExpr.getElseExpr());
    }

    private String doConvertCastExpr(String indent, CastExpr castExpr) {
        String type= castExpr.getType().toString();
        switch (type) {
            case "String":
                type= "str";
                break;
            case "char":
                if ( ConversionUtils.isIntegerType(guessType(castExpr.getExpression())) ) {
                    type = "byte";
                } else {
                    return "string(byte("+doConvert("", castExpr.getExpression() ) + "))";
                }
                break;
            case "int":
                type= "long";
                break;
            case "long":
                type= "long64";
                break;
            default:
                type = ""; // (FieldHandler)fh
                break;
        }
        return type + "(" + doConvert("", castExpr.getExpression() ) + ")";
    }

    /**
     * TODO: verify this
     * @param indent
     * @param tryStmt
     * @return
     */
    private String doConvertTryStmt(String indent, TryStmt tryStmt) {
        StringBuilder sb= new StringBuilder();
        sb.append( indent ).append( "catch, err\n");
        sb.append( indent ).append( "if err eq 0 then begin\n");
        sb.append( doConvert( indent, tryStmt.getTryBlock() ) );
        sb.append( indent ).append( "endif else begin\n");
        for ( CatchClause cc: tryStmt.getCatchClauses() ) {
            sb.append( doConvert( indent, cc.getBody() ) );
        }
        if ( tryStmt.getFinallyBlock().isPresent()) {
            sb.append( indent ).append( "finally:\n");
            sb.append( doConvert( indent, tryStmt.getFinallyBlock().get() ) );
        }
        sb.append( indent ).append( "endelse\n");
        sb.append( indent ).append( "catch, /cancel\n");
        return sb.toString();
    }

    // TODO: replace MultiTypeParameters, which disappeared in the newer JavaParser version.
    // private String doConvertMultiTypeParameter(String indent, MultiTypeParameter multiTypeParameter) {
    //     return utilFormatTypeList( multiTypeParameter.getTypes() );
    // }

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

    public static void main(String[] args ) throws ParseException, FileNotFoundException, IOException {
        ConvertJavaToIDL c= new ConvertJavaToIDL();
        c.setOnlyStatic(true);
        c.setUnittest(false);
       System.err.println("----");
       System.err.println(c.doConvert("{ int x= Math.pow(3,5); }"));
       System.err.println("----");
       System.err.println("----");
       System.err.println(c.doConvert("{ int x=0; if (x>0) { y=0; } }"));
       System.err.println("----");
       System.err.println("----");
       System.err.println(c.doConvert("x=3"));
       System.err.println("----");
       System.err.println("----");
       System.err.println(c.doConvert("Math.pow(3,c)"));
       System.err.println("----");
       System.err.println("----");
       System.err.println(c.doConvert("3*c"));
       System.err.println("----");
       System.err.println("----");
       System.err.println(c.doConvert("\"apple\".subString(3)"));
       System.err.println("----");

        //InputStream ins= new URL("https://raw.githubusercontent.com/jbfaden/JavaJythonConverter/main/src/java/test/ForDemo.java").openStream();
        //InputStream ins= new URL("https://raw.githubusercontent.com/jbfaden/JavaJythonConverter/main/src/java/test/FibExample.java").openStream();
        //InputStream ins= new URL("https://raw.githubusercontent.com/jbfaden/JavaJythonConverter/main/src/java/test/ArraySubset.java").openStream();
        InputStream ins= new URL("https://raw.githubusercontent.com/jbfaden/JavaJythonConverter/main/src/java/test/StringSplitDemo.java").openStream();
        //InputStream ins= new FileInputStream("/net/spot8/home/jbf/ct/hapi/git/uri-templates/UriTemplatesJava/src/org/hapiserver/URITemplate.java");
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
    private String javaNameToIdlName( String str ) {
        if ( str.equals("fh1") ) {
            System.err.println("here stop");
        }
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
                lastLower= false;
            } else if ( Character.isLowerCase(c) ) {
                if ( inUpperCaseWord ) { // "URITemplate" -> uri_template
                    result.insert( i-1, '_' );
                }
                inUpperCaseWord= false;
                lastLower= true;
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
            return "strtrim("+doConvert("",e)+",2)";
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
     * take index and length and return "2:5" type notation, (where in IDL the second number is inclusive)!
     * @param targetIdx
     * @param targetLen
     * @return
     */
    private String utilCreateIndexs(Expression targetIdx, Expression targetLen) {
        String targetIndex = doConvert("",targetIdx);
        String targetLength = doConvert("",targetLen);
        if ( targetIndex.equals("0") ) {
            return targetIndex + ":("+targetLength+"-1)"; // it's only one character, so don't bother removing the 0 in 0:
        } else {
            if ( targetIndex.equals(targetLength) &&
                    ( targetIdx instanceof FieldAccessExpr || targetIdx instanceof NameExpr ) ) {
                return targetIndex + ":2*"+targetLength+"-1"; // HUH?
            } else {
                return targetIndex + ":" + targetIndex + "+" + targetLength+"-1";
            }
        }
    }

    private String doConvertEnumDeclaration(String indent, EnumDeclaration enumDeclaration) {
        StringBuilder builder= new StringBuilder();
        if ( true ) {

            getCurrentScopeClasses().put( enumDeclaration.getName().asString(), enumDeclaration );

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
                            methodName= ((MethodDeclaration)n).getName().asString();
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
            return indent + javaNameToIdlName(s);
        } else if ( getCurrentScopeFields().containsKey(s) ) {
            FieldDeclaration ss= getCurrentScopeFields().get(s);
            boolean isStatic= ConversionUtils.isStaticField(ss);
            if ( isStatic ) {
                scope = javaNameToIdlName( theClassName );
                return indent + scope + (scope.length()==0 ? "" : "_") + javaNameToIdlName(s);
            } else {
                scope = "self";
                return indent + scope + (scope.length()==0 ? "" : ".") + javaNameToIdlName(s);
            }
        } else {
            return indent + javaNameToIdlName(s);
        }

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
        return exprType.equals(STRING_TYPE) || exprType.equals(Primitive.CHAR);
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
}
