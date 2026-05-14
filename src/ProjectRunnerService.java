import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectRunnerService {

    public Project runProject(String projectName,
                              File submissionsDir,
                              List<TestCase> testCases,
                              Configuration selectedConfig) throws ZipServiceException {

        DatabaseManager db = new DatabaseManager();
        long projectId = -1;
        Configuration projectConfig = selectedConfig != null
                ? selectedConfig
                : new Configuration("AUTO", "AUTO", "", "", "", "");

        try {
            db.connect();
            db.initSchema();
            projectId = db.saveProject(projectName, projectConfig, testCases);
            db.deleteSubmissionsForProject(projectId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ZipService zipService = new ZipService();
        List<StudentZipSubmission> submissions = zipService.extractAll(submissionsDir);

        EvaluationService evaluationService = new EvaluationService();

        try {
            for (StudentZipSubmission submission : submissions) {
                if (submission.getResult() != null &&
                        submission.getResult().getStatus() == Status.EXTRACTION_ERROR) {
                    try {
                        db.upsertSubmission(projectId, submission);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                Configuration config;
                Language actualLanguage = null;
                boolean customConfigSelected = selectedConfig != null;

                if (customConfigSelected) {
                    config = selectedConfig;
                } else {
                    actualLanguage = detectLanguage(submission.getExtractedFolder());
                    config = getConfig(actualLanguage, submission.getExtractedFolder());
                }

                if (config == null) {
                    submission.setResult(new Result(
                            Status.RUNTIME_ERROR,
                            "",
                            "Unsupported language or no configuration found."
                    ));
                    continue;
                }

                String mainFile = customConfigSelected
                        ? findMainFileNameByConfig(submission.getExtractedFolder(), config)
                        : findMainFileName(actualLanguage, submission.getExtractedFolder(), config);

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
                projectConfig,
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
                    return relativePath(folder, file);
                }
                if (name.equals("main.py")) fallback = file;
            } catch (Exception ignored) { }
        }
        if (fallback != null) return relativePath(folder, fallback);
        for (File file : getAllFiles(folder)) {
            if (file.getName().endsWith(".py")) return relativePath(folder, file);
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
                    return relativePath(folder, file);
                }
                if (name.equalsIgnoreCase("Main.hs") || name.equalsIgnoreCase("main.hs")) {
                    fallback = file;
                }
            } catch (Exception ignored) { }
        }
        if (fallback != null) return relativePath(folder, fallback);
        for (File file : getAllFiles(folder)) {
            String name = file.getName();
            if (name.endsWith(".hs") || name.endsWith(".lhs")) return relativePath(folder, file);
        }
        return null;
    }

    private String findMainFileName(Language lang, File folder, Configuration config) {
        if (lang == Language.JAVA) return findMainClass(folder);
        if (lang == Language.PYTHON) return findMainPythonFile(folder);
        if (lang == Language.HASKELL) return findMainHaskellFile(folder);
        if (lang == Language.C) {
            for (File f : getAllFiles(folder)) if (f.getName().endsWith(".c")) return relativePath(folder, f);
        }
        String ext = config.getSourceExtension();
        if (ext != null && !ext.isEmpty()) {
            for (File f : getAllFiles(folder)) {
                if (f.getName().endsWith(ext)) {
                    return relativePath(folder, f);
                }
            }
        }
        return null;
    }
    private String findMainFileNameByConfig(File folder, Configuration config) {
        String extension = config.getSourceExtension();
        String pattern = config.getEntryPointPattern();

        File fallback = null;

        for (File file : getAllFiles(folder)) {
            String fileName = file.getName();

            if (extension != null && !extension.isEmpty() && !fileName.endsWith(extension)) {
                continue;
            }

            if (fallback == null) {
                fallback = file;
            }

            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));

                if (pattern != null && !pattern.isEmpty()) {
                    if (content.matches("(?s).*" + pattern + ".*")) {
                        return mainToken(folder, file, config);
                    }
                }
            } catch (Exception ignored) { }
        }

        if (fallback != null) {
            return mainToken(folder, fallback, config);
        }

        return null;
    }

    private String mainToken(File folder, File file, Configuration config) {
        String language = config.getLanguage() == null ? "" : config.getLanguage();
        String extension = config.getSourceExtension() == null ? "" : config.getSourceExtension();

        if (language.equalsIgnoreCase("JAVA")) {
            String fileName = file.getName();
            return extension.isEmpty() ? fileName : fileName.replace(extension, "");
        }

        return relativePath(folder, file);
    }

    private String relativePath(File folder, File file) {
        return folder.toPath()
                .toAbsolutePath()
                .normalize()
                .relativize(file.toPath().toAbsolutePath().normalize())
                .toString()
                .replace(File.separatorChar, '/');
    }
}
