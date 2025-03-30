
import com.cottagesystems.convert.ConvertJavaToJavascript;
import com.github.javaparser.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConvertJavaToJavascriptTest {

    private final ConvertJavaToJavascript converter = new ConvertJavaToJavascript();

    @Test
    void testAssignment() throws ParseException {
        assertEquals("x = 3", converter.doConvert("x=3"));
    }

    @Test
    void testArithmeticOps() throws ParseException {
        assertEquals("-x * y + z / 2 - 5", converter.doConvert("-x*y+z/2-5"));
    }

    @Test
    void testComparisonOps() throws ParseException {
        assertEquals("x < y", converter.doConvert("x<y"));
        assertEquals("x <= y", converter.doConvert("x<=y"));
        assertEquals("x > y", converter.doConvert("x>y"));
        assertEquals("x >= y", converter.doConvert("x>=y"));
        assertEquals("x == y", converter.doConvert("x==y"));
    }

    @Test
    void testArrayLiteral() throws ParseException {
        assertEquals("[3, 1, 2]", converter.doConvert("new int[]{3,1,2}"));
    }

    @Test
    void testStringLiteralWithSingleQuote() throws ParseException {
        assertEquals("\"ain't it pretty\"", converter.doConvert("\"ain't it pretty\""));
    }

    @Test
    void testStringCatenation() throws ParseException {
        assertEquals("\"XYZ\" + \" pizza\"", converter.doConvert("\"XYZ\" + \" pizza\""));
    }

}

