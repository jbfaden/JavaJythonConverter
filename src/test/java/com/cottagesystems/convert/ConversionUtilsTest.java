import com.cottagesystems.convert.ConversionUtils;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConversionUtilsTest {

    @Test
    void testIsIntegerType() {
        // Non-primitive stuff
        assertFalse(ConversionUtils.isIntegerType(null));
        assertFalse(ConversionUtils.isIntegerType(new ClassOrInterfaceType("Integer")));
        // Floating point types
        assertFalse(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.FLOAT)));
        assertFalse(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.DOUBLE)));
        // Integer types
        assertTrue(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.INT)));
        assertTrue(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.SHORT)));
        assertTrue(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.LONG)));
        // Excluded int-like types.
        assertFalse(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.BYTE)));
        assertFalse(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.CHAR)));
        assertFalse(ConversionUtils.isIntegerType(new PrimitiveType(Primitive.BOOLEAN)));
    }

    @Test
    void testFindBinaryExprType() {
        final Type intType = new PrimitiveType(Primitive.INT);
        final Type floatType = new PrimitiveType(Primitive.FLOAT);
        assertEquals(null, ConversionUtils.findBinaryExprType(intType, null));
        assertEquals(null, ConversionUtils.findBinaryExprType(null, intType));
        assertEquals(intType, ConversionUtils.findBinaryExprType(intType, intType));
        assertEquals(floatType, ConversionUtils.findBinaryExprType(floatType, intType));
        assertEquals(floatType, ConversionUtils.findBinaryExprType(intType, floatType));
        // TODO: do we want to change the behavior in these cases, promoting to wider type instead?
        final Type longType = new PrimitiveType(Primitive.LONG);
        final Type doubleType = new PrimitiveType(Primitive.DOUBLE);
        assertEquals(null, ConversionUtils.findBinaryExprType(intType, longType));
        assertEquals(null, ConversionUtils.findBinaryExprType(floatType, doubleType));
    }
}

