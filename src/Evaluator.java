import java.io.File;

public class Evaluator {

    private final CommandExecutor executor;

    public Evaluator() {
        this.executor = new CommandExecutor();
    }

    public Evaluator(CommandExecutor executor) {
        this.executor = executor;
    }

    public Evaluator(long timeoutSeconds) {
        this.executor = new CommandExecutor(timeoutSeconds);
    }

    public CommandResult compile(StudentZipSubmission submission,
                                 Configuration config) throws EvaluatorException {

        validateSubmissionAndConfig(submission, config);

        String compileCommand = config.getCompileCommand();
        if (compileCommand == null || compileCommand.trim().isEmpty()) {
            return new CommandResult(0, "", "");
        }

        File workingDirectory = submission.getExtractedFolder();

        return executor.execute(
                compileCommand,
                workingDirectory
        );
    }

    public EvaluationResult run(StudentZipSubmission submission,
                                Configuration config,
                                TestCase testCase,
                                CommandResult compileResult) throws EvaluatorException {

        validateSubmissionAndConfig(submission, config);

        if (testCase == null) {
            throw new EvaluatorException("TestCase cannot be null");
        }

        File workingDirectory = submission.getExtractedFolder();

        String runCommand = buildRunCommand(config, testCase);
        CommandResult runResult = executor.execute(runCommand, workingDirectory);

        return new EvaluationResult(
                true,
                true,
                compileResult.getExitCode(),
                compileResult.getOutput(),
                compileResult.getError(),
                runResult.getExitCode(),
                runResult.getOutput(),
                runResult.getError()
        );
    }

    private String buildRunCommand(Configuration config, TestCase testCase) {
        String base = config.getRunCommand();
        String input = testCase.getInput();

        if (input == null || input.trim().isEmpty()) {
            return base;
        }

        if (base.contains("$INPUT")) {
            return base.replace("$INPUT", input);
        }

        return base + " " + input;
    }

    private void validateSubmissionAndConfig(StudentZipSubmission submission,
                                             Configuration config) throws EvaluatorException {

        if (submission == null) {
            throw new EvaluatorException("Submission cannot be null");
        }

        if (config == null) {
            throw new EvaluatorException("Configuration cannot be null");
        }

        File workingDirectory = submission.getExtractedFolder();

        if (workingDirectory == null) {
            throw new EvaluatorException(
                    "Submission has no extracted folder: " + submission.getStudentId()
            );
        }

        if (!workingDirectory.exists() || !workingDirectory.isDirectory()) {
            throw new EvaluatorException(
                    "Extracted folder does not exist: " + workingDirectory.getAbsolutePath()
            );
        }

        String runCommand = config.getRunCommand();

        if (runCommand == null || runCommand.trim().isEmpty()) {
            throw new EvaluatorException("Configuration has no run command");
        }
    }
}