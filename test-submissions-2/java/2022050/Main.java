import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        int[] nums = {
                Integer.parseInt(args[0]),
                Integer.parseInt(args[1]),
                Integer.parseInt(args[2])
        };

        Arrays.sort(nums);
        System.out.println(nums[0] + " " + nums[1] + " " + nums[2]);
    }
}