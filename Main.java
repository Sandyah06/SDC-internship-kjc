//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter The Limit until where it should be printed : ");
        int a = sc.nextInt();


        System.out.println("Even numbers are :");
        for (int i = 1; i <= a; i++){
            if (i%2==0) {
                System.out.println(i+"");
            }}

        System.out.println("");
        System.out.println("odd numbers are :");
        for (int j = 1; j <= a; j++){
            if (j%2!=0) {
                System.out.println(j+"");
            }}
        sc.close();
    }
}