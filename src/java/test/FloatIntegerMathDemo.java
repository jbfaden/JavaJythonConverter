
package test;

/**
 *
 * @author jbf
 */
public class FloatIntegerMathDemo {
    
    /**
     * return the julianDay for the year month and day. This was verified
     * against another calculation (julianDayWP, commented out above) from
     * http://en.wikipedia.org/wiki/Julian_day. Both calculations have 20
     * operations.
     *
     * @param year calendar year greater than 1582.
     * @param month the month number 1 through 12.
     * @param day day of month. For day of year, use month=1 and doy for day.
     * @return the Julian day
     * @see #fromJulianDay(int) 
     */
    public static int julianDay(int year, int month, int day) {
        if (year <= 1582) {
            throw new IllegalArgumentException("year must be more than 1582");
        }
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4
                - 3 * ((year + (month - 9) / 7) / 100 + 1) / 4
                + 275 * month / 9 + day + 1721029;
        return jd;
    }
    
    public static void main( String[] args ) {
        int a= 1000;
        int b= 34;
        double x= 34.;
        double y= 1000;
        float p = 3.14F;
        double d = 3.14d;
        
        System.out.println( a/b );
        System.out.println( a/x );
        System.out.println( a/(a+b) );
        System.out.println( a/(x+b) );
        System.out.println( a/(int)(x+b) );
        
        System.out.println( julianDay( 1979, 1, 1 ) );
        System.out.println( p );
        System.out.println( d);
    }
}
