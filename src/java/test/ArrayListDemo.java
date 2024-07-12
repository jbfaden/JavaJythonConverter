
package test;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo list handling
 * @author jbf
 */
public class ArrayListDemo {
    public static void main(String[] args) {
        List list= new ArrayList();
        list.add("apple");
        list.add("bear");
        list.add("catcher");
        if (list.contains("k") ) System.err.println("it contains");
        System.out.println("list size "+list.size() );
        System.out.println("list index "+list.indexOf("catcher"));
        list.remove("apple");
        list.remove(0);
        System.out.println("list get " + list.get(0) );
    }
}
