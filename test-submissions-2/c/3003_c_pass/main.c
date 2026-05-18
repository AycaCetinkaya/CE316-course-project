#include <stdio.h>
#include <stdlib.h>

int main(int argc, char *argv[]) {
    int nums[32];
    int count = argc - 1;

    for (int i = 0; i < count; i++) {
        nums[i] = atoi(argv[i + 1]);
    }

    for (int i = 0; i < count; i++) {
        for (int j = i + 1; j < count; j++) {
            if (nums[j] < nums[i]) {
                int temp = nums[i];
                nums[i] = nums[j];
                nums[j] = temp;
            }
        }
    }

    for (int i = 0; i < count; i++) {
        if (i > 0) {
            printf(" ");
        }
        printf("%d", nums[i]);
    }
    printf("\n");

    return 0;
}
