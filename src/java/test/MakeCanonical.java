/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.regex.Pattern;

/**
 *
 * @author jbf
 */
public class MakeCanonical {
        /**
     * convert %() and ${} to standard $(), and support legacy modes in one
     * compact place.  Asterisk (*) is replaced with $x.
     * Note, commas may still appear in qualifier lists, and 
     * makeQualifiersCanonical will be called to remove them.
     * Copied from Das2's TimeParser.
     * @param formatString like %{Y,m=02}*.dat or $(Y;m=02)$x.dat
     * @return formatString containing canonical spec, $() and $x instead of *, like $(Y,m=02)$x.dat
     */
    public static String makeCanonical( String formatString ) {
        boolean wildcard= formatString.contains("*");
        boolean oldSpec= formatString.contains("${");
        Pattern p= Pattern.compile("\\$[0-9]+\\{");
        boolean oldSpec2= p.matcher(formatString).find();
        if ( formatString.startsWith("$") && !wildcard && !oldSpec && !oldSpec2 ) return formatString;
        if ( formatString.contains("%") && !formatString.contains("$") ) {
            formatString= formatString.replaceAll("\\%", "\\$");
        }
        oldSpec= formatString.contains("${"); // it might contain this now.
        if ( oldSpec && !formatString.contains("$(") ) {
            formatString= formatString.replaceAll("\\$\\{", "\\$(");
            formatString= formatString.replaceAll("\\}", "\\)");
        }
        if ( oldSpec2 && !formatString.contains("$(") ) {
            formatString= formatString.replaceAll("\\$([0-9]+)\\{", "\\$$1(");
            formatString= formatString.replaceAll("\\}", "\\)");
        }
        if ( wildcard ) {
            formatString= formatString.replaceAll("\\*", "\\$x");
        }
        int i=1;
        if ( i<formatString.length() && formatString.charAt(i)=='(' ) {
            i++;
        }
        while ( i<formatString.length() 
                && Character.isAlphabetic(formatString.charAt(i) ) ) {
            i++;
        }
        if ( i<formatString.length() && formatString.charAt(i)==',' ) {
            formatString= formatString.replaceFirst(",",";");
        }
            
        return formatString;
    }
    
    public static void main( String[] args ) {
        System.out.println(makeCanonical("%Y_*.dat"));
    }

}
