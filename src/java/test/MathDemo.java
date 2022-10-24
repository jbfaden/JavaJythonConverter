
package test;

/**
 * Java Math functions
 * @author jbf
 */
public class MathDemo {
    public static void main(String[] args ) {
        System.out.println(Math.floorDiv(-300,1000));
        System.out.println(Math.floorDiv(300,1000));
        System.out.println(Math.floorDiv(9999,1000));
        System.out.println(Math.floorDiv(10000,1000));
        System.out.println(Math.floorDiv(10001,1000));
        System.out.println(Math.floorDiv(-4,3));
        System.out.println(Math.floorDiv(0,1000));
        System.out.println(Math.floorDiv(-9999,1000));
        System.out.println(Math.floorDiv(-10000,1000));
        System.out.println(Math.floorDiv(-10001,1000));
        System.out.println(Math.floorMod(-4,3));        
        System.out.println(Math.floorMod(-41,3));        
    }
}
