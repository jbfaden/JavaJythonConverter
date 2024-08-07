package test;

/**
 * This gets closer to a real class which might do something.  This is an
 * object which has an internal state.
 * @author jbf
 */
public class TimeUtilTest {

    private int h, m;
    private int s;

    public TimeUtilTest( ) {
        this( 0, 0, 0 );
    }
    
    public TimeUtilTest( int h, int m, int s ) {
        this.h= h;
        this.m= m;
        this.s= s;
    }
    
    public void print() {
        System.out.println(toString());
    }

    /**
     * Add a component, hours or minutes or seconds, to the time.
     *
     * @param component 'h'|'m'|'s', the component to add
     * @param s, the number of that component
     */
    public void addComponent(String component, int s) {
        if (component.equals("s")) {
            this.s = this.s + s;
        } else if (component.equals("m")) {
            this.m = this.m + s;
        } else if (component.equals("h")) {
            this.h = this.h + s;
        }
        while (this.s > 60) {
            this.m = this.m + 1;
            this.s = this.s - 60;
        }
        while (this.m > 60) {
            this.h = this.h + 1;
            this.m = this.m - 60;
        }
    }
    
    @Override
    public String toString( ) {
        return String.format("%02d:%02d:%02d", this.h, this.m, this.s);
    }


}
