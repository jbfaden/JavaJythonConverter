
package test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * demo regex compile, matcher, matches find, and group
 * @author jbf
 */
public class RegexDemo {
    public static void main( String[] args ) {
        Pattern p= Pattern.compile("a([abc]*)ab");
        Matcher m= p.matcher("abacab");
        if ( m.matches() ) {
            System.out.println(""+m.groupCount()+" "+m.group(0));
        } 
        m= p.matcher("babacabc");
        if ( m.find() ) {
            System.out.println(""+m.groupCount()+" "+m.group(0));
        }
    }
}
