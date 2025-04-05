import java.util.Arrays;
import java.util.List;

import com.cottagesystems.convert.ConvertJavaToPython;
import com.github.javaparser.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

class ConvertJavaToPythonTest {

    private final ConvertJavaToPython converter = new ConvertJavaToPython();

    public ConvertJavaToPythonTest() {
        converter.setUnittest(false);
    }

    @Test
    void testAssignment() throws ParseException {
        assertEquals("x = 3", converter.doConvert("x=3"));
        assertLinesMatch(
            Arrays.asList("times = [0] * 7"),
            Arrays.asList(converter.doConvert("int[] times= new int[7];")));
    }

    @Test
    void testComments() throws ParseException {
        // TODO: we're getting extra trailing newlines in output.
        assertEquals("# hi", converter.doConvert("// hi").replace("\n", ""));
        // TODO: avoid trailing spaces.
        assertEquals("# bye ", converter.doConvert("/* bye */").replace("\n", ""));
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
        "    for (int i = 0; i < x.length; i++) {\n" +
        "      sum += x[i];" +
        "    }\n" +
        "    return sum;" +
        "  }\n" +
        "};";
        // TODO: avoid inserting extra blank at beginning?
        List<String> expectedPython = Arrays.asList("",
            "# Sum 'em",
            "def sum(x):",
            "    sum = 0",
            "    for i in range(0, len(x)):",
            "        sum += x[i]",
            "    return sum");
        assertEquals(expectedPython, Arrays.asList(converter.doConvert(javaProgram).split("\n+")));
    }
}

