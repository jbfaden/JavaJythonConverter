
package test;

/**
 * Demonstrates where Python conversion needs to rename a variable.
 * TODO: this is not done yet, and the Java code must be manually 
 * modified.
 * @author jbf
 */
public class FieldNameConflictDemo {
    private final String format="%04d";
    public String format( int i ) {
        return String.format( this.format, i );
    }
    public static void main(String[] args ) {
        System.out.println( new FieldNameConflictDemo().format(99) );
    }
}
