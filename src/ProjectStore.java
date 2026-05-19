import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectStore {

    public static final String FILE_EXTENSION = "iaeproj";
    public static final String FORMAT_TAG = "iaeproj-v1";

    public static class Bundle {
        public final Project project;
        public final String inputFilePath;
        public final String expectedOutputFilePath;
        public final String submissionsFolderPath;

        public Bundle(Project project,
                      String inputFilePath,
                      String expectedOutputFilePath,
                      String submissionsFolderPath) {
            this.project = project;
            this.inputFilePath = inputFilePath == null ? "" : inputFilePath;
            this.expectedOutputFilePath = expectedOutputFilePath == null ? "" : expectedOutputFilePath;
            this.submissionsFolderPath = submissionsFolderPath == null ? "" : submissionsFolderPath;
        }
    }

    public void saveTo(File file,
                       Project project,
                       String inputFilePath,
                       String expectedOutputFilePath,
                       String submissionsFolderPath) throws IOException {
        if (file == null) throw new IllegalArgumentException("Target file cannot be null");
        if (project == null) throw new IllegalArgumentException("Project cannot be null");

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        long lastModified = project.getLastModified() > 0
                ? project.getLastModified()
                : System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format\": ").append(JsonUtil.encodeString(FORMAT_TAG)).append(",\n");
        sb.append("  \"name\": ").append(JsonUtil.encodeString(project.getName())).append(",\n");
        sb.append("  \"lastModified\": ").append(lastModified).append(",\n");

        Configuration config = project.getConfiguration();
        sb.append("  \"configuration\": ")
          .append(config == null
                  ? new Configuration("AUTO", "AUTO", "", "", "", "").toJson()
                  : config.toJson())
          .append(",\n");

        sb.append("  \"filePaths\": {");
        sb.append("\"inputFilePath\":").append(JsonUtil.encodeString(safe(inputFilePath))).append(",");
        sb.append("\"expectedOutputFilePath\":").append(JsonUtil.encodeString(safe(expectedOutputFilePath))).append(",");
        sb.append("\"submissionsFolderPath\":").append(JsonUtil.encodeString(safe(submissionsFolderPath)));
        sb.append("},\n");

        sb.append("  \"testCases\": [");
        List<TestCase> testCases = project.getTestCases() == null
                ? new ArrayList<>() : project.getTestCases();
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            sb.append("{");
            sb.append("\"input\":").append(JsonUtil.encodeString(tc.getInput() == null ? "" : tc.getInput())).append(",");
            sb.append("\"expectedOutput\":").append(JsonUtil.encodeString(tc.getExpectedOutput() == null ? "" : tc.getExpectedOutput()));
            sb.append("}");
            if (i < testCases.size() - 1) sb.append(",");
        }
        sb.append("],\n");

        sb.append("  \"submissions\": [");
        List<StudentZipSubmission> submissions = project.getSubmissions() == null
                ? new ArrayList<>() : project.getSubmissions();
        for (int i = 0; i < submissions.size(); i++) {
            sb.append(submissionToJson(submissions.get(i), testCases));
            if (i < submissions.size() - 1) sb.append(",");
        }
        sb.append("]\n");
        sb.append("}\n");

        Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String submissionToJson(StudentZipSubmission sub, List<TestCase> testCases) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"studentId\":").append(JsonUtil.encodeString(sub.getStudentId() == null ? "" : sub.getStudentId())).append(",");
        sb.append("\"zipPath\":").append(JsonUtil.encodeString(
                sub.getZipFile() == null ? "" : sub.getZipFile().getAbsolutePath())).append(",");
        sb.append("\"extractedPath\":").append(JsonUtil.encodeString(
                sub.getExtractedFolder() == null ? "" : sub.getExtractedFolder().getAbsolutePath())).append(",");

        Result result = sub.getResult();
        sb.append("\"status\":").append(JsonUtil.encodeString(
                result == null || result.getStatus() == null
                        ? "RUNTIME_ERROR" : result.getStatus().name())).append(",");
        sb.append("\"output\":").append(JsonUtil.encodeString(
                result == null || result.getOutput() == null ? "" : result.getOutput())).append(",");
        sb.append("\"errorMessage\":").append(JsonUtil.encodeString(
                result == null || result.getErrorMessage() == null ? "" : result.getErrorMessage())).append(",");

        sb.append("\"perTestResults\":[");
        List<PerTestResult> perTest = sub.getPerTestResults();
        for (int j = 0; j < perTest.size(); j++) {
            PerTestResult ptr = perTest.get(j);
            int idx = indexOfTestCaseId(testCases, ptr.getTestCaseId());
            sb.append("{");
            sb.append("\"testCaseIndex\":").append(idx).append(",");
            sb.append("\"status\":").append(JsonUtil.encodeString(
                    ptr.getStatus() == null ? "RUNTIME_ERROR" : ptr.getStatus().name())).append(",");
            sb.append("\"actualOutput\":").append(JsonUtil.encodeString(ptr.getActualOutput())).append(",");
            sb.append("\"errorMessage\":").append(JsonUtil.encodeString(ptr.getErrorMessage())).append(",");
            sb.append("\"exitCode\":").append(ptr.getExitCode());
            sb.append("}");
            if (j < perTest.size() - 1) sb.append(",");
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private int indexOfTestCaseId(List<TestCase> testCases, long id) {
        for (int i = 0; i < testCases.size(); i++) {
            if (testCases.get(i).getId() == id) return i;
        }
        return -1;
    }

    public Bundle loadFrom(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("Project file not found: " + (file == null ? "null" : file.getAbsolutePath()));
        }

        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        if (content.trim().isEmpty()) {
            throw new IOException("Project file is empty: " + file.getAbsolutePath());
        }

        Map<String, String> root = parseObjectDeep(content);

        String name = unquote(root.get("name"));
        if (name == null || name.isEmpty()) {
            throw new IOException("Project file missing 'name'.");
        }

        long lastModified = 0L;
        try {
            String lm = root.get("lastModified");
            if (lm != null && !lm.isEmpty()) lastModified = Long.parseLong(lm.trim());
        } catch (NumberFormatException ignored) {
        }

        Configuration config = root.containsKey("configuration")
                ? Configuration.fromJson(root.get("configuration"))
                : new Configuration("AUTO", "AUTO", "", "", "", "");

        Map<String, String> filePaths = root.containsKey("filePaths")
                ? parseObjectDeep(root.get("filePaths"))
                : new LinkedHashMap<>();
        String inputFilePath = unquote(filePaths.get("inputFilePath"));
        String expectedOutputFilePath = unquote(filePaths.get("expectedOutputFilePath"));
        String submissionsFolderPath = unquote(filePaths.get("submissionsFolderPath"));

        List<TestCase> testCases = new ArrayList<>();
        if (root.containsKey("testCases")) {
            for (String tcJson : JsonUtil.splitTopLevelObjects(root.get("testCases"))) {
                Map<String, String> m = parseObjectDeep(tcJson);
                testCases.add(new TestCase(
                        unquote(m.get("input")),
                        unquote(m.get("expectedOutput"))
                ));
            }
        }

        List<StudentZipSubmission> submissions = new ArrayList<>();
        if (root.containsKey("submissions")) {
            for (String subJson : JsonUtil.splitTopLevelObjects(root.get("submissions"))) {
                submissions.add(submissionFromJson(subJson, testCases));
            }
        }

        Project project = new Project(name, config, submissions, testCases);
        project.setLastModified(lastModified);
        return new Bundle(project, inputFilePath, expectedOutputFilePath, submissionsFolderPath);
    }

    private StudentZipSubmission submissionFromJson(String json, List<TestCase> testCases) {
        Map<String, String> m = parseObjectDeep(json);
        String studentId = unquote(m.get("studentId"));
        String zipPath = unquote(m.get("zipPath"));
        String extractedPath = unquote(m.get("extractedPath"));
        String statusStr = unquote(m.get("status"));
        String output = unquote(m.get("output"));
        String errorMessage = unquote(m.get("errorMessage"));

        StudentZipSubmission sub = new StudentZipSubmission(
                studentId == null ? "" : studentId,
                new File(zipPath == null ? "" : zipPath)
        );
        if (extractedPath != null && !extractedPath.isEmpty()) {
            sub.setExtractedFolder(new File(extractedPath));
        }

        Status status;
        try {
            status = Status.valueOf(statusStr);
        } catch (Exception e) {
            status = Status.RUNTIME_ERROR;
        }
        sub.setResult(new Result(status, output, errorMessage));

        List<PerTestResult> perTest = new ArrayList<>();
        if (m.containsKey("perTestResults")) {
            for (String ptrJson : JsonUtil.splitTopLevelObjects(m.get("perTestResults"))) {
                Map<String, String> p = parseObjectDeep(ptrJson);
                long testCaseIndex = parseIntSafe(p.get("testCaseIndex"), -1);

                Status ptrStatus;
                try {
                    ptrStatus = Status.valueOf(unquote(p.get("status")));
                } catch (Exception e) {
                    ptrStatus = Status.RUNTIME_ERROR;
                }

                perTest.add(new PerTestResult(
                        testCaseIndex,
                        ptrStatus,
                        unquote(p.get("actualOutput")),
                        unquote(p.get("errorMessage")),
                        parseIntSafe(p.get("exitCode"), 0)
                ));
            }
        }
        sub.setPerTestResults(perTest);
        return sub;
    }

    private static int parseIntSafe(String s, int fallback) {
        if (s == null) return fallback;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String unquote(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return unescape(s.substring(1, s.length() - 1));
        }
        return s;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            try {
                                sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                                i += 5;
                            } catch (NumberFormatException ex) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                        break;
                    default: sb.append(n); i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static Map<String, String> parseObjectDeep(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        if (json == null) return out;

        int len = json.length();
        int i = 0;

        while (i < len && json.charAt(i) != '{') i++;
        if (i >= len) return out;
        i++;

        while (i < len) {
            i = skipWs(json, i);
            if (i >= len || json.charAt(i) == '}') break;
            if (json.charAt(i) == ',') { i++; continue; }
            if (json.charAt(i) != '"') { i++; continue; }

            int keyStart = i;
            i = skipString(json, i);
            String key = unescape(json.substring(keyStart + 1, i - 1));

            i = skipWs(json, i);
            if (i < len && json.charAt(i) == ':') i++;
            i = skipWs(json, i);
            if (i >= len) break;

            int valStart = i;
            i = skipValue(json, i);
            out.put(key, json.substring(valStart, i).trim());
        }

        return out;
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static int skipString(String s, int i) {
        int len = s.length();
        i++;
        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len) { i += 2; continue; }
            if (c == '"') return i + 1;
            i++;
        }
        return i;
    }

    private static int skipValue(String s, int i) {
        int len = s.length();
        if (i >= len) return i;
        char c = s.charAt(i);
        if (c == '"') return skipString(s, i);
        if (c == '{' || c == '[') {
            int depth = 0;
            boolean inStr = false;
            boolean esc = false;
            for (; i < len; i++) {
                char ch = s.charAt(i);
                if (esc) { esc = false; continue; }
                if (inStr) {
                    if (ch == '\\') esc = true;
                    else if (ch == '"') inStr = false;
                    continue;
                }
                if (ch == '"') { inStr = true; continue; }
                if (ch == '{' || ch == '[') depth++;
                else if (ch == '}' || ch == ']') {
                    depth--;
                    if (depth == 0) return i + 1;
                }
            }
            return i;
        }
        while (i < len && s.charAt(i) != ',' && s.charAt(i) != '}' && s.charAt(i) != ']') i++;
        return i;
    }
}
