/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

/**
 *
 * @author jbf
 */
public class StaticClass1 {
    
    static void bar1() {
        bar2();
    }
    static void bar2() {
        bar1();
    }
}
