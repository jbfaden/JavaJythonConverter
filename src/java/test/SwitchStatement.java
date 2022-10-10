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
public class SwitchStatement {
    public static void main( String[] args ) {
        char c='a';
        switch (c) {
            case 'a':
            case 'b':
                System.out.println("a or b");
            default:
                System.out.println("not a or b");
        }
    }
}
