package test;

/**
 *
 * @author jbf
 */
public class IfFun {

    public static void main(String[] args) {
        if (true) {
            System.out.println("h0");
        } else {
            System.out.println("h1");
        }

        if (true) {
            System.out.println("h2");
        }

        if (true) {
            System.out.println("h2");
        } else {
            System.out.println("h4");
        }

        if (true) {
            System.out.println("h5");
        } else {
            System.out.println("h6");
        }

        if (true) {
            System.out.println("h7");
        } else if (true) {
            System.out.println("h8");
        }

        if (true) {
            System.out.println("h9");
        } else if (true) {
            System.out.println("h10");
        } else {
            System.out.println("h11");
        }

        if (true) {
            System.out.println("h9");
        } else if (true) {
            System.out.println("h10");
        } else {
            System.out.println("h11");
        }

    }
}
