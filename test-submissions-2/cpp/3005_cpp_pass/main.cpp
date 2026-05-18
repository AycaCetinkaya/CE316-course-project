#include <algorithm>
#include <iostream>
#include <vector>

int main(int argc, char *argv[]) {
    std::vector<int> nums;

    for (int i = 1; i < argc; i++) {
        nums.push_back(std::stoi(argv[i]));
    }

    std::sort(nums.begin(), nums.end());

    for (std::size_t i = 0; i < nums.size(); i++) {
        if (i > 0) {
            std::cout << " ";
        }
        std::cout << nums[i];
    }
    std::cout << std::endl;

    return 0;
}
