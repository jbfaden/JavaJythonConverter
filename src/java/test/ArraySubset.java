
package test;

import java.util.Arrays;

/**
 *
 * @author jbf
 */
public class ArraySubset {
    public static void main(String[]args) {
        String[] a = "0,1,2,3,4,5,6,7,8,9".split("\\,");
        String[] a1= Arrays.copyOfRange( a, 3, 6 );
        System.err.println( String.join( ",", a1 ) );
    }
}
