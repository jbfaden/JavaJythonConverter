package test;

/**
 * demo for loop handling
 * @author jbf
 */
public class ForDemo {
    public static void main( String[] args ) {
        int s= 0;
        int a= 0;
        for ( int i=0; i<10; i++ ) { // should convert
            s+= i;
        }
        System.out.println("s="+s);
        for ( s=0; s<10; s++ ) { // should not convert -- no init
            a=a+1;
        }
        System.out.println("a="+a);
        for ( int i=0; i<10; i++ ) { // should not convert -- i is modified
            i=i+1;
            a=a+1;
            System.out.println("a="+a +" i="+i);
        }
        for ( int i=0; i<=2; i++ ) {
            a=a+1;
            System.out.println("a="+a +" i="+i);
        }                
        for ( int i=10; i>2; i-- ) {
            a=a+1;
            System.out.println("a="+a +" i="+i);
        }
        for ( int i=10; i>=2; i-- ) {
            a=a+1;
            System.out.println("a="+a +" i="+i);
        }        

    }
    
    
}
