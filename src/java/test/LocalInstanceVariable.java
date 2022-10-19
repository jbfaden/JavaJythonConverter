
package test;

/**
 * Verify handling of instance and local variables.
 * @author jbf
 */
public class LocalInstanceVariable {
    char vv = '0';
    
    public void action() {
        vv= '1';
        System.out.println("vv: "+ vv);
        if ( true ) {
            char vv = '2';
            System.out.println("vv: "+ vv);
            System.out.println("this.vv: "+ this.vv);
            if ( true ) {
                vv = '3';
                System.out.println("vv: "+ vv);
                this.vv='4';
                System.out.println("this.vv: "+ this.vv);
            }
            this.vv= '5';
            System.out.println("this.vv: "+ this.vv);
            vv= '6';
            System.out.println("vv: "+ vv);
        }
    }
    
    public static void main(String[] args ) {
        new LocalInstanceVariable().action();
    }
}
