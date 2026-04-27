public class EvaluationResult {

    private final boolean compiled;
    private final boolean ran;
    private final int compileExitCode;
    private final String compileStdout;
    private final String compileStderr;
    private final int runExitCode;
    private final String runStdout;
    private final String runStderr;

    public EvaluationResult(boolean compiled,
                            boolean ran,
                            int compileExitCode,
                            String compileStdout,
                            String compileStderr,
                            int runExitCode,
                            String runStdout,
                            String runStderr) {
        this.compiled = compiled;
        this.ran = ran;
        this.compileExitCode = compileExitCode;
        this.compileStdout = compileStdout;
        this.compileStderr = compileStderr;
        this.runExitCode = runExitCode;
        this.runStdout = runStdout;
        this.runStderr = runStderr;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public boolean isRan() {
        return ran;
    }

    public int getCompileExitCode() {
        return compileExitCode;
    }

    public String getCompileStdout() {
        return compileStdout;
    }

    public String getCompileStderr() {
        return compileStderr;
    }

    public int getRunExitCode() {
        return runExitCode;
    }

    public String getRunStdout() {
        return runStdout;
    }

    public String getRunStderr() {
        return runStderr;
    }
}
