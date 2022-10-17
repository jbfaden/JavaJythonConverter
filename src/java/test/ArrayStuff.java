
package test;

/**
 *
 * @author jbf
 */
public class ArrayStuff {
    public static int foo() {
        int[] times= new int[7];
        times[3]= 1;
        System.out.println(times[3]);
        return 0;
    }
    public static void main(String[]args ) {
        if ( args.length>0 ) {
            System.err.println( args.length );
        }
    }
}
