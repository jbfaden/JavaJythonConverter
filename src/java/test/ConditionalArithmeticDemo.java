
package test;

/**
 * demo conditional expression
 * @author jbf
 */
public class ConditionalArithmeticDemo {
    // a method for finding the larger number between i & j  
    static int findMaxNo( int i, int j) {
        return i > j ? i : j ;
    }
    
    public static void main( String[] i ) {
        System.out.println(findMaxNo(1,2));
        System.out.println(findMaxNo(2,1));
        System.out.println(findMaxNo(2,2));
    }
}
