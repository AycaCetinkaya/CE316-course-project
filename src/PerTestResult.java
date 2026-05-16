public class PerTestResult {

    private final long testCaseId;
    private final Status status;
    private final String actualOutput;
    private final String errorMessage;
    private final int exitCode;

    public PerTestResult(long testCaseId,
                         Status status,
                         String actualOutput,
                         String errorMessage,
                         int exitCode) {
        this.testCaseId = testCaseId;
        this.status = status;
        this.actualOutput = actualOutput == null ? "" : actualOutput;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
        this.exitCode = exitCode;
    }

    public long getTestCaseId() { return testCaseId; }
    public Status getStatus() { return status; }
    public String getActualOutput() { return actualOutput; }
    public String getErrorMessage() { return errorMessage; }
    public int getExitCode() { return exitCode; }
}
