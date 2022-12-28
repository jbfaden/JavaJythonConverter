
package test;

/**
 *
 * @author jbf
 */
public class EqualsIgnoreCase {
    public static void main(String[] args) {
        if ( "aaa".equalsIgnoreCase("AAA") ) {
            System.err.print("BBB".toLowerCase());
        }
    }
}
