
package test;

/**
 * test translation of Java class with inner classes
 * @author jbf
 */
public class InnerClass {
    static interface State {
        public void action();
    };
    static class State1 implements State {
        @Override
        public void action() {
            System.out.println("state1");
        }
    }
    static class State2 implements State {
        @Override
        public void action() {
            String b="";
            for ( int j=0; j<4; j++ ) {
                b+='a';
            }
            System.out.println("state2 "+b);
        }
    }
    public static void main( String[] args ) {
        State state= new State1();
        for ( int i=0; i<10; i++ ) {
            state.action();
            if ( i==4 ) state= new State2();
        }
    }
}
