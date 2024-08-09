
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
import japa.parser.ast.body.TypeDeclaration;
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
import japa.parser.ast.body.VariableDeclaratorId;
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
import japa.parser.ast.expr.MarkerAnnotationExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.stmt.CatchClause;
import japa.parser.ast.stmt.EmptyStmt;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.ForeachStmt;
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

    public static final String VERSION = "20240625a";
    
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
    private static final ReferenceType STRING_TYPE = ASTHelper.createReferenceType( "String", 0 );
    
    private static final AnnotationExpr DEPRECATED = new MarkerAnnotationExpr( new NameExpr("Deprecated") );
    
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
            CompilationUnit unit= japa.parser.JavaParser.parse(ins,"UTF-8");
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
                    sb.append( javaNameToIdlName(theClassName) ).append(".main([])\n");
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
            if ( mce.getName().equals("compareTo") 
                    && ((IntegerLiteralExpr)b.getRight()).toString().equals("0") ) {
                if ( null!=b.getOperator() ) switch (b.getOperator()) {
                    case greater:
                        return doConvert("",mce.getScope()) + " gt " + doConvert("",mce.getArgs().get(0));
                    case greaterEquals:
                        return doConvert("",mce.getScope()) + " ge " + doConvert("",mce.getArgs().get(0));
                    case less:
                        return doConvert("",mce.getScope()) + " lt " + doConvert("",mce.getArgs().get(0));
                    case lessEquals:
                        return doConvert("",mce.getScope()) + " le " + doConvert("",mce.getArgs().get(0));
                    case equals:
                        return doConvert("",mce.getScope()) + " eq " + doConvert("",mce.getArgs().get(0));
                    case notEquals:
                        return doConvert("",mce.getScope()) + " ne " + doConvert("",mce.getArgs().get(0));
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
            if ( rightType.equals(STRING_TYPE) 
                    && leftType instanceof PrimitiveType 
                    && !leftType.equals(ASTHelper.CHAR_TYPE) ) {
                left= "strtrim("+left+",2)";
            }
            if ( leftType.equals(STRING_TYPE) 
                    && rightType instanceof PrimitiveType 
                    && !rightType.equals(ASTHelper.CHAR_TYPE) ) {
                right= "strtrim("+right+",2)";
            }
        }
        
        if ( leftType!=null && !( b.getRight() instanceof NullLiteralExpr )
                && leftType.equals(ASTHelper.createReferenceType("String", 0)) && rightType==null ) {
            right= "strtrim("+right+",2)";
        }
        
        switch (op) {
            case plus:
                return left + " + " + right;
            case minus:
                return left + " - " + right;
            case divide:
                    return left + " / " + right;
            case times:
                return left + " * " + right;
            case greater:
                return left + " gt " + right;
            case less:
                return left + " lt " + right;
            case greaterEquals:
                return left + " ge " + right;                
            case lessEquals:
                return left + " le " + right;
            case and:
                return left + " and " + right;
            case or:
                return left + " or " + right;
            case equals:
                if ( b.getRight() instanceof NullLiteralExpr ) {
                    return left + " is " + right;
                } else {
                    return left + " eq " + right;
                }
            case notEquals:
                return left + " ne " + right;
            case remainder:
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
        ReferenceType STRING = ASTHelper.createReferenceType("String", 0);
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
                if ( ( leftType!=null && leftType.equals(STRING) ) ||
                        ( rightType!=null && rightType.equals(STRING) ) ) {
                    return STRING;
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
                    }
                } else if ( scopeType.toString().equals("StringBuilder") ) {
                    switch ( mce.getName() ) {
                        case "toString":
                            return ASTHelper.createReferenceType("String", 0);
                    }
                } else if ( scopeType.toString().equals("String") ) {
                    switch ( mce.getName() ) {
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
            Type contextType= localVariablesStack.peek().get(contextName); // allow local variables to override class variables.
            if ( contextType==null ) contextType= getCurrentScope().get(contextName);
            if ( Character.isUpperCase(contextName.charAt(0)) ) { // Yup, we're assuming that upper case refers to a class
                clasType= contextName;
            } else if ( stringMethods.contains(name) 
                    && ( contextType==null || contextType.equals(ASTHelper.createReferenceType("String", 0) )) ) {
                clasType= "String";
            } else if ( characterMethods.contains(name) 
                    && ( contextType==null || contextType.equals(ASTHelper.createReferenceType("String", 0) )) ) {
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

        // remove diamond typing (Map<String,String> -> Map)
        int i= clasType.indexOf("<"); 
        if ( i>=0 ) {
            clasType= clasType.substring(0,i);
        }
        
        if ( clasType.equals("StringBuilder") ) {
            if ( name.equals("append") ) {
                return indent + doConvert("",clas) + " = " + doConvert("",clas) + " + " + utilAssertStr(args.get(0)) ;
            } else if ( name.equals("toString") ) {
                return indent + doConvert("",clas);
            } else if ( name.equals("insert") ) {
                String n= doConvert("",clas);
                String i0= doConvert("",args.get(0));
                String ins= doConvert("",args.get(1));
                return indent + n + " = ''.join( ( " + n + "[0:"+ i0 + "], "+ins+", " + n + "["+i0+":] ) ) ; J2J expr -> assignment"; // expr becomes assignment, this will cause problems
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
                    return doConvert(indent,args.get(0)) + "/" + doConvert(indent,args.get(1));
                case "floorMod":
                    String x= doConvert(indent,args.get(0));
                    String y= doConvert(indent,args.get(1));
                    return "(" + x + " - " + y + " * ( "+x+"/"+y + ") )";
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
                    return indent + doConvert("",clas) + ".Remove(" + doConvert("",args.get(0)) + ")";
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
                    if ( guessType(args.get(0)).equals(ASTHelper.INT_TYPE) ) {
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
                        sb.append(indent).append("string(format=").append(doConvert("",args.get(0))).append(",");
                        sb.append( utilFormatExprList( args.subList(1, args.size() ) ) );
                        sb.append(")");
                        return sb.toString();
                    } else {
                        break;
                    }
                case "substring":
                    if ( args.size()==1 ) {
                        return "strmid("+doConvert(indent,clas)+","+ doConvert("",args.get(0))+")";
                    } else if ( args.size()==2 ) {
                        return "strmid("+doConvert(indent,clas)+","+ doConvert("",args.get(0)) +","+ doConvert("",args.get(1))+"-1)";
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
                    return "("+doConvert(indent,clas)+")"+".startswith("+ utilFormatExprList(args) +")";
                case "endsWith":
                    return doConvert(indent,clas)+".endswith("+ utilFormatExprList(args) +")";
                case "equalsIgnoreCase":
                    return "strcmp(" + doConvert(indent,clas)+","+ utilFormatExprList(args) +",/FOLD_CASE)";
                    
                case "trim":
                    return doConvert(indent,clas)+".strip()";
                case "replace":
                    String search = doConvert("",args.get(0));
                    String replac = doConvert("",args.get(1));
                    return doConvert(indent,clas)+".replace("+search+", "+replac+")";
                case "replaceAll":
                    search= doConvert("",args.get(0));
                    replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                    return indent + "StrJoin( StrSplit( "+ doConvert(indent,clas) + "," + search + ",/extract ), "+replac + ")";
                case "replaceFirst":
                    search= doConvert("",args.get(0));
                    replac= utilUnquoteReplacement( doConvert("",args.get(1)) );
                    additionalImports.put("import re\n",Boolean.TRUE);
                    return indent + "re.sub("+search+", "+replac+", "+doConvert("",clas)+",1)";
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
                    sb.append(doConvert("",args.get(1))).append(":").append(doConvert("",args.get(2))).append("-1");
                    sb.append("]");
                    return sb.toString();
                }
                case "equals": {
                    // if they are not objects, then Python == can be used.
                    StringBuilder sb= new StringBuilder();
                    Type t1= guessType(args.get(0)); // are both arrays containing primative objects (int,float,etc)
                    Type t2= guessType(args.get(1));
                    if ( t1!=null && t1.equals(ASTHelper.createReferenceType(ASTHelper.INT_TYPE,1)) 
                            && t2!=null && t2.equals(ASTHelper.createReferenceType(ASTHelper.INT_TYPE,1)) ) {
                        sb.append(indent).append(doConvert("",args.get(0))).append("==");
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
                return doConvert("",clas) + "!=None";
            } else if ( name.equals("find") ) {
                if ( clas instanceof MethodCallExpr ) {
                    return doConvert("",clas).replaceAll("match", "search") + "!=None";
                } else {
                    return doConvert("",clas) + "!=None  ; J2J: USE search not match above";
                }
            }
        }
        if ( clasType.equals("Thread") 
                && name.equals("sleep") ) {
            return indent + "wait, "+ "(" + doConvert("",args.get(0) ) + "/1000.)";
        }
        
        if ( unittest && clas==null ) {
            if ( name.equals("assertEquals") || name.equals("assertArrayEquals") ) {
                StringBuilder sb= new StringBuilder();
                sb.append(indent).append( "self.assertEqual(" );
                sb.append(doConvert("",args.get(0))).append(",").append(doConvert("",args.get(1)));
                sb.append(")");
                return sb.toString();
            }
        }
        
        
        if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("err") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArgs().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArgs().get(0) );
                sb.append(indent).append( "printf, -2, " ).append(s).append("");
            } else {
                String strss;
                if ( !isStringType( guessType(methodCallExpr.getArgs().get(0)) ) ) {
                    strss= "strtrim("+ doConvert( "", methodCallExpr.getArgs().get(0) ) + ",2)";
                } else {
                    strss= doConvert( "", methodCallExpr.getArgs().get(0) );
                }
                sb.append(indent).append( "printf, -2, " ).append( strss );
            }
            return sb.toString();
        } else if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("out") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArgs().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArgs().get(0) );
                sb.append(indent).append( "print," ).append( s );
            } else {
                sb.append(indent).append( "print," ).append( doConvert( "", methodCallExpr.getArgs().get(0) ) );
            }    
            return sb.toString();
        } else if ( name.equals("print") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("err") ) {
            StringBuilder sb= new StringBuilder();
            String s= doConvert( "", methodCallExpr.getArgs().get(0) );
            sb.append(indent).append( "printf, -2, " ).append(s);
            return sb.toString();
        } else if ( name.equals("print") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("out") ) {
            StringBuilder sb= new StringBuilder();

            if (  methodCallExpr.getArgs().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArgs().get(0) );
                sb.append(indent).append( "printf, -1, " ).append( s );
            } else {
                sb.append(indent).append( "printf, -1, " ).append( doConvert( "", methodCallExpr.getArgs().get(0) ) );
            }    
            return sb.toString();
        } else if ( name.equals("length") && args==null ) {
            return indent + "length("+ doConvert("",clas)+")";
        } else if ( name.equals("equals") && args.size()==1 ) {
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
        } else if ( name.equals("exit") && clasType.equals("System") ) {
            return indent + "exit, status="+ doConvert("",methodCallExpr.getArgs().get(0));

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
                        boolean isStatic= ModifierSet.isStatic(mm.getModifiers() );
                        if ( isStatic ) {
                            return indent + javaNameToIdlName( m.getName() ) + "." + javaNameToIdlName( name ) + "("+ utilFormatExprList(args) +")";
                        } else {
                            return indent + "self." + javaNameToIdlName( name ) + "("+ utilFormatExprList(args) +")";
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
                    if ( onlyStatic && clasName.equals(theClassName) )  {
                        return indent            + javaNameToIdlName( name ) + "("+ utilFormatExprList(args) +")";
                    } else {
                        return indent + clasName +"."+javaNameToIdlName( name )+ "("+ utilFormatExprList(args) +")";
                    }
                }
            }
        }
    }

    private String doConvertAssignExpr(String indent, AssignExpr assign ) {
        String target= doConvert( "", assign.getTarget() );
        Operator operator= assign.getOperator();
        
        switch (operator) {
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
            case assign:
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
        s= s.replaceAll("'","\\\\'");
        return "'" + s + "'";
    }

    private String doConvertBlockStmt(String indent,BlockStmt blockStmt) {
        
        pushScopeStack(true);
        
        StringBuilder result= new StringBuilder();
        List<Statement> statements= blockStmt.getStmts();
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
        if ( expressionStmt.getComment()!=null ) {
            sb.append( utilRewriteComments(indent, expressionStmt.getComment() ) );
        }
        sb.append( doConvert( indent, expressionStmt.getExpression() ) );
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
                return indent + "; J2J: "+variableDeclarationExpr.toString().trim();
            }
            if ( camelToSnake ) {
                String news= camelToSnakeAndRegister(s);
                s= news;
            }
            // TODO: we should guard against the many reserved words in IDL, like switch
            if ( v.getInit()!=null 
                    && ( v.getInit() instanceof ArrayInitializerExpr ) 
                    && ( variableDeclarationExpr.getType() instanceof PrimitiveType ) ) {
                Type t= ASTHelper.createReferenceType( ((PrimitiveType)variableDeclarationExpr.getType()), 1 );
                localVariablesStack.peek().put( s, t );
            } else {
                localVariablesStack.peek().put( s, variableDeclarationExpr.getType() );
            }
            if ( v.getInit()!=null ) {
                if ( v.getInit() instanceof ObjectCreationExpr && ((ObjectCreationExpr)v.getInit()).getAnonymousClassBody()!=null ) {
                    for ( BodyDeclaration bd: ((ObjectCreationExpr)v.getInit()).getAnonymousClassBody() ) {
                        b.append(doConvert( indent+"; J2J:", bd ) );
                    }
                }
                b.append( indent ).append(s).append(" = ").append(doConvert("",v.getInit()) );
            }
        }
        return b.toString();
    }

    private String specialConvertElifStmt( String indent, IfStmt ifStmt ) {
        StringBuilder b= new StringBuilder();
        b.append(indent).append("endif else if ");
        b.append( doConvert("", ifStmt.getCondition() ) );
        b.append(" then begin\n");
        b.append( doConvert(indent,ifStmt.getThenStmt() ) );
        if ( ifStmt.getElseStmt()!=null ) {
            if ( ifStmt.getElseStmt() instanceof IfStmt ) {
                b.append( specialConvertElifStmt( indent, (IfStmt)ifStmt.getElseStmt() ) );
                b.append(indent).append("endif\n");
            } else {
                b.append(indent).append("endif else begin\n");
                b.append( doConvert(indent,ifStmt.getElseStmt()) );
                b.append(indent).append("endelse\n");
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
                ((MethodCallExpr)ifStmt.getCondition()).getName().equals("isLoggable") ) {
            return indent + "; J2J: if "+ifStmt.getCondition() + " ... removed";
        }
        b.append(indent).append("if ");
        b.append( doConvert("", ifStmt.getCondition() ) );
        if ( ifStmt.getThenStmt() instanceof BlockStmt ) {
            b.append(" then begin\n");
            b.append( doConvert(indent,ifStmt.getThenStmt() ) );
            b.append( indent ) .append( "endif ");
        } else {
            b.append(" then ");
            b.append( doConvert("",ifStmt.getThenStmt() ) ).append("\n");
        }
        if ( ifStmt.getElseStmt()!=null ) {
            if ( ifStmt.getElseStmt() instanceof IfStmt ) {
                String ssss= specialConvertElifStmt( indent, (IfStmt)ifStmt.getElseStmt() ) ;
                int i= ssss.indexOf("endif ");
                b.append( ssss.substring(i+6) );
            } else {
                b.append("else");
                if ( ifStmt.getElseStmt() instanceof BlockStmt ) {
                    b.append(" begin\n");
                    b.append( doConvert(indent,ifStmt.getElseStmt()) );
                    b.append(indent).append( "endelse\n" );
                } else {
                    b.append(" begin ");
                    b.append( doConvert("",ifStmt.getElseStmt() ) ).append("\n");
                    b.append(" endelse\n ");
                }
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
        boolean initOkay= init1!=null;
        boolean compareOkay= compare!=null 
                && ( compare.getOperator()==BinaryExpr.Operator.less || compare.getOperator()==BinaryExpr.Operator.lessEquals 
                || compare.getOperator()==BinaryExpr.Operator.greater || compare.getOperator()==BinaryExpr.Operator.greaterEquals );
        String compareTo= doConvert("",compare.getRight());
        if ( compare.getOperator()==BinaryExpr.Operator.less ) {
            if ( compare.getRight() instanceof IntegerLiteralExpr ) {
                compareTo= String.valueOf(Integer.parseInt(((IntegerLiteralExpr)compare.getRight()).getValue())-1);
            } else {
                compareTo = compareTo + "-1";
            }
        } else if ( compare.getOperator()==BinaryExpr.Operator.greater ) {
            if ( compare.getRight() instanceof IntegerLiteralExpr ) {
                compareTo= String.valueOf(Integer.parseInt(((IntegerLiteralExpr)compare.getRight()).getValue())+1);
            } else {
                compareTo = compareTo + "+1";
            }
        }
        
        boolean updateOkay= update1!=null;
        
        boolean bodyOkay= false;
        if ( initOkay && compareOkay && updateOkay ) { // check that the loop variable isn't modified within loop
            bodyOkay= utilCheckNoVariableModification( forStmt.getBody(), v.getName() );
        }
        
        boolean usedWhile= false;
        if ( initOkay && compareOkay && updateOkay && bodyOkay ) {
            b.append(indent).append( "for " ).append( v.getName() ).append("=");
            b.append("");
            b.append( doConvert("",init1.getVars().get(0).getInit()) ).append(",");
            b.append( compareTo );
            if (  update1.getOperator()==UnaryExpr.Operator.posDecrement 
                    || update1.getOperator()==UnaryExpr.Operator.preDecrement ) {
                b.append(",-1");
            }
            b.append(" do begin\n");
        } else {        
            if ( init!=null ) {
                init.forEach((e) -> {
                    b.append(indent).append( doConvert( "", e ) ).append( "\n" );
                });
            }
            b.append( indent ).append("while ").append(doConvert( "", forStmt.getCompare() )).append(" do begin  ; J2J for loop\n");        
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

    private String doConvertForeachStmt(String indent, ForeachStmt foreachStmt) {
        StringBuilder b= new StringBuilder();
        if ( foreachStmt.getVariable().getVars().size()!=1 ) {
            throw new IllegalArgumentException("expected only one variable in foreach statement");
        }
        String variableName = foreachStmt.getVariable().getVars().get(0).getId().getName();
        Type variableType= foreachStmt.getVariable().getType();
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

    private String doConvertReturnStmt(String indent, ReturnStmt returnStmt) {
        if ( returnStmt.getExpr()==null ) {
            return indent + "return";
        } else {
            return indent + "return, " + doConvert("", returnStmt.getExpr());
        }
    }

    private String doConvertArrayCreationExpr(String indent, ArrayCreationExpr arrayCreationExpr) {
        if ( arrayCreationExpr.getInitializer()!=null ) {
            ArrayInitializerExpr ap= arrayCreationExpr.getInitializer();
            StringBuilder sb= new StringBuilder();
            return "[" + utilFormatExprList( ap.getValues() ) + "]";
        } else {
            String item;
            if ( arrayCreationExpr.getType().equals( ASTHelper.BYTE_TYPE ) ||
                    arrayCreationExpr.getType().equals( ASTHelper.SHORT_TYPE ) ||
                    arrayCreationExpr.getType().equals( ASTHelper.INT_TYPE ) ||
                    arrayCreationExpr.getType().equals( ASTHelper.LONG_TYPE ) ) {
                item="0";
            } else if ( arrayCreationExpr.getType().equals( ASTHelper.CHAR_TYPE )) {
                item= "''";
            } else {
                item= "None";
            }
            if ( arrayCreationExpr.getDimensions().size()>1 ) {
                throw new IllegalStateException("unable to handle multi-dimensional arrays");
            }
            return "replicate("+item+ ","+doConvert( "", arrayCreationExpr.getDimensions().get(0) ) + ")" ;
            
        }
    }

    private String doConvertFieldAccessExpr(String indent, FieldAccessExpr fieldAccessExpr) {
        String s= doConvert( "", fieldAccessExpr.getScope() );
        
        if ( this.getCurrentScopeClasses().containsKey(s) ) {
            if ( !this.theClassName.equals(s) ) {
                s= the_class_name + "." + s; 
            }
        }
        
        // test to see if this is an array and "length" of the array is accessed.
        if ( fieldAccessExpr.getField().equals("length") ) {
            String inContext= s;
            if ( inContext.startsWith("self.") ) {
                inContext= inContext.substring(5);
            }
            Type t= localVariablesStack.peek().get(inContext);
            if (t==null ) {
                t= getCurrentScope().get(inContext);
            }
            if ( t!=null && t instanceof ReferenceType && ((ReferenceType)t).getArrayCount()>0 ) { 
                return indent + "n_elements("+ s + ")";
            }
        }
        if ( onlyStatic && s.equals(classNameStack.peek()) ) {
            return fieldAccessExpr.getField();
        } else {
            if ( s.equals("Collections") ) {
                String f= fieldAccessExpr.getField();
                switch (f) {
                    case "EMPTY_MAP":
                        return indent + "DICTIONARY()";
                    case "EMPTY_SET":
                        return indent + "HASH()"; // Jython 2.2 does not have sets.
                    case "EMPTY_LIST":
                        return indent + "LIST()";
                    default:
                        break;
                }
            }
            return indent + s + "." + fieldAccessExpr.getField();
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
            case preIncrement: {
                additionalClasses.put("; J2J: increment used at line "+unaryExpr.getBeginLine()+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpr());
                return indent + n + " = " + n + " + 1";
            }
            case preDecrement: {
                additionalClasses.put("; J2J: decrement used at line "+unaryExpr.getBeginLine()+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpr());
                return indent + n + " = " + n + " - 1";
            }
            case posIncrement: {
                additionalClasses.put("; J2J: increment used at line "+unaryExpr.getBeginLine()+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpr());
                return indent + n + " = " + n + " + 1";
            }
            case posDecrement: {
                additionalClasses.put("; J2J: decrement used at line "+unaryExpr.getBeginLine()+", which needs human study.\n",true );
                String n= doConvert("",unaryExpr.getExpr());
                return indent + n + " = " + n + " - 1";
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

    private String doConvertInitializerDeclaration(String indent, InitializerDeclaration initializerDeclaration) {
        return doConvert(indent,initializerDeclaration.getBlock());
    }
    
    private String doConvertClassOrInterfaceDeclaration(String indent, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        StringBuilder sb= new StringBuilder();
        
        String name = classOrInterfaceDeclaration.getName();
        if ( classNameStack.isEmpty() ) {
            theClassName= name;
            if ( camelToSnake ) {
                the_class_name= camelToSnake(name);
            } else {
                the_class_name= name;
            }
        }
        classNameStack.push(name);
        
        getCurrentScopeClasses().put( name, classOrInterfaceDeclaration );
        
        pushScopeStack(false);
        getCurrentScope().put( "this", new ClassOrInterfaceType(name) );
        
        if ( onlyStatic ) {
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                sb.append( doConvert(indent,n) ).append("\n");
                sb.append( "\n" );
            });
        } else {
            
            if ( unittest ) {
                sb.append( "\n# cheesy unittest temporary\n");
                sb.append( "def assertEquals(a,b):\n"
                        + "    if ( not a==b ): stop, 'a!=b'\n");
                sb.append( "def assertArrayEquals(a,b):\n");
                sb.append( "    if ( len(a)==len(b) ): \n");
                sb.append( "        for i in xrange(len(a)): \n");
                sb.append( "            if ( a[i]!=b[i] ): stop, 'a[%d]!=b[%d]'%(i,i))\n" );
                sb.append( "def fail(msg):\n"
                        + "    print(msg)\n"
                        + "    stop, 'fail: '+msg\n");
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
            
            if ( classOrInterfaceDeclaration.getExtends()!=null && classOrInterfaceDeclaration.getExtends().size()==1 ) { 
                String extendName= doConvert( "", classOrInterfaceDeclaration.getExtends().get(0) );
                sb.append( indent ).append("class " ).append( pythonName ).append("(" ).append(extendName).append(")").append(":\n");
            } else if ( classOrInterfaceDeclaration.getImplements()!=null ) { 
                List<ClassOrInterfaceType> impls= classOrInterfaceDeclaration.getImplements();
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
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                if ( n instanceof MethodDeclaration ) {
                    classMethods.put( ((MethodDeclaration) n).getName(), classOrInterfaceDeclaration );
                    getCurrentScopeMethods().put(((MethodDeclaration) n).getName(),(MethodDeclaration)n );
                } else if ( n instanceof ClassOrInterfaceDeclaration ) {
                    ClassOrInterfaceDeclaration coid= (ClassOrInterfaceDeclaration)n;
                    getCurrentScope().put( coid.getName(), new ClassOrInterfaceType(coid.getName()) );
                }
            });
            
            // object data goes into structure with the same name
            StringBuilder structureDefinition= new StringBuilder();
            
            // check for unique names
            Map<String,Node> nn= new HashMap<>();
            for ( Node n: classOrInterfaceDeclaration.getChildrenNodes() ) {
                if ( n instanceof ClassOrInterfaceType ) {
                    String name1= ((ClassOrInterfaceType)n).getName();
                    if ( nn.containsKey(name1) ) {
                        sb.append(indent).append("; J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                    }
                    nn.put( name1, n );
                } else if ( n instanceof FieldDeclaration ) {
                    
                    for ( VariableDeclarator vd : ((FieldDeclaration)n).getVariables() ) {
                        String name1= vd.getId().getName();
                        if ( nn.containsKey(name1) ) {
                            sb.append(indent).append("; J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                        }
                        getCurrentScope().put( name1, ((FieldDeclaration)n).getType() ); //TODO: Does Python and JavaScript have this?
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
                            sb.append(indent).append("; J2J: Name is used twice in class: ")
                                .append(pythonName).append(" ").append(name1).append("\n");
                    }
                    nn.put( name1, n );
                } else {
                    System.err.println("Not supported: "+n);
                }
            }
            
            StringBuilder commons= new StringBuilder();
            StringBuilder commonsInit= new StringBuilder(); // the part which will go in the common block
            
            ConstructorDeclaration constructor= null;
            
            for ( Node n : classOrInterfaceDeclaration.getChildrenNodes() ) {
                if ( n instanceof FieldDeclaration ) {
                    boolean isStatic= ModifierSet.isStatic(((FieldDeclaration)n).getModifiers() );
                    for ( VariableDeclarator vd : ((FieldDeclaration)n).getVariables() ) {
                        String vname= vd.getId().getName();
                        
                        getCurrentScopeFields().put( vname,(FieldDeclaration)n);
                        
                        if ( isStatic ) {
                            commons.append(", ").append(name).append("_").append(vname);
                            commonForStaticVariables= "common "+the_class_name;
                            if ( vd.getInit()!=null ) {
                                String v= doConvert("",vd.getInit());
                                commonsInit.append(indent).append(s4).append(the_class_name).append("_").append(vname).append("=").append(v).append("\n");
                            }
                        } else {
                            if ( vd.getInit()!=null ) {
                                structureDefinition.append(",").append(vd.getId().getName()).append(":").append(doConvert("",vd.getInit()));
                            } else {
                                structureDefinition.append(",").append(vd.getId().getName()).append(":ptr_new()");
                            }
                        }
                    }
                }                
                
            }
            
            if ( commonForStaticVariables.length()>0 ) {
                commonForStaticVariables= commonForStaticVariables + commons;
            }
            
            for ( Node n : classOrInterfaceDeclaration.getChildrenNodes() ) {
                sb.append("\n");
                if ( n instanceof ClassOrInterfaceType ) {
                    // skip this strange node
                    sb.append("; J2J: inner class skipped: ").append(((ClassOrInterfaceType)n).getName()).append("\n");
                } else if ( n instanceof EmptyMemberDeclaration ) {
                    // skip this strange node
                    sb.append("; J2J: empty member skipped: ").append(((EmptyMemberDeclaration)n));
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
                    String ss= doConvert( indent, constructor.getBlock() );
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
                sb.append("test = ").append(classOrInterfaceDeclaration.getName()).append("()\n");
                for ( Node n : classOrInterfaceDeclaration.getChildrenNodes() ) {
                    if ( n instanceof MethodDeclaration 
                            && ((MethodDeclaration)n).getName().startsWith("test") 
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
        String comments= utilRewriteComments( indent, methodDeclaration.getComment() );
        sb.append( comments );
        
        String methodName= methodDeclaration.getName();
        String idlName;
        if ( camelToSnake ) {
            idlName= camelToSnakeAndRegister(methodName);
        } else {
            idlName= methodName;
        }

        String classNameQualifier= onlyStatic ? "" : the_class_name + "::";
        
        if ( methodDeclaration.getType() instanceof japa.parser.ast.type.VoidType ) {
            sb.append( indent ).append( "pro " ).append(classNameQualifier).append( idlName );
        } else {
            sb.append( indent ).append( "function " ).append(classNameQualifier).append( idlName );
        }
        
        boolean comma; 
        
        comma = true;

        pushScopeStack(false);

        if ( methodDeclaration.getParameters()!=null ) {
            for ( Parameter p: methodDeclaration.getParameters() ) { 
                String name= p.getId().getName();
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
        
        if ( methodDeclaration.getBody()!=null ) {
            if ( isStatic && !onlyStatic ) {
                sb.append(indent).append(s4).append("compile_opt idl2, static\n");
                if ( this.commonForStaticVariables.length()>0 ) {
                    sb.append(indent).append(s4).append(this.commonForStaticVariables).append("\n");
                }
            }
            sb.append( doConvert( indent, methodDeclaration.getBody() ) );  
        } else {
            sb.append(indent).append(s4).append("pass");  
        }
        popScopeStack();
        
        sb.append("end");
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
                VariableDeclaratorId id= v.getId();
                String name= id.getName();
                String pythonName;
                if ( camelToSnake ) {
                    pythonName= camelToSnakeAndRegister(name);
                } else {
                    pythonName= name;
                }
                
                if ( v.getInit()!=null && v.getInit().toString().startsWith("Logger.getLogger") ) {
                    getCurrentScope().put( name, ASTHelper.createReferenceType("Logger", 0) );
                    //addLogger();
                    sb.append( indent ).append("; J2J: ").append(fieldDeclaration.toString());
                    continue;
                }
                
                getCurrentScope().put( name,fieldDeclaration.getType() );
                getCurrentScopeFields().put( name,fieldDeclaration);

                if ( v.getInit()==null ) {
                    String implicitDeclaration = utilImplicitDeclaration( fieldDeclaration.getType() );
                    if ( implicitDeclaration!=null ) {
                        sb.append( indent ).append( pythonName ).append(" = ").append( implicitDeclaration ).append("\n");
                    } else {
                        sb.append( indent ).append( pythonName ).append(" = ").append( "None  ; J2J added" ).append("\n");
                    }
                } else if ( v.getInit() instanceof ConditionalExpr ) {
                    ConditionalExpr ce= (ConditionalExpr)v.getInit();
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
        Expression msg= throwStmt.getExpr();
        if ( throwStmt.getExpr() instanceof ObjectCreationExpr ) {
            List<Expression> args= ((ObjectCreationExpr)throwStmt.getExpr()).getArgs();
            if ( args.size()==1 ) {
                msg= args.get(0);
            }
        }
        return indent + "stop, "+ doConvert("",msg);
    }

    private String doConvertWhileStmt(String indent, WhileStmt whileStmt) {
        StringBuilder sb= new StringBuilder(indent);
        sb.append( "WHILE ");
        sb.append( doConvert( "", whileStmt.getCondition() ) );
        sb.append( " DO BEGIN\n" );
        if ( whileStmt.getBody() instanceof ExpressionStmt ) {
            sb.append( doConvert( indent+s4, whileStmt.getBody() ) );
        } else {
            sb.append( doConvert( indent, whileStmt.getBody() ) );
        }
        sb.append(indent).append("ENDWHILE\n");
        return sb.toString();
    }

    private String doConvertArrayInitializerExpr(String indent, ArrayInitializerExpr arrayInitializerExpr) {
        return indent + "[" + utilFormatExprList( arrayInitializerExpr.getValues() ) + "]";
    }

    
    private String doConvertSwitchStmt(String indent, SwitchStmt switchStmt) {
         StringBuilder b= new StringBuilder();
        b.append( indent ).append( "SWITCH " );
        b.append( doConvert("",switchStmt.getSelector()) ).append(" OF\n");
        String nextIndent= indent + s4;
        String nextNextIndent = nextIndent + s4;
        for ( SwitchEntryStmt ses: switchStmt.getEntries() ) {
            Expression label= ses.getLabel();
            String slabel;
            if ( label==null ) {
                b.append( nextIndent ).append( "ELSE: BEGIN\n");
            } else {
                slabel= doConvert("",ses.getLabel());
                b.append( nextIndent ).append( "").append( slabel ).append(": BEGIN\n");
            }
            
            if ( ses.getStmts()!=null ) {
                for ( Statement s : ses.getStmts() ) {
                    b.append( doConvert( nextNextIndent,s) );
                    b.append( "\n" );
                }
            }
            b.append( nextIndent ).append("END\n");
        }
        b.append( indent ).append("ENDSWITCH\n");
        return b.toString();
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
                return indent + "Exception()";
            } else {
                return indent + "Exception("+ doConvert("",objectCreationExpr.getArgs().get(0))+")";
            }
        } else {
            String qualifiedName= utilQualifyClassName(objectCreationExpr.getType());
            if ( qualifiedName!=null ) {
                return indent + qualifiedName + "("+ utilFormatExprList(objectCreationExpr.getArgs())+ ")";
            } else {
                if ( objectCreationExpr.getAnonymousClassBody()!=null ) {
                    StringBuilder sb= new StringBuilder();
                    String body= doConvert( indent, objectCreationExpr.getAnonymousClassBody().get(0) );
                    sb.append(indent).append(objectCreationExpr.getType()).append("(").append(utilFormatExprList(objectCreationExpr.getArgs())).append(")"); 
                    sb.append("*** ; J2J: This is extended in an anonymous inner class ***");
                    return sb.toString();
                } else {
                    if ( objectCreationExpr.getType().getName().equals("HashMap") ) { 
                        return indent + "DICTIONARY()";
                    } else if ( objectCreationExpr.getType().getName().equals("ArrayList") ) { 
                        return indent + "LIST()";
                    } else if ( objectCreationExpr.getType().getName().equals("HashSet") ) {
                        return indent + "HASH()"; 
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
                                    return indent + "strjoin( "+ doConvert("",e) +")";
                                } else if ( t!=null && t.equals(ASTHelper.createReferenceType("StringBuilder",0) ) ) {
                                    return  doConvert("",e); // these are just strings.
                                }
                                System.err.println("here "+t);
                            }
                        }
                        return indent + objectCreationExpr.getType() + "("+ utilFormatExprList(objectCreationExpr.getArgs())+ ")";
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
        if ( constructorDeclaration.getParameters()!=null ) {
            for ( Parameter p: constructorDeclaration.getParameters() ) { 
                String name= p.getId().getName();
                localVariablesStack.peek().put( name, p.getType() );
            }
        }
        sb.append( doConvert(indent,constructorDeclaration.getBlock()) );
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
                if ( isIntegerType(guessType(castExpr.getExpr())) ) {
                    type = "chr";
                } else {
                    return "string(byte("+doConvert("", castExpr.getExpr() ) + "))";
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
        return type + "(" + doConvert("", castExpr.getExpr() ) + ")";
    }

    private String doConvertTryStmt(String indent, TryStmt tryStmt) {
        StringBuilder sb= new StringBuilder();
        sb.append( indent ).append( "try:\n");
        sb.append( doConvert( indent, tryStmt.getTryBlock() ) );
        for ( CatchClause cc: tryStmt.getCatchs() ) {
            String id= doConvert( "",cc.getExcept().getId() );
            sb.append(indent).append("except ").append(doConvert( "",cc.getExcept() )).append( ", ").append(id).append(":\n");
            sb.append( doConvert( indent, cc.getCatchBlock() ) );
        }
        if ( tryStmt.getFinallyBlock()!=null ) {
            sb.append( indent ).append( "finally:\n");
            sb.append( doConvert( indent, tryStmt.getFinallyBlock() ) );
        }
        return sb.toString();
    }

    private String doConvertMultiTypeParameter(String indent, MultiTypeParameter multiTypeParameter) {
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
                return "***J2J" + referenceType.toString() +"***J2J";
        }
        
    }

    public static void main(String[] args ) throws ParseException, FileNotFoundException, IOException {
        ConvertJavaToIDL c= new ConvertJavaToIDL();
        c.setOnlyStatic(true);
        c.setUnittest(false);
//        System.err.println("----");
//        System.err.println(c.doConvert("{ int x= Math.pow(3,5); }"));
//        System.err.println("----");
//        System.err.println("----");
//        System.err.println(c.doConvert("{ int x=0; if (x>0) { y=0; } }"));
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
            
            getCurrentScopeClasses().put( enumDeclaration.getName(), enumDeclaration );
            
            builder.append(indent).append("class ").append(enumDeclaration.getName()).append(":\n");
            List<EnumConstantDeclaration> ll = enumDeclaration.getEntries();
            
            for ( Node n: enumDeclaration.getChildrenNodes() ) {
                if ( n instanceof ConstructorDeclaration ) {
                    String params= utilFormatParameterList( ((ConstructorDeclaration)n).getParameters() );
                    builder.append(indent).append(s4 + "def compare( self, o1, o2 ):\n");
                    builder.append(indent).append(s4 + "    raise Exception('Implement me')\n");
                }
            }

            for ( EnumConstantDeclaration l : ll ) {
                String args = utilFormatExprList(l.getArgs()); 
                args= "";// we drop the args
                //TODO find anonymous extension  l.getArgs().get(0).getChildrenNodes()
                builder.append(indent).append(enumDeclaration.getName()).append(".").append(l.getName()).append(" = ")
                        .append(enumDeclaration.getName()).append("(").append(args).append(")") .append("\n");
                String methodName=null;
                if ( l.getArgs().get(0).getChildrenNodes()!=null ) {
                    for ( Node n: l.getArgs().get(0).getChildrenNodes() ) {
                        if ( n instanceof MethodDeclaration ) {
                            methodName= ((MethodDeclaration)n).getName();
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
        String s= nameExpr.getName();
        String scope;
        if ( localVariablesStack.peek().containsKey(s) ) {
            return indent + javaNameToIdlName(s);
        } else if ( getCurrentScopeFields().containsKey(s) ) {
            FieldDeclaration ss= getCurrentScopeFields().get(s);
            boolean isStatic= ModifierSet.isStatic( ss.getModifiers() );
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
    
    private boolean utilCheckNoVariableModification(BlockStmt body, String name ) {
        for ( Statement s: body.getStmts() ) {
            if ( !utilCheckNoVariableModification( s, name ) ) {
                return false;
            }
        }
        return true;
    }

    private boolean utilCheckNoVariableModification(Statement body, String name ) {
        for ( Node n: body.getChildrenNodes() ) {
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
    
}
