import sys

nums = [int(x) for x in sys.argv[1:]]
nums.sort()

print(" ".join(str(x) for x in nums))