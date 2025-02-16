
package test;

/**
 *
 * @author jbf
 */
public class ReplaceAllDemo {
    public static void main( String[] args ) {
        String name="abbcddeffggsdf";
        String[] ss= new String[] { "b+", "f+" };
        for ( String s: ss ) {
            name= name.replaceAll( s, "_" );
        }
        System.out.println(name);
    }
}
