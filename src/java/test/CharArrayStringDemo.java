
package test;

/**
 * String constructors that take char array etc
 * @author jbf
 */
public class CharArrayStringDemo {
    public static void main( String[] args ) {
        {
            char[] cc= new char[4];
            for ( int i=97; i<101; i++ ) cc[i-97]= (char)i;
            String s= new String(cc);
            System.out.println(s);
            
        }
        {
            StringBuilder sb= new StringBuilder("The ");
            sb.append("big red ");
            sb.append("dog");
            String s= new String(sb);
            System.out.println(s);
        }
    }
}
