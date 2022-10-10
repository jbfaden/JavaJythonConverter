
package test;

/**
 * remove deprecated methods from classes.
 * @author jbf
 */
public class DeprecatedMethod {
    public static String method( String arg ) {
        return method( arg, 0 );
    }
    
    /**
     * @deprecated
     */
    @Deprecated
    public static String method( String arg, int offset ) {
        return arg.substring(offset);
    }
}
