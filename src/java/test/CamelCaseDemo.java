
package test;


/**
 * Provide the option to have variable names and functions converted from camel case to snake case
 * (camelCase -> snake_case).
 * @author jbf
 */
class CamelCaseDemo {
    
    private static final int myConst= 4;
    
    private static String showFavor( String nameURL ) {
        return nameURL + myConst;
    }
    
    public static void main( String[] args ) {
        int myBigVariable= 1;
        System.err.println("myBigVariable: "+myBigVariable);
        System.err.println("showFavor: "+ showFavor("NAME") );
    }
}

