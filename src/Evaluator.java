import java.io.File;

public class Evaluator {

    private final CommandExecutor executor;

    public Evaluator() {
        this.executor = new CommandExecutor();
    }

    public Evaluator(CommandExecutor executor) {
        this.executor = executor;
    }

    public EvaluationResult evaluate(StudentZipSubmission submission,
                                     Configuration config,
                                     TestCase testCase) throws EvaluatorException {
        validateInputs(submission, config, testCase);

        File workingDirectory = submission.getExtractedFolder();

        CommandResult compileResult = executor.execute(
                config.getCompileCommand(),
                workingDirectory
        );

        if (!compileResult.isSuccess()) {
            return new EvaluationResult(
                    false,
                    false,
                    compileResult.getExitCode(),
                    compileResult.getOutput(),
                    compileResult.getError(),
                    0,
                    "",
                    ""
            );
        }

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
        return base + " " + input;
    }

    private void validateInputs(StudentZipSubmission submission,
                                Configuration config,
                                TestCase testCase) throws EvaluatorException {
        if (submission == null) {
            throw new EvaluatorException("Submission cannot be null");
        }
        if (config == null) {
            throw new EvaluatorException("Configuration cannot be null");
        }
        if (testCase == null) {
            throw new EvaluatorException("TestCase cannot be null");
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

        String compileCommand = config.getCompileCommand();
        if (compileCommand == null || compileCommand.trim().isEmpty()) {
            throw new EvaluatorException("Configuration has no compile command");
        }

        String runCommand = config.getRunCommand();
        if (runCommand == null || runCommand.trim().isEmpty()) {
            throw new EvaluatorException("Configuration has no run command");
        }
    }
}
