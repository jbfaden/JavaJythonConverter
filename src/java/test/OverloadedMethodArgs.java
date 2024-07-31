
package test;

/**
 * Demonstrates where an additional optional argument is added in Java using
 * overloaded methods.
 * @author jbf
 */
public class OverloadedMethodArgs {
    public static void printSum( int[] nn ) {
        printSum( nn, 0 );
    }
    
    public static void printSum( int[] nn, int fill ) {
        int sum=0; 
        for ( int i=0; i<nn.length; i++ ) {
            if ( nn[i]!=fill ) {
                sum+= nn[i];
            }
        }
        System.err.println("Sum is "+sum);
    }
    public static void main( String[] args ) {
        printSum( new int[] { 1,2,3,4,5 } );
    }
}
