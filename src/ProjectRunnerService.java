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

                Configuration config = selectedConfig != null
                        ? selectedConfig
                        : autoDetectConfig(submission.getExtractedFolder());

                if (config == null) {
                    submission.setResult(new Result(
                            Status.RUNTIME_ERROR,
                            "",
                            "Unsupported language or no configuration found."
                    ));
                    continue;
                }

                String mainFile = findMainFileNameByConfig(submission.getExtractedFolder(), config);

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


                evaluationService.evaluate(submission, studentConfig, testCases);

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

    private Configuration autoDetectConfig(File folder) {
        if (folder == null || !folder.exists()) return null;

        ConfigStore store = new ConfigStore();
        List<Configuration> configs = store.loadAll();
        List<File> files = getAllFiles(folder);

        for (Configuration config : configs) {
            String ext = config.getSourceExtension();
            String pattern = config.getEntryPointPattern();
            if (ext == null || ext.isEmpty()) continue;

            for (File f : files) {
                if (!f.getName().toLowerCase().endsWith(ext.toLowerCase())) continue;
                if (pattern == null || pattern.isEmpty()) return config;
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                    if (content.matches("(?s).*" + pattern + ".*")) return config;
                } catch (Exception ignored) { }
            }
        }

        for (Configuration config : configs) {
            String ext = config.getSourceExtension();
            if (ext == null || ext.isEmpty()) continue;
            for (File f : files) {
                if (f.getName().toLowerCase().endsWith(ext.toLowerCase())) return config;
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

    private String javaFullyQualifiedName(File file, String content) {
        String className = file.getName().replace(".java", "");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^\\s*package\\s+([a-zA-Z_][\\w.]*)\\s*;")
                .matcher(content);
        if (m.find()) {
            return m.group(1) + "." + className;
        }
        return className;
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

        if (language.equalsIgnoreCase("JAVA")) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                return javaFullyQualifiedName(file, content);
            } catch (Exception e) {
                return file.getName().replace(".java", "");
            }
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
