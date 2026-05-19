import java.util.List;

public class ResultsExporter {

    public static String toCsv(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("student_id,status,per_test_results,exit_code,error_message\n");

        if (project == null || project.getSubmissions() == null) {
            return sb.toString();
        }

        for (StudentZipSubmission submission : project.getSubmissions()) {
            Result result = submission.getResult();
            List<PerTestResult> perTestResults = submission.getPerTestResults();

            appendCsvRow(sb,
                    submission.getStudentId(),
                    statusName(result),
                    summarizePerTestResults(perTestResults),
                    summarizeExitCodes(perTestResults),
                    collectErrorMessage(result, perTestResults)
            );
        }

        return sb.toString();
    }

    public static String toJson(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"project\": ").append(JsonUtil.encodeString(project == null ? "" : project.getName())).append(",\n");
        sb.append("  \"results\": [\n");

        List<StudentZipSubmission> submissions = project == null || project.getSubmissions() == null
                ? java.util.Collections.emptyList()
                : project.getSubmissions();
        for (int i = 0; i < submissions.size(); i++) {
            StudentZipSubmission submission = submissions.get(i);
            Result result = submission.getResult();
            List<PerTestResult> perTestResults = submission.getPerTestResults();

            sb.append("    {\n");
            appendJsonField(sb, "student_id", submission.getStudentId(), 6, true);
            appendJsonField(sb, "status", statusName(result), 6, true);
            appendJsonField(sb, "per_test_results", summarizePerTestResults(perTestResults), 6, true);
            appendJsonField(sb, "exit_code", summarizeExitCodes(perTestResults), 6, true);
            appendJsonField(sb, "error_message", collectErrorMessage(result, perTestResults), 6, true);
            sb.append("      \"tests\": [\n");

            for (int j = 0; j < perTestResults.size(); j++) {
                PerTestResult perTestResult = perTestResults.get(j);
                sb.append("        {\n");
                appendJsonNumberField(sb, "test_case_id", perTestResult.getTestCaseId(), 10, true);
                appendJsonField(sb, "status", perTestResult.getStatus() == null ? "PENDING" : perTestResult.getStatus().name(), 10, true);
                appendJsonNumberField(sb, "exit_code", perTestResult.getExitCode(), 10, true);
                appendJsonField(sb, "actual_output", perTestResult.getActualOutput(), 10, true);
                appendJsonField(sb, "error_message", perTestResult.getErrorMessage(), 10, false);
                sb.append("        }");
                if (j < perTestResults.size() - 1) sb.append(',');
                sb.append('\n');
            }

            sb.append("      ]\n");
            sb.append("    }");
            if (i < submissions.size() - 1) sb.append(',');
            sb.append('\n');
        }

        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String statusName(Result result) {
        return result == null || result.getStatus() == null ? "PENDING" : result.getStatus().name();
    }

    private static String summarizePerTestResults(List<PerTestResult> perTestResults) {
        if (perTestResults == null || perTestResults.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < perTestResults.size(); i++) {
            PerTestResult perTestResult = perTestResults.get(i);
            if (i > 0) sb.append("; ");
            sb.append("test ").append(i + 1)
                    .append(" (case ").append(perTestResult.getTestCaseId()).append("): ")
                    .append(perTestResult.getStatus() == null ? "PENDING" : perTestResult.getStatus().name());
        }
        return sb.toString();
    }

    private static String summarizeExitCodes(List<PerTestResult> perTestResults) {
        if (perTestResults == null || perTestResults.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < perTestResults.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(perTestResults.get(i).getExitCode());
        }
        return sb.toString();
    }

    private static String collectErrorMessage(Result result, List<PerTestResult> perTestResults) {
        StringBuilder sb = new StringBuilder();
        if (result != null && result.getErrorMessage() != null && !result.getErrorMessage().isBlank()) {
            sb.append(result.getErrorMessage().trim());
        }

        if (perTestResults != null) {
            for (PerTestResult perTestResult : perTestResults) {
                String error = perTestResult.getErrorMessage();
                if (error != null && !error.isBlank()) {
                    if (sb.length() > 0) sb.append("; ");
                    sb.append("case ").append(perTestResult.getTestCaseId()).append(": ").append(error.trim());
                }
            }
        }

        return sb.toString();
    }

    private static void appendCsvRow(StringBuilder sb, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(csvValue(values[i]));
        }
        sb.append('\n');
    }

    private static String csvValue(String value) {
        if (value == null) return "";
        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static void appendJsonField(StringBuilder sb, String key, String value, int spaces, boolean comma) {
        sb.append(" ".repeat(spaces))
                .append(JsonUtil.encodeString(key))
                .append(": ")
                .append(JsonUtil.encodeString(value == null ? "" : value));
        if (comma) sb.append(',');
        sb.append('\n');
    }

    private static void appendJsonNumberField(StringBuilder sb, String key, long value, int spaces, boolean comma) {
        sb.append(" ".repeat(spaces))
                .append(JsonUtil.encodeString(key))
                .append(": ")
                .append(value);
        if (comma) sb.append(',');
        sb.append('\n');
    }
}
