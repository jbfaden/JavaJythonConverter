package test;

import javax.imageio.ImageIO;


/**
 * This is a small class useful for demonstrating code.
 * @author jbf
 */
public class SmallClass {
    
    int data;

    public static void stringCharIssues( String[] args ) {
        String s="a";
        for ( int j=0; j<26; j++ ) {
            System.out.println( new String( new char[] { (char)( s.charAt(0)+j ) } ) );
        }
        System.err.println( ImageIO.getCacheDirectory() );
        
        String b= s + (char)(98);
        System.err.println(b);
    }

    @Override
    public String toString() {
        return "a SmallClass "+this.data;
    }
    
    public static void main( String[] args ) {
        stringCharIssues(args);
    }
}
