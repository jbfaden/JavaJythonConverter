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
        System.err.println( s.replaceFirst("1","7") );
        System.err.println( s.replaceAll("1","7") );
        System.err.println( s.replace("1","7") );
        System.err.println( s.replaceFirst("1","7") );
        System.err.println( s.replace("[23]","_") );
        System.err.println( s.replace("\\3", "_"));
    }
    
}
