
package test;

/**
 * IDL has some poor conversions of while.
 * @author jbf
 */
public class WhileDemo {
    public static void main( String[] args ) {
        int rep=10;
        while (rep>0) {
            rep=rep-1;
            System.out.println("rep="+rep);
        }
        System.out.println("Now count up from rep="+rep+"\n");
        while (rep<10) rep++;
        System.out.println( String.format("Done rep=%d\n",rep) );
    }
}
