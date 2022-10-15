package test;

/**
 * Demonstrate different handling of static fields vs class fields.
 * @author jbf
 */
public class StaticFieldClassFieldDemo {
    static String warn = "warning";
    static String okay = "ok";
    String status;
    StaticFieldClassFieldDemo() {
        this.status= warn;
    }
    void printStatus() {
        if ( status.equals(warn) ) {
            System.out.println("** "+status+" **");
        } else {
            System.out.println(status);
        }
    }
    public static void main( String[] args ) {
        new StaticFieldClassFieldDemo().printStatus();
    }
}
