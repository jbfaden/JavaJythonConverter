
package test;

/**
 * pep8 says there should be whitespace around operators, etc.
 * @author jbf
 */
public class Pep8Formatting {
    public static void main( String[] args ) {
        int c= (1+2)*4/5;
        if ( c>9 ) {
            c++;
        } else if (c==8) {
            c=c+1;
        } else {
            c=-c;
        }
    }
}
