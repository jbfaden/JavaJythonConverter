
package test;

/**
 *
 * @author jbf
 */
public class CurrentTimeMillisDemo {
    public static void main( String[] args ) throws InterruptedException {
        // demo that sleep/wait is converted properly
        long l= System.currentTimeMillis();
        Thread.sleep(1000);
        if ( System.currentTimeMillis()-l > 0 ) {
            System.err.println("Yup time has elapsed!");
        }
    }
}
