
package test;

/**
 * FibExample calculates the first ten numbers of Fibonacci sequence.
 * https://en.wikipedia.org/wiki/Fibonacci_number
 * @author jbf
 */
public class FibExample {
   public static void main(String[] args) {

    int n = 10, firstTerm = 0, secondTerm = 1;

    System.out.println("Fibonacci Series till " + n + " terms:");
    char c= 'a';
    System.out.println( c + " the char"); 

    for (int i = 1; i < n; i++) {
      System.out.println(firstTerm + ", ");

      // compute the next term
      int nextTerm = firstTerm + secondTerm;
      firstTerm = secondTerm;
      secondTerm = nextTerm;
    }
  }
}
