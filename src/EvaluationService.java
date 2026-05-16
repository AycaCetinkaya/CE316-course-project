import java.util.ArrayList;
import java.util.List;

public class EvaluationService {

    private final Evaluator evaluator;
    private final ExactMatchComparator comparator;

    public EvaluationService() {
        this.evaluator = new Evaluator();
        this.comparator = new ExactMatchComparator();
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

        for (int i = 0; i < testCases.size(); i++) {

            TestCase testCase = testCases.get(i);

            try {
                EvaluationResult evalResult = evaluator.run(
                        submission,
                        config,
                        testCase,
                        compileResult
                );

                Result testResult = comparator.toResult(
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
                        .append(System.lineSeparator());

                if (testResult.getStatus() != Status.SUCCESS) {
                    return new Result(
                            testResult.getStatus(),
                            allOutputs.toString(),
                            "Failed at test case " + (i + 1) + ": " + testResult.getErrorMessage()
                    );
                }

            } catch (EvaluatorException e) {
                perTestResults.add(new PerTestResult(
                        testCase.getId(),
                        Status.RUNTIME_ERROR,
                        "",
                        e.getMessage(),
                        -1
                ));
                return new Result(
                        Status.RUNTIME_ERROR,
                        allOutputs.toString(),
                        "Evaluator run error at test case " + (i + 1) + ": " + e.getMessage()
                );
            }
        }

        return new Result(
                Status.SUCCESS,
                allOutputs.toString(),
                ""
        );
    }
}