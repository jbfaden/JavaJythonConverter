/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jbf
 */
public class SetDemo {

    public static void main(String[] args) {
        Set s = new HashSet();
        s.add('a');
        s.add('b');

        Set z = new HashSet();
        z.add('y');
        z.add('z');

        s.addAll(z);
        System.out.println(Arrays.asList(s));
    }
}
