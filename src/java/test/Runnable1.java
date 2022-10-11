
package test;

/**
 *
 * @author jbf
 */
public class Runnable1 {
    public void v() {
        Runnable r = new Runnable() {
            public void run() {
                System.out.println("hello");
            }
        };
        new Thread(r).run();
    }
    public static void main( String[] args ) {
        new Runnable1().v();
    }
}