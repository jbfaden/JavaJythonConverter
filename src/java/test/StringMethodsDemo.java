
package test;

/**
 *
 * @author jbf
 */
public class StringMethodsDemo {
    public static void main( String[] args ) {
        String s= "1234512345";
        System.out.println(s.indexOf("5"));
        System.out.println(s.indexOf("5",6));
        System.out.println(s.lastIndexOf("5"));
        System.out.println(s.lastIndexOf("5",6));
        System.out.println(s.length());
        System.out.println(s.startsWith("123"));
        System.out.println(s.substring(5).startsWith("123"));
    }
}
