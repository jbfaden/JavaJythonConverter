package test;

/**
 * This gets closer to a real class which might do something.  These are
 * static methods, used to operate on a prescribed representation of time.
 * @author jbf
 */
public class TimeUtil {
    
    public static int HOUR=0;
    public static int MINUTE=1;
    public static int SECOND=2;
    
    public static int[] create( int h, int m, int s ) {
        return new int[] { h, m, s };
    }
    
    /**
     * print the time to stdout.
     * @param time three-component time array
     */
    public static void print( int[] time ) {
        System.out.println(toString(time));
    }

    /**
     * Add a component, hours or minutes or seconds, to the time.
     *
     * @param time, a three-element int array of [ hours, minutes, seconds ]
     * @param component, SECOND or MINUTE or HOUR
     * @param s, the number of that component
     */
    public static void addComponent( int [] time, int component, int s) {
        if ( component==SECOND ) {
            time[SECOND]= time[SECOND] + s;
        } else if ( component==MINUTE ) {
            time[MINUTE]= time[MINUTE] + s;
        } else if ( component==HOUR ) {
            time[HOUR]= time[HOUR] + s;
        }
        while ( time[SECOND] > 60) {
            time[MINUTE] = time[MINUTE] + 1;
            time[SECOND] = time[SECOND]- 60;
        }
        if ( time[MINUTE] > 60) {
            int hr= time[MINUTE] / 60;
            int mn= time[MINUTE] - 60 * hr;
            time[HOUR] = time[HOUR] + hr;
            time[MINUTE] = time[MINUTE] - hr*60 + mn;
        }
    }
    
    public static String toString( int[] time ) {
        return String.format("%02d:%02d:%02d", time[0], time[1], time[2] );
    }


}
