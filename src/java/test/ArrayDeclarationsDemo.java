package test;

import java.util.Arrays;

/**
 *
 * @author jbf
 */
public class ArrayDeclarationsDemo {
   public static void main( String[] args ) {
       int a[]= { 1,2,3 };
       int[] b= { 1,2,3 };
       int[] c;
       c= new int[] { 1,2,3 };
       int d[];
       d= new int[] { 1,2,3 };
       int[][] e= { { 1,2,3 }, { 4,5,6 } };
       System.err.print(Arrays.asList(e));
   }
}
