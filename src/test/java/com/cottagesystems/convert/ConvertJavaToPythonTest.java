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
    void testSlashSlashInString() throws ParseException {
        assertEquals("x = 'https://autoplog.org/'", converter.doConvert("String x = \"https://autoplog.org/\";"));
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
        "        result = -1;\n" +
        "    }\n" +
        "    return result;\n" +
        "  }\n" +
        "}";
        // TODO: avoid inserting extra blank at beginning?
        List<String> expectedPython = Arrays.asList("",
            "# Select!",
            "def select(x, a, b):",
            "    result = -1",
            "    if x == 'A' or x == 'a':",
            "        result = a",
            "    elif x == 'B' or x == 'b':",
            "        result = b",
            "    else:",
            "        result = -1",
            "    return result");
        assertLinesMatch(expectedPython, Arrays.asList(converter.doConvert(javaProgram).split("\n+")));
    }

    @Test
    void testFullClassWithSlashSlashInString() throws ParseException {
        String javaProgram = "package test;\n" +
        "public class TestServletPWD {\n" +
        "  public static void main( String[] args ) {\n" +
        "    String vapurl= \"https://github.com/autoplot/dev/blob/master/demos/2024/20240907/kodalith.vap\";\n" +
        "    System.out.println(vapurl);\n" +
        "  }\n" +
        "}";
        // Currently strips the class definition
        List<String> expectedPython = Arrays.asList("",
            "def main(args):",
            "    vapurl = 'https://github.com/autoplot/dev/blob/master/demos/2024/20240907/kodalith.vap'",
            "    print(vapurl)",
            "if __name__ == '__main__':",
            "    main([])");
        assertLinesMatch(expectedPython, Arrays.asList(converter.doConvert(javaProgram).split("\n+")));
    }
}

