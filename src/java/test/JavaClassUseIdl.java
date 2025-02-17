/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test;

import java.util.regex.Pattern;


/**
 * IDL requires that when an expression has a method called, that it be
 * wrapped in parentheses.  For example, 
 * <code>p.matcher(formatString).find();</code>
 * must have <code>(p.matcher(formatString)).find()</code>.
 * @author jbf
 */
public class JavaClassUseIdl {
    public static void main( String[] args ) {
        String formatString= "data*";
        boolean wildcard= formatString.contains("*");
        boolean oldSpec= formatString.contains("${");
        Pattern p= Pattern.compile("\\$[0-9]+\\{");
        boolean oldSpec2= p.matcher(formatString).find();
        System.err.println(oldSpec2);
        String timeString="1234 56789";
        System.err.println( Character.isWhitespace( timeString.charAt(5) ) );
    }

}
