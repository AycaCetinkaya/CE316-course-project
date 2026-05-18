import sys


if __name__ == "__main__":
    nums = sorted(int(arg) for arg in sys.argv[1:])
    print(" ".join(str(num) for num in nums))
