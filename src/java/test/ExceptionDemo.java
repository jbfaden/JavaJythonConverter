
package test;

import java.io.IOException;

/**
 * how well can we convert exceptions?
 * @author jbf
 */
public class ExceptionDemo {
    
    private static void doSomething() throws IOException {
        throw new IOException("You called me");
    }
    
    public static void main( String[] args ) {
        try {
            doSomething();
        } catch ( IOException ex2 ) {
            System.out.println(ex2);
        }
    }
    
}
