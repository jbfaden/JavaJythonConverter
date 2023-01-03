package test;

interface State {
   public String action();
} 

class State1 implements State {
   @Override
   public String action() {
       return "State1";
   }
}

class State2 implements State {
   @Override
   public String action() {
       return "State2";
   }
}

public class Stateful {
    public static void main( String[] args ) {
        State s = new State1();
        for ( int i=0; i<10; i++ ) {
            System.err.println( "" + i + " " + s.action() );
            if ( i==5 ) s= new State2();
        }
    }
}
