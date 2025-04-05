import java.beans.Transient;
import java.util.Arrays;
import java.util.List;

import com.cottagesystems.convert.ConvertJavaToJavascript;
import com.github.javaparser.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class ConvertJavaToJavascriptTest {

    private final ConvertJavaToJavascript converter = new ConvertJavaToJavascript();

    @Test
    void testAssignment() throws ParseException {
        assertEquals("x = 3", converter.doConvert("x=3"));
        assertLinesMatch(
            Arrays.asList("var times = [0,0,0,0,0,0,0];"),
            Arrays.asList(converter.doConvert("int[] times= new int[7];")));
    }

    @Test
    void testComments() throws ParseException {
        // TODO: we're getting extra trailing newlines in output.
        assertEquals("// hi", converter.doConvert("// hi").replace("\n", ""));
        assertEquals("/** bye  */", converter.doConvert("/* bye */").replace("\n", ""));
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

    @Test
    void testForLoop() throws ParseException {
        String javaProgram = "// Sum 'em\n" +
        "class Summer {\n" +
        "  public static int sum(int[] x) {\n" +
        "    int sum = 0;\n" +
        "    for (int i = 0; i < x.length; i++) {\n" +
        "      sum += x[i];" +
        "    }\n" +
        "    return sum;" +
        "  }\n" +
        "};";
        List<String> expectedJavaScript = Arrays.asList(
            "/**",
            " Sum 'em",
            " */",
            "class Summer {",
            "    static sum(x) {",
            "        var sum = 0;",
            "        for ( var i = 0; i < x.length; i++) {",
            "            sum += x[i];",
            "        }",
            "        return sum;",
            "    }",
        "}");
        assertEquals(expectedJavaScript, Arrays.asList(converter.doConvert(javaProgram).split("\n+")));
    }

    @Test
    void testIfElse() throws ParseException {
        // TODO: maintain comments on branches of if-else.
        // TODO: maintain indent level for comments.
        // TODO: render one-line comments from Java same in JavaScript.
        String javaProgram =
        "class Maxer {\n" +
        "  // Returns the bigger one.\n" +
        "  public static int max(int x, int y) {\n" +
        "    if (x > y) {\n" +
        "      return x;  // x is bigger\n" +
        "    } else {\n" +
        "      return y; // y is bigger\n" +
        "    }\n" +
        "  }\n" +
        "};";
        List<String> expectedJavaScript = Arrays.asList(
            "class Maxer {",
            "    /**",
            " Returns the bigger one.",
            "     */",
            "    static max(x, y) {",
            "        if (x > y) {",
            "            return x;",
            "        } else {",
            "            return y;",
            "        }",
            "    }",
            "}");
        assertEquals(expectedJavaScript, Arrays.asList(converter.doConvert(javaProgram).split("\n+")));
    }
}

