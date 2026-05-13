import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectRunnerService {

    public Project runProject(String projectName,
                              File submissionsDir,
                              List<TestCase> testCases,
                              Language selectedLanguage) throws ZipServiceException {

        // --- Database Setup (Version A) ---
        DatabaseManager db = new DatabaseManager();
        try {
            db.connect();
            db.initSchema();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ZipService zipService = new ZipService();
        List<StudentZipSubmission> submissions = zipService.extractAll(submissionsDir);

        EvaluationService evaluationService = new EvaluationService();
        long projectId = -1;

        try {
            for (StudentZipSubmission submission : submissions) {
                if (submission.getResult() != null &&
                        submission.getResult().getStatus() == Status.EXTRACTION_ERROR) {
                    continue;
                }

                Language actualLanguage = selectedLanguage == Language.AUTO
                        ? detectLanguage(submission.getExtractedFolder())
                        : selectedLanguage;

                Configuration config = getConfig(actualLanguage, submission.getExtractedFolder());

                if (config == null) {
                    submission.setResult(new Result(
                            Status.RUNTIME_ERROR,
                            "",
                            "Unsupported language or no configuration found."
                    ));
                    continue;
                }

                String mainFile = findMainFileName(actualLanguage, submission.getExtractedFolder(), config);

                if (mainFile == null) {
                    submission.setResult(new Result(Status.RUNTIME_ERROR, "", "Main file not found."));
                    continue;
                }

                Configuration studentConfig = new Configuration(
                        config.getName(),
                        config.getLanguage(),
                        config.getCompileCommand().replace("$MAIN", mainFile),
                        config.getRunCommand().replace("$MAIN", mainFile),
                        config.getSourceExtension(),
                        config.getEntryPointPattern()
                );


                if (projectId == -1) {
                    try {
                        long configId = db.upsertConfiguration(config);
                        projectId = db.upsertProject(projectName, configId);

                        db.deleteTestCasesForProject(projectId);
                        for (TestCase tc : testCases) {
                            db.insertTestCase(projectId, tc);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                List<StudentZipSubmission> single = new ArrayList<>();
                single.add(submission);

                Project singleProject = new Project(projectName, studentConfig, single, testCases);
                evaluationService.evaluateProject(singleProject);

                try {
                    db.upsertSubmission(projectId, submission);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            db.disconnect();
        }

        Project resultProject = new Project(
                projectName,
                new Configuration(selectedLanguage.toString(), "", ""),
                submissions,
                testCases
        );
        resultProject.setId(projectId);
        return resultProject;
    }

    private Language detectLanguage(File folder) {
        if (folder == null || !folder.exists()) return Language.UNKNOWN;

        for (File file : getAllFiles(folder)) {
            String name = file.getName().toLowerCase();

            if (name.endsWith(".c")) return Language.C;
            if (name.endsWith(".java")) return Language.JAVA;
            if (name.endsWith(".py")) return Language.PYTHON;
            if (name.endsWith(".hs") || name.endsWith(".lhs")) return Language.HASKELL;
        }

        return Language.UNKNOWN;
    }

    private Configuration getConfig(Language lang, File folder) {
        ConfigStore store = new ConfigStore();
        List<Configuration> configs = store.loadAll();

        for (Configuration config : configs) {
            if (config.getLanguage().equalsIgnoreCase(lang.name())) {
                return config;
            }
        }
        return null;
    }

    private List<File> getAllFiles(File folder) {
        List<File> files = new ArrayList<>();
        File[] items = folder.listFiles();

        if (items == null) return files;

        for (File item : items) {
            if (item.isFile()) {
                files.add(item);
            } else if (item.isDirectory()) {
                files.addAll(getAllFiles(item));
            }
        }

        return files;
    }

    private String findMainClass(File folder) {
        for (File file : getAllFiles(folder)) {
            if (file.getName().endsWith(".java")) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    if (content.contains("public static void main") ||
                            content.matches("(?s).*public\\s+static\\s+void\\s+main\\s*\\(.*")) {
                        return file.getName().replace(".java", "");
                    }
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private String findMainPythonFile(File folder) {
        File fallback = null;
        for (File file : getAllFiles(folder)) {
            String name = file.getName();
            if (!name.endsWith(".py")) continue;
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                if (content.contains("if __name__ == \"__main__\"") ||
                        content.contains("if __name__ == '__main__'")) {
                    return name;
                }
                if (name.equals("main.py")) fallback = file;
            } catch (Exception ignored) { }
        }
        if (fallback != null) return fallback.getName();
        for (File file : getAllFiles(folder)) {
            if (file.getName().endsWith(".py")) return file.getName();
        }
        return null;
    }

    private String findMainHaskellFile(File folder) {
        File fallback = null;
        for (File file : getAllFiles(folder)) {
            String name = file.getName();
            if (!name.endsWith(".hs") && !name.endsWith(".lhs")) continue;
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                if (content.matches("(?s).*\\bmain\\s*[:=].*")) {
                    return name;
                }
                if (name.equalsIgnoreCase("Main.hs") || name.equalsIgnoreCase("main.hs")) {
                    fallback = file;
                }
            } catch (Exception ignored) { }
        }
        if (fallback != null) return fallback.getName();
        for (File file : getAllFiles(folder)) {
            String name = file.getName();
            if (name.endsWith(".hs") || name.endsWith(".lhs")) return name;
        }
        return null;
    }

    private String findMainFileName(Language lang, File folder, Configuration config) {
        if (lang == Language.JAVA) return findMainClass(folder);
        if (lang == Language.PYTHON) return findMainPythonFile(folder);
        if (lang == Language.HASKELL) return findMainHaskellFile(folder);
        if (lang == Language.C) {
            for (File f : getAllFiles(folder)) if (f.getName().endsWith(".c")) return f.getName();
        }
        String ext = config.getSourceExtension();
        if (ext != null && !ext.isEmpty()) {
            for (File f : getAllFiles(folder)) {
                if (f.getName().endsWith(ext)) {
                    return f.getName();
                }
            }
        }
        return null;
    }
}