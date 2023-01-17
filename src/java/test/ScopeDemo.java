package test;

/**
 * Demo scope handling, such as local variables, class variables, and static fields.
 *
 * @author jbf
 */
public class ScopeDemo {

    public static int MONTH = 2;
    private int digit;
    private int attempts=0;

    public static String version() {
        return "20230117";
    }

    public boolean isMonth() {
        attempts++;
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
    
    @Override
    public String toString() {
        return "digit: "+digit + "  attempts: "+attempts;
    }
    
    public static void main(String[]args ) {
        ScopeDemo d= new ScopeDemo();
        d.setDigit(3);
        d.isMonth();
    }

}
