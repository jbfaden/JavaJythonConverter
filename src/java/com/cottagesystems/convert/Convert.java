
package com.cottagesystems.convert;

import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.Node;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
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
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
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
import japa.parser.ast.type.ReferenceType;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for converting Java to Jython using an AST.
 * @author jbf
 */
public class Convert {
    
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

    /*** internal parsing state ***/
    
    /**
     * record the name of the class (e.g. TimeUtil) so that internal references can be corrected.
     */
    private String staticClassName;
    
    /**
     * record the method names, since Python will need to refer to "self" to call methods but Java does not.
     */
    private Map<String,ClassOrInterfaceDeclaration> classMethods = new HashMap<>();
    
    /**
     * return imported class names.
     */
    private Map<String,Object> importedClasses = new HashMap<>();
     
    /**
     * return imported methods, from star imports.
     */
    private Map<String,Object> importedMethods = new HashMap<>();
    
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
     * wrap the methods in a dummy class to see if it compiles
     * @param javasrc
     * @return
     * @throws ParseException 
     */
    public String utilMakeClass( String javasrc ) throws ParseException {
        String modSrc= "class Foo {\n"+javasrc + "}\n";
        return modSrc;
    }
    
    public String doConvert( String javasrc ) throws ParseException {
        Exception throwMe= null;
        try {
            ByteArrayInputStream ins= new ByteArrayInputStream( javasrc.getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= japa.parser.JavaParser.parse(ins,"UTF-8");
            return doConvert( "", unit );
        } catch ( ParseException ex ) {
            throwMe= ex;
        }
        
        try {
            ByteArrayInputStream ins= new ByteArrayInputStream( utilMakeClass(javasrc).getBytes(Charset.forName("UTF-8")) );
            CompilationUnit unit= japa.parser.JavaParser.parse(ins,"UTF-8");
            return doConvert( "", unit );
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
                
                return bb.toString();
            
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
        BinaryExpr.Operator op= b.getOperator();
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
    
    private String doConvertMethodCallExpr(String indent,MethodCallExpr methodCallExpr) {
        Expression clas= methodCallExpr.getScope();
        String name= methodCallExpr.getName();
        List<Expression> args= methodCallExpr.getArgs();
        if ( name.equals("pow") && clas instanceof NameExpr && ((NameExpr)clas).getName().equals("Math") ) {
            if ( args.get(1) instanceof IntegerLiteralExpr ) {
                return doConvert(indent,args.get(0)) + "**"+ doConvert(indent,args.get(1));
            } else {
                return doConvert(indent,args.get(0)) + "**("+ doConvert(indent,args.get(1))+")";
            }
            
        } else if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("err") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArgs().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArgs().get(0) );
                sb.append(indent).append( "sys.stderr.write(" ).append( s.substring(0,s.length()-1) ).append( "\\n')" );
            } else {
                sb.append(indent).append( "sys.stderr.write(" ).append( doConvert( "", methodCallExpr.getArgs().get(0) ) ).append( "+'\\n')" );
            }
            return sb.toString();
        } else if ( name.equals("println") && clas instanceof FieldAccessExpr &&
                ((FieldAccessExpr)clas).getField().equals("out") ) {
            StringBuilder sb= new StringBuilder();
            if (  methodCallExpr.getArgs().get(0) instanceof StringLiteralExpr ) {
                String s= doConvert( "", methodCallExpr.getArgs().get(0) );
                sb.append(indent).append( "print(" ).append( s.substring(0,s.length()-1) ).append( "\\n')" );
            } else {
                sb.append(indent).append( "print(" ).append( doConvert( "", methodCallExpr.getArgs().get(0) ) ).append( "+'\\n')" );
            }    
            return sb.toString();
        } else if ( name.equals("format") && clas instanceof NameExpr && ((NameExpr)clas).getName().equals("String") ) {
            StringBuilder sb= new StringBuilder();
            sb.append(indent).append(args.get(0)).append(" % (");
            sb.append( utilFormatExprList( args.subList(1, args.size() ) ) );
            sb.append(" )");
            return sb.toString();
        } else if ( name.equals("length") && args==null ) {
            return indent + "len("+ doConvert("",clas)+")";
        } else if ( name.equals("substring") ) {
            if ( args.size()==2 ) {
                return doConvert(indent,clas)+"["+ doConvert("",args.get(0)) + ":"+ doConvert("",args.get(1)) +"]";
            } else {
                return doConvert(indent,clas)+"["+ doConvert("",args.get(0)) +":]";
            }
        } else if ( name.equals("charAt") ) {
            return "ord(" + doConvert(indent,clas)+"["+ doConvert("",args.get(0)) +"])";
        } else if ( name.equals("startsWith") ) {
            return doConvert(indent,clas)+".startswith("+ utilFormatExprList(args) +")";
        } else if ( name.equals("endsWith") ) {
            return doConvert(indent,clas)+".endswith("+ utilFormatExprList(args) +")";
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
                if ( onlyStatic && clasName.equals(staticClassName) )  {
                    return indent            + name + "("+ utilFormatExprList(args) +")";
                } else {
                    return indent + clasName +"."+name + "("+ utilFormatExprList(args) +")";
                }
            }
        }
    }

    private String doAssignExpr(String indent, AssignExpr assign ) {
        return indent + doConvert( "", assign.getTarget() ) + " = " + doConvert( "", assign.getValue() );
    }
    
    private String doConvert( String indent, Node n ) {
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
                if ( nameMapForward.containsKey(s) ) {
                    return indent + nameMapForward.get(s);
                } else {
                    return indent + s;
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
                return doConvertBlockStmt(s4+indent,(BlockStmt)n);
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
            case "InitializerDeclaration":
                return doConvertInitializerDeclaration(indent,(InitializerDeclaration)n);
            case "ObjectCreationExpr":
                return doConvertObjectCreationExpr(indent,(ObjectCreationExpr)n);
            case "ClassOrInterfaceType":
                return doConvertClassOrInterfaceType(indent,(ClassOrInterfaceType)n);
            default:
                return indent + "*** "+simpleName + "*** " + n.toString() + "*** end "+simpleName + "****";
        }
    }
    

    private String doConvertStringLiteralExpr(String indent,StringLiteralExpr stringLiteralExpr) {
        return "'" + stringLiteralExpr.getValue() + "'";
    }

    private String doConvertBlockStmt(String indent,BlockStmt blockStmt) {
        StringBuilder result= new StringBuilder();
        List<Statement> statements= blockStmt.getStmts();
        if ( statements==null ) {
            return indent + "pass\n";
        }
        for ( Statement s: statements ) {
            String aline= doConvert(indent,s);
            if ( aline.trim().length()==0 ) continue;
            result.append(doConvert(indent,s));
            result.append("\n");
        }
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
                    b.append( indent ).append("if ").append(cc.getCondition()).append(":\n");
                    b.append(s4).append(indent).append( s );
                    b.append(" = ").append(doConvert("",cc.getThenExpr() )).append("\n");
                    b.append( indent ).append("else:\n" );
                    b.append(s4).append(indent).append( s );
                    b.append(" = ").append(doConvert("",cc.getElseExpr() )).append("\n");
                } else {
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
        b.append( doConvert( indent, forStmt.getBody() ) );
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
        StringBuilder sb= new StringBuilder();
        
        List<Node> nodes= compilationUnit.getChildrenNodes();
        for ( int i=0; i<nodes.size(); i++ ) {
            sb.append( doConvert( "", nodes.get(i) ) ).append("\n");
        }
        
        //for ( Comment c: compilationUnit.getComments() ) {
        //    sb.append("# ").append(c.getContent()).append("\n");
        //}
        
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
        String s= doConvert( indent, fieldAccessExpr.getScope() );
        if ( onlyStatic && s.equals(staticClassName) ) {
            return fieldAccessExpr.getField();
        } else {
            return s + "." + fieldAccessExpr.getField();
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
        if ( onlyStatic ) {
            staticClassName= classOrInterfaceDeclaration.getName();
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                sb.append( doConvert(indent,n) ).append("\n");
            });
        } else {
            String comments= utilRewriteComments(indent, classOrInterfaceDeclaration.getComment() );
            sb.append( "\n" );
            sb.append( comments );
            
            if ( classOrInterfaceDeclaration.getExtends()!=null && classOrInterfaceDeclaration.getExtends().size()==1 ) {  //TODO: multiple
                String extendName= doConvert( "", classOrInterfaceDeclaration.getExtends().get(0) );
                sb.append( indent ).append("class " ).append( classOrInterfaceDeclaration.getName() ).append("(" ).append(extendName).append(")").append(":\n");
            } else {
                sb.append( indent ).append("class " ).append( classOrInterfaceDeclaration.getName() ).append(":\n");
            }

            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                if ( n instanceof MethodDeclaration ) {
                    classMethods.put( ((MethodDeclaration) n).getName(), classOrInterfaceDeclaration );
                }
            });
            
            classOrInterfaceDeclaration.getChildrenNodes().forEach((n) -> {
                if ( n instanceof ClassOrInterfaceType ) {
                    // skip this strange node
                } else {
                    sb.append( doConvert( s4+indent, n ) ).append("\n");
                }
            });
        }
        return sb.toString();
    }

    private String doConvertMethodDeclaration(String indent, MethodDeclaration methodDeclaration) {
        boolean isStatic= ModifierSet.isStatic(methodDeclaration.getModifiers() );
        
        if ( onlyStatic && !isStatic ) {
            return "";
        }
        
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
        if ( methodDeclaration.getParameters()!=null ) {
            for ( Parameter p: methodDeclaration.getParameters() ) { 
                if ( comma ) {
                    sb.append(", ");
                } else {
                    comma = true;
                }
                sb.append( p.getId().getName() );
            }
        }
        sb.append( "):\n" );
        
        sb.append( doConvert( indent, methodDeclaration.getBody() ) );
        
        if ( pythonTarget==PythonTarget.jython_2_2 && isStatic && !onlyStatic ) {
            sb.append(indent).append(methodDeclaration.getName()).append(" = staticmethod(").append(methodDeclaration.getName()).append(")");
            sb.append(indent).append("\n");
        }
        return sb.toString();
    }

    Map<String,Type> scope= new HashMap<>();
    Map<String,String> nameMapForward= new HashMap<>();
    Map<String,String> nameMapReverse= new HashMap<>();
    
    /**
     * return the current scope, which includes local variables.  Presently this just uses one scope,
     * but this should check for code blocks, etc.
     * @return 
     */
    private Map<String,Type> getCurrentScope() {
        return scope;
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
                Map<String,Type> currentScope= getCurrentScope();
                currentScope.put( v.getId().getName(),fieldDeclaration.getType() );
                if ( v.getInit()==null ) {
                    
                } else if ( v.getInit() instanceof ConditionalExpr ) {
                    ConditionalExpr ce= (ConditionalExpr)v.getInit();
                    sb.append( indent ).append("if ").append(doConvert( "",ce.getCondition() )).append(":\n");
                    sb.append(s4).append(indent).append( v.getId()).append("=").append( doConvert( "",ce.getThenExpr() ) ).append("\n");
                    sb.append( indent ).append( "else:\n");
                    sb.append(s4).append(indent).append( v.getId()).append("=").append( doConvert( "",ce.getElseExpr() ) ).append("\n");
                    
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
        for ( int ises = 0; ises<nses; ises++ ) {
            SwitchEntryStmt ses = switchStmt.getEntries().get(ises);
            if ( iff ) {
                sb.append(indent).append("if ").append(selector).append("==").append(ses.getLabel()).append(":\n");
                iff=false;
            } else {
                if ( ses.getLabel()!=null ) {
                    sb.append(indent).append("elif ").append(selector).append("==").append(ses.getLabel()).append(":\n");
                } else {
                    sb.append(indent).append("else:\n");
                }   
            }
            
            if ( ses.getLabel()==null && ises!=(nses-1) ) {
                throw new IllegalArgumentException("default must be last of switch statement");
            }
            List<Statement> statements= ses.getStmts();
            if ( ses.getLabel()!=null && !( ( statements.get(statements.size()-1) instanceof BreakStmt ) ||
                    ( statements.get(statements.size()-1) instanceof ReturnStmt ) ||
                    ( statements.get(statements.size()-1) instanceof ThrowStmt ) ) ) {
                sb.append(s4).append(indent).append("### Switch Fall Through Not Implemented ###");
                for ( Statement s: statements.subList(0,statements.size()-1) ) {
                    sb.append("#").append(doConvert(s4+indent, s )).append("\n");
                }
            } else {
                if ( statements.get(statements.size()-1) instanceof BreakStmt ) {
                    for ( Statement s: statements.subList(0,statements.size()-1) ) {
                        sb.append(doConvert(s4+indent, s )).append("\n");
                    }
                } else {
                    for ( Statement s: statements ) {
                        sb.append(doConvert(s4+indent, s )).append("\n");
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

    private String doConvertObjectCreationExpr(String indent, ObjectCreationExpr objectCreationExpr) {
        return indent + objectCreationExpr.getType() + "("+ utilFormatExprList(objectCreationExpr.getArgs())+ ")";
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
    
}
