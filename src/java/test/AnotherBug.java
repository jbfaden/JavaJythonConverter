package test;
        
class AnotherBug {
    static boolean isLeapYear(String yr) {
        return true;
    }
    static void testit() { 
        if (true) { 
            System.err.println(isLeapYear("1992"));
        } else {
            System.err.println(isLeapYear("1992"));
        }
    }
}
