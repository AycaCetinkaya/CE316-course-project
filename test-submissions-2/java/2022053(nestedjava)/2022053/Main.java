import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int[] nums = new int[args.length];

        for (int i = 0; i < args.length; i++) {
            nums[i] = Integer.parseInt(args[i]);
        }

        Arrays.sort(nums);

        for (int i = 0; i < nums.length; i++) {
            if (i > 0) System.out.print(" ");
            System.out.print(nums[i]);
        }

        System.out.println();
    }
}