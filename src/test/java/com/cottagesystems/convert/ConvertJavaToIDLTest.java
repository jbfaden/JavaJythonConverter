import java.util.Arrays;
import java.util.List;

import com.cottagesystems.convert.ConvertJavaToIDL;
import com.github.javaparser.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class ConvertJavaToIDLTest {

    private final ConvertJavaToIDL converter = new ConvertJavaToIDL();

    @Test
    void testAssignment() throws ParseException {
        assertEquals("x = 3", converter.doConvert("x=3"));
    }

    @Test
    void testComments() throws ParseException {
        // TODO: we're getting extra trailing newlines in output.
        assertEquals("; hi\n", converter.doConvert("// hi"));
        assertEquals(";+\n;bye\n;-\n\n", converter.doConvert("/* bye */"));
    }

    @Test
    void testArithmeticOps() throws ParseException {
        assertEquals("-x * y + z / 2 - 5", converter.doConvert("-x*y+z/2-5"));
    }

    @Test
    void testComparisonOps() throws ParseException {
        assertEquals("x lt y", converter.doConvert("x<y"));
        assertEquals("x le y", converter.doConvert("x<=y"));
        assertEquals("x gt y", converter.doConvert("x>y"));
        assertEquals("x ge y", converter.doConvert("x>=y"));
        assertEquals("x eq y", converter.doConvert("x==y"));
    }

    @Test
    void testArrayLiteral() throws ParseException {
        assertEquals("[3, 1, 2]", converter.doConvert("new int[]{3,1,2}"));
    }

    @Test
    void testStringLiteralWithSingleQuote() throws ParseException {
        assertEquals("'ain\\'t it pretty'", converter.doConvert("\"ain't it pretty\""));
    }

    @Test
    void testStringCatenation() throws ParseException {
        assertEquals("'XYZ' + ' pizza'", converter.doConvert("\"XYZ\" + \" pizza\""));
    }

    @Test
    void testForLoop() throws ParseException {
        String javaProgram = "class Summer {\n" +
        "  // Sum 'em\n" +
        "  public static int sum(int[] x) {\n" +
        "    int sum = 0;\n" +
        "    // Yup\n" +
        "    for (int i = 0; i < x.length; i++) {\n" +
        "      sum += x[i];" +
        "    }\n" +
        "    return sum;" +
        "  }\n" +
        "};";
        // TODO: avoid inserting extra blank at beginning of output.
        // TODO: should we be keeping that internal comment also?
        List<String> expectedPython = Arrays.asList(
            "",
            ";+",
            ";Sum 'em",
            ";-",
            "function sum, x",
            "    sum = 0",
            "    for i=0,n_elements(x)-1 do begin",
            "        sum += x[i]",
            "    endfor",
            "    return, sum",
            "end");
        assertLinesMatch(expectedPython, Arrays.asList(converter.doConvert(javaProgram).split("\n+")));
    }

    @Test
    void testSwitch() throws ParseException {
        String javaProgram = "class Selector {\n" +
        "  // Select!\n" +
        "  public static int select(String x, int a, int b) {\n" +
        "    int result = -1;\n" +
        "    switch(x) {\n" +
        "      case \"A\":\n" +
        "      case \"a\":\n" +
        "        result = a;\n" +
        "        break;\n" +
        "      case \"B\":\n" +
        "      case \"b\":\n" +
        "        result = b;\n" +
        "        break;\n" +
        "      default:\n" +
        "        result = -100;\n" +
        "    }\n" +
        "    return result;\n" +
        "  }\n" +
        "}";
        // TODO: avoid inserting extra blank at beginning?
        List<String> expectedPython = Arrays.asList("",
            ";+",
            ";Select!",
            ";-",
            "function select, x, a, b",
            "    result = -1",
            "    SWITCH x OF",
            "        'A':",
            "        'a': BEGIN",
            "            result = a",
            "            break",
            "        END",
            "        'B':",
            "        'b': BEGIN",
            "            result = b",
            "            break",
            "        END",
            "        ELSE: BEGIN",
            "            result = -100",
            "        END",
            "    ENDSWITCH",
            "    return, result",
            "end");
            assertLinesMatch(expectedPython, Arrays.asList(converter.doConvert(javaProgram).split("\n+")));
    }
}

