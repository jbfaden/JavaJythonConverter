/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test;

/**
 *
 * @author jbf
 */
public class ForEachExample {
  public static void main(String[] args) {
    int[] numbers = {1, 2, 3, 4, 5};

    for (int number : numbers) {
      System.out.println(number);
    }

    String[] names = {"Alice", "Bob", "Charlie"};

    for (String name : names) {
      System.out.println("Hello, " + name + "!");
    }
  }
}