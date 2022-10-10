/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

/**
 * test of when two methods are used so that a default setting can be used.  In
 * Jython this becomes method( arg, offset=0 ).
 * @author jbf
 */
public class DefaultArg {
    public static String method( String arg ) {
        return method( arg, 0 );
    }
    
    public static String method( String arg, int offset ) {
        return arg.substring(offset);
    }
}
