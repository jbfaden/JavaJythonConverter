
package test;

/**
 * Demonstrates some of the difficulties in converting when Jython doesn't have a Character type which is like and integer.
 * @author jbf
 */
public class StringCharacterInt {
    private static int parseInt(String s) {
        int result;
        int len= s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 48 || c >= 58) {
                throw new IllegalArgumentException("only digits are allowed in string");
            }
        }
        switch (len) {
            case 2:
                result = 10 * (s.charAt(0) - 48) + (s.charAt(1) - 48);
                return result;
            case 3:
                result = 100 * (s.charAt(0) - 48) + 10 * (s.charAt(1) - 48) + (s.charAt(2) - 48);
                return result;
            default:
                result = 0;
                for (int i = 0; i < s.length(); i++) {
                    result = 10 * result + (s.charAt(i) - 48);
                }
                return result;
        }
    }
    
}
