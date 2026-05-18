#include <stdio.h>
#include <stdlib.h>

void sort3(int arr[]);

int main(int argc, char *argv[]) {
    int nums[3];

    nums[0] = atoi(argv[1]);
    nums[1] = atoi(argv[2]);
    nums[2] = atoi(argv[3]);

    sort3(nums);

    printf("%d %d %d\n", nums[0], nums[1], nums[2]);
    return 0;
}