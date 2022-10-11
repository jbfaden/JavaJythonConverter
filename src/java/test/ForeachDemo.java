
package test;

/**
 * Demo foreach statement
 * @author jbf
 */
public class ForeachDemo {
    public static void main( String[] args ) {
        for ( String s : new String[] { "a", "b", "c" } ) {
            System.err.println(s);
        }
    }
}
