
package test;

/**
 *
 * @author jbf
 */
public class StringFormatDemo {
    
    public static void main( String[] args ) {
        int[] nn= new int[] { 2022, 12, 27 };
        String time =  String.format("%04d-%02d-%02dZ", nn[0], nn[1], nn[2]);
        System.out.println(time);
    }

}
