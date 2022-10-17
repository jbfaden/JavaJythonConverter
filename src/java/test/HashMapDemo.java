
package test;

import java.util.HashMap;

/**
 * HashMaps are converted to dictionaries.
 * @author jbf
 */
public class HashMapDemo {
    public static void main( String[] args ) {
        HashMap<String,String> m= new HashMap<>();
        m.put( "name", "foo" );
        System.out.println( m.get("name") );
        if ( m.containsKey("barg") ) {
            System.out.println( m.get("barg") );
        }
        System.out.println( m.get("barg") );
        m.remove("foo");
    }
}
