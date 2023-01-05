/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

/**
 *
 * @author jbf
 */
public class StringReplaceDemo {
    public static void main(String[]args ) {
        String s= "121212[23]4";
        String b= "1234\\\\45";
        System.err.println( "01: " + s );
        System.err.println( "02: " + b );
        System.err.println( "03: " + s.replaceFirst("1","7") );
        System.err.println( "04: " + b.replaceFirst("[\\\\]{2}","\\\\") ); // these are regular expressions
        System.err.println( "05: " + b.replace("\\\\","\\") );  // this is not
        System.err.println( "06: " + s.replaceAll("1","7") );
        System.err.println( "07: " + s.replace("1","7") );
        System.err.println( "08: " + s.replaceFirst("1","7") );
        System.err.println( "09: " + s.replace("[23]","_") );
        System.err.println( "10: " + s.replace("\\3", "_"));
    }
    
}
