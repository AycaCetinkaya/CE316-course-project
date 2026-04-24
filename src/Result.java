public class Result {
    private Status status;
    private String output;
    private String errorMessage;

    public Result(Status status, String output, String errorMessage) {
        this.status = status;
        this.output = output;
        this.errorMessage = errorMessage;
    }

    public Status getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}