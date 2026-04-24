public class TestCase {
    private String input; // optional
    private String expectedOutput;

    public TestCase(String input, String expectedOutput) {
        this.input = input;
        this.expectedOutput = expectedOutput;
    }

    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
}