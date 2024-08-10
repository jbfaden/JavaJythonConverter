package test;

/**
 * IDL must have a dummy variable to call the function.
 * @author jbf
 */
public class FunctionCallDemo2 {

    public static int myfunc() {
        System.out.println("I am a function");
        return(1);
    }

    public static void mypro() {
        System.out.println("I am a pro");
    }

    public static void main(String[] args) {
        //IDL needs a place to put result.
        mypro();
        myfunc();
    }
}
