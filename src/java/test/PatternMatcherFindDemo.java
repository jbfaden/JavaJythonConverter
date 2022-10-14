
package test;

import java.util.regex.Pattern;

/**
 *
 * @author jbf
 */
public class PatternMatcherFindDemo {
    public static void main( String[] args ) {
        Pattern p= Pattern.compile("\\$[0-9]+\\{");
        
        boolean oldSpec2= p.matcher("__$99{__").find();
        System.err.println("oldSpec2: "+oldSpec2);
        
        boolean oldSpec3= p.matcher("$99{").find();
        System.err.println("oldSpec3: "+oldSpec3);

        boolean oldSpec4= p.matcher("$9x9{").find();
        System.err.println("oldSpec4: "+oldSpec4);        
    }
}
