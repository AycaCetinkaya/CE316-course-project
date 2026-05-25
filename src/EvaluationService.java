import java.util.ArrayList;
import java.util.List;

public class EvaluationService {

    private final Evaluator evaluator;
    private final OutputComparator comparator;

    public EvaluationService() {
        this(new ExactMatchComparator());
    }

    public EvaluationService(OutputComparator comparator) {
        this.evaluator = new Evaluator();
        this.comparator = comparator == null ? new ExactMatchComparator() : comparator;
    }

    public EvaluationService(OutputComparator comparator, long timeoutSeconds) {
        this.evaluator = new Evaluator(timeoutSeconds);
        this.comparator = comparator == null ? new ExactMatchComparator() : comparator;
    }

    public void evaluateProject(Project project) {

        Configuration config = project.getConfiguration();
        List<TestCase> testCases = project.getTestCases();

        for (StudentZipSubmission submission : project.getSubmissions()) {
            evaluate(submission, config, testCases);
        }
    }

    public void evaluate(StudentZipSubmission submission,
                         Configuration config,
                         List<TestCase> testCases) {

        System.out.println("\nEvaluating: " + submission.getStudentId());

        if (submission.getResult() != null &&
                submission.getResult().getStatus() == Status.EXTRACTION_ERROR) {
            System.out.println("  Skipped due to extraction error.");
            submission.setPerTestResults(new ArrayList<>());
            return;
        }

        List<PerTestResult> perTestResults = new ArrayList<>();
        Result finalResult = evaluateSubmission(submission, config, testCases, perTestResults);

        submission.setResult(finalResult);
        submission.setPerTestResults(perTestResults);

        System.out.println("  Final Status: " + finalResult.getStatus());
    }

    private Result evaluateSubmission(StudentZipSubmission submission,
                                      Configuration config,
                                      List<TestCase> testCases,
                                      List<PerTestResult> perTestResults) {

        if (testCases == null || testCases.isEmpty()) {
            return new Result(
                    Status.RUNTIME_ERROR,
                    "",
                    "No test cases found."
            );
        }

        CommandResult compileResult;

        try {
            compileResult = evaluator.compile(submission, config);
        } catch (EvaluatorException e) {
            return new Result(
                    Status.RUNTIME_ERROR,
                    "",
                    "Evaluator compile error: " + e.getMessage()
            );
        }

        if (!compileResult.isSuccess()) {
            for (TestCase testCase : testCases) {
                perTestResults.add(new PerTestResult(
                        testCase.getId(),
                        Status.COMPILE_ERROR,
                        compileResult.getOutput(),
                        compileResult.getError(),
                        compileResult.getExitCode()
                ));
            }

            return new Result(
                    Status.COMPILE_ERROR,
                    compileResult.getOutput(),
                    compileResult.getError()
            );
        }

        StringBuilder allOutputs = new StringBuilder();
        StringBuilder allErrors = new StringBuilder();

        Status finalStatus = Status.SUCCESS;

        for (int i = 0; i < testCases.size(); i++) {

            TestCase testCase = testCases.get(i);

            try {
                EvaluationResult evalResult = evaluator.run(
                        submission,
                        config,
                        testCase,
                        compileResult
                );

                Result testResult = toResult(
                        evalResult,
                        testCase.getExpectedOutput()
                );

                perTestResults.add(new PerTestResult(
                        testCase.getId(),
                        testResult.getStatus(),
                        evalResult.getRunStdout(),
                        evalResult.getRunStderr(),
                        evalResult.getRunExitCode()
                ));

                allOutputs.append("Test Case ")
                        .append(i + 1)
                        .append(" -> ")
                        .append(testResult.getStatus())
                        .append(System.lineSeparator());

                allOutputs.append("Output: ")
                        .append(testResult.getOutput())
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());

                if (testResult.getStatus() != Status.SUCCESS) {
                    finalStatus = chooseWorseStatus(finalStatus, testResult.getStatus());

                    allErrors.append("Test Case ")
                            .append(i + 1)
                            .append(": ")
                            .append(testResult.getErrorMessage())
                            .append(System.lineSeparator());
                }

            } catch (EvaluatorException e) {
                perTestResults.add(new PerTestResult(
                        testCase.getId(),
                        Status.RUNTIME_ERROR,
                        "",
                        e.getMessage(),
                        -1
                ));

                finalStatus = chooseWorseStatus(finalStatus, Status.RUNTIME_ERROR);

                allOutputs.append("Test Case ")
                        .append(i + 1)
                        .append(" -> ")
                        .append(Status.RUNTIME_ERROR)
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());

                allErrors.append("Evaluator run error at test case ")
                        .append(i + 1)
                        .append(": ")
                        .append(e.getMessage())
                        .append(System.lineSeparator());
            }
        }

        return new Result(
                finalStatus,
                allOutputs.toString(),
                allErrors.toString()
        );
    }

    private Result toResult(EvaluationResult evalResult, String expectedOutput) {
        if (!evalResult.isCompiled()) {
            return new Result(
                    Status.COMPILE_ERROR,
                    "",
                    evalResult.getCompileStderr()
            );
        }

        if (evalResult.getRunExitCode() != 0) {
            return new Result(
                    Status.RUNTIME_ERROR,
                    evalResult.getRunStdout(),
                    evalResult.getRunStderr()
            );
        }

        boolean match = comparator.compare(evalResult.getRunStdout(), expectedOutput);

        if (!match) {
            return new Result(
                    Status.WRONG_OUTPUT,
                    evalResult.getRunStdout(),
                    ""
            );
        }

        return new Result(
                Status.SUCCESS,
                evalResult.getRunStdout(),
                ""
        );
    }
    private Status chooseWorseStatus(Status current, Status candidate) {
        if (candidate == Status.RUNTIME_ERROR) {
            return Status.RUNTIME_ERROR;
        }

        if (candidate == Status.WRONG_OUTPUT && current == Status.SUCCESS) {
            return Status.WRONG_OUTPUT;
        }

        return current;
    }
}