//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.util.Scanner;

public class arithmetic{
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter The 1st number : ");
        int a = sc.nextInt();
        System.out.print("Enter The 2nd number : ");
        int b = sc.nextInt();
        System.out.print("Enter your choice : ");
        int choice = sc.nextInt();
        sc.close();
        switch (choice) {
            case 1:
                System.out.println("sum is " + (a + b));
                break;
            case 2:
                System.out.println("difference is " + (a - b));
                break;
            case 3:
                System.out.println("quotient is " + a / b);
                break;
            case 4:
                System.out.println("multiplication is " + a * b);
        }

    }
}
