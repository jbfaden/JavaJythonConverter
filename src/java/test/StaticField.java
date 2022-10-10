package test;

class StaticField {
    static int MYINT = 5;
    static void my1() { 
        System.out.println( MYINT );
    }
    static void mymethod() { 
        my1();
        System.out.println( MYINT );
    }
}