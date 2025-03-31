package com.cottagesystems.convert;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseException;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.Type;


import java.util.List;
import java.util.Optional;

// public only for testing. This should not be used by outside packages.
public final class ConversionUtils {

    private static final Modifier STATIC_MODIFIER = Modifier.staticModifier();

    public static boolean isStaticField(FieldDeclaration field) {
        if (field == null) {
            return false;
        }
        return field.getModifiers().contains(STATIC_MODIFIER);
    }

    // TODO: avoid duplication between the isStatic methods.
    public static boolean isStaticMethod(MethodDeclaration method) {
        if (method == null) {
            return false;
        }
        return method.getModifiers().contains(STATIC_MODIFIER);
    }

    public static Type getFieldType(FieldDeclaration field) {
        List<VariableDeclarator> variables = field.getVariables();
        if (variables.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one variable for " + field.toString());
        }
        return variables.get(0).getType();
    }

    public static <T> T extractParseResult(ParseResult<T> result) throws ParseException {
        /**
         * Extracts the underlying result if successful, otherwise raising an exception.
         *
         * @param result Result to be examined.
         * @return The underlying parsed out data type.
         * @throws ParseException with an informative message about what went wrong.
         */
        if (result.isSuccessful()) {
            return result.getResult().get();
        }
        // TODO: switch to using ParseProblemException, which JavaParser uses now.
        throw new ParseException(new ParseProblemException(result.getProblems()).toString());
    }

    /**
     * Gets the first line represented by the node.
     *
     * @param node Node to examine.
     * @return The line number or a negative number if unavailable.
     */
    public static int getBeginLine(Node node) {
        Optional<Range> range = node.getRange();
        if (!range.isPresent()) {
            return -1;
        }
        return range.get().begin.line;
    }

    /**
     * Gets the last line represented by the node.
     *
     * @param node Node to examine.
     * @return The line number or a negative number if unavailable.
     */
    public static int getEndLine(Node node) {
        Optional<Range> range = node.getRange();
        if (!range.isPresent()) {
            return -1;
        }
        return range.get().end.line;
    }

    /**
     * Finds the type of a binary expression given the types of its operands.
     * @param leftType
     * @param rightType
     * @return null or the resolved type.
     */
    public static Type findBinaryExprType(Type leftType, Type rightType) {
        if ( leftType == null || rightType == null ) {
            return null;
        }
        if ( leftType.equals(rightType) ) {
            return leftType;
        }
        if (!leftType.isPrimitiveType() || !rightType.isPrimitiveType()) {
            return null;
        }
        Primitive leftPrimitive = leftType.asPrimitiveType().getType();
        Primitive rightPrimitive = rightType.asPrimitiveType().getType();
        // TODO: What if left and right are different types of floating point or int?
        if ( ( leftPrimitive == Primitive.DOUBLE || leftPrimitive == Primitive.FLOAT)
            && ( isIntegerType(rightType) || rightPrimitive==Primitive.BYTE ) ) {
            return leftType;
        }
        if ( ( rightPrimitive==Primitive.DOUBLE || rightPrimitive==Primitive.FLOAT )
            && ( isIntegerType(leftType) || leftPrimitive==Primitive.BYTE ) ) {
            return rightType;
        }
        return null;
    }

    /**
     * Tests whether the type represents an integer.
     * @param exprType
     * @return true if integer division should be used.
     */
    public static boolean isIntegerType(Type exprType) {
        if ( exprType == null || !exprType.isPrimitiveType() ) {
            return false;
        }
        Primitive primitive = exprType.asPrimitiveType().getType();
        return primitive == Primitive.INT || primitive == Primitive.LONG || primitive == Primitive.SHORT;
    }
}