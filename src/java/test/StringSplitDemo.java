
package test;

import java.util.Arrays;

/**
 * String.split doesn't use a regex in Python
 * @author jbf
 */
public class StringSplitDemo {
    public static void main( String[] args ) {
        String formatString= "$Y$m$d";
        String [] ss = formatString.split("\\$");
        System.out.println(Arrays.toString(ss));
    }
}
