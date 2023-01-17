package test;

/**
 * Demo scope handling, such as local variables, class variables, and static fields.
 *
 * @author jbf
 */
public class ScopeDemo {

    public static int MONTH = 2;
    private int digit;

    public static String version() {
        return "20230117";
    }

    public boolean isMonth() {
        int ldigit = 4;
        if (digit == MONTH) {
            return true;
        } else {
            if (ldigit == MONTH) {
                return true;
            } else {
                return false;
            }
        }
    }

    public void setDigit(int digit) {
        this.digit = digit;
    }
    
    public static void main(String[]args ) {
        ScopeDemo d= new ScopeDemo();
        d.setDigit(3);
        d.isMonth();
    }

}
