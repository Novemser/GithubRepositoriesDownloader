package test;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * GithubRepoGetter
 * <p>
 * Created by Novemser on 2016/10/1.
 */
public class Circulation {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        double e1 = 0, e, sum = 1;
        int x, n = 1;
        Scanner input = new Scanner(System.in);
        System.out.print("输入n的值:");
        x = input.nextInt();
        while (n <= x) {
            sum = sum * n;
            e1 = e1 + (1.0 * n / sum);
            n++;
        }

        e1 = e1 + 1;

        BigDecimal a = new BigDecimal(e1);
        e=a.setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue();
        System.out.println("e="+e);

    }
}
