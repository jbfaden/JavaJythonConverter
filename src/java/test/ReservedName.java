
package test;

/**
 * Here we see that the function name will need to be modified so that
 * there is not a conflict with the reserved words of the language.
 * @author jbf
 */
public class ReservedName {
    public static boolean gt(int a, int b) {
        return a>b;
    }
    public static void main(String[] args ) {
        System.out.println(gt(9,0));
    }

}
