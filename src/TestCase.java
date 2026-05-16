public class TestCase {
    private long id;
    private String input;
    private String expectedOutput;

    public TestCase(String input, String expectedOutput) {
        this(-1, input, expectedOutput);
    }

    public TestCase(long id, String input, String expectedOutput) {
        this.id = id;
        this.input = input;
        this.expectedOutput = expectedOutput;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
}
