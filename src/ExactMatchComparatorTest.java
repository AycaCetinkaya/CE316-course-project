// this class won't be included in the final state of the program.

public class ExactMatchComparatorTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        ExactMatchComparator comparator = new ExactMatchComparator();

        test("exactMatch", comparator.compare("hello", "hello"));
        test("trailingNewline", comparator.compare("hello\n", "hello"));
        test("trailingSpaces", comparator.compare("hello   ", "hello"));
        test("emptyBoth", comparator.compare("", ""));
        test("mismatch", !comparator.compare("1 2 3", "3 2 1"));
        test("multilineMatch", comparator.compare("a\nb\nc", "a\nb\nc"));
        test("windowsLineEndings", comparator.compare("hello\r\nworld", "hello\nworld"));

        // toResult tests
        EvaluationResult compileErr = new EvaluationResult(false, false, 1, "", "compile error", 0, "", "");
        test("compileError", comparator.toResult(compileErr, "any").getStatus() == Status.COMPILE_ERROR);

        EvaluationResult runtimeErr = new EvaluationResult(true, true, 0, "", "", 1, "", "crash");
        test("runtimeError", comparator.toResult(runtimeErr, "any").getStatus() == Status.RUNTIME_ERROR);

        EvaluationResult wrongOut = new EvaluationResult(true, true, 0, "", "", 0, "wrong", "");
        test("wrongOutput", comparator.toResult(wrongOut, "right").getStatus() == Status.WRONG_OUTPUT);

        EvaluationResult success = new EvaluationResult(true, true, 0, "", "", 0, "correct\n", "");
        test("success", comparator.toResult(success, "correct").getStatus() == Status.SUCCESS);

        System.out.println("\n" + passed + " passed, " + failed + " failed.");
    }

    private static void test(String name, boolean condition) {
        if (condition) {
            System.out.println("PASS: " + name);
            passed++;
        } else {
            System.out.println("FAIL: " + name);
            failed++;
        }
    }
}