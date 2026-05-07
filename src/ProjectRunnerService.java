import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectRunnerService {

    public Project runProject(String projectName,
                              File submissionsDir,
                              List<TestCase> testCases,
                              Language selectedLanguage) throws ZipServiceException {

        ZipService zipService = new ZipService();
        List<StudentZipSubmission> submissions = zipService.extractAll(submissionsDir);

        EvaluationService evaluationService = new EvaluationService();

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
                        "Unsupported language or no source file found."
                ));
                continue;
            }

            List<StudentZipSubmission> single = new ArrayList<>();
            single.add(submission);

            Project singleProject = new Project(projectName, config, single, testCases);
            evaluationService.evaluateProject(singleProject);
        }

        return new Project(
                projectName,
                new Configuration(selectedLanguage.toString(), "", ""),
                submissions,
                testCases
        );
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

    private Configuration getConfig(Language language, File folder) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        switch (language) {
            case C:
                return new Configuration(
                        "C Config",
                        "gcc *.c -o main",
                        isWindows ? "main.exe" : "./main"
                );

            case JAVA:
                String mainClass = findMainClass(folder);
                if (mainClass == null) return null;

                return new Configuration(
                        "Java Config",
                        "javac *.java",
                        "java " + mainClass
                );

            case PYTHON:
                String mainPython = findMainPythonFile(folder);
                if (mainPython == null) return null;

                return new Configuration(
                        "Python Config",
                        "echo skip",
                        (isWindows ? "python " : "python3 ") + mainPython
                );

            case HASKELL:
                String mainHaskell = findMainHaskellFile(folder);
                if (mainHaskell == null) return null;

                return new Configuration(
                        "Haskell Config",
                        "ghc --make " + mainHaskell + " -o main",
                        isWindows ? "main.exe" : "./main"
                );

            default:
                return null;
        }
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

                    if (content.contains("public static void main")) {
                        return file.getName().replace(".java", "");
                    }
                } catch (Exception ignored) {
                }
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

                if (name.equals("main.py")) {
                    fallback = file;
                }
            } catch (Exception ignored) {
            }
        }

        if (fallback != null) {
            return fallback.getName();
        }

        for (File file : getAllFiles(folder)) {
            if (file.getName().endsWith(".py")) {
                return file.getName();
            }
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
            } catch (Exception ignored) {
            }
        }

        if (fallback != null) {
            return fallback.getName();
        }

        for (File file : getAllFiles(folder)) {
            String name = file.getName();
            if (name.endsWith(".hs") || name.endsWith(".lhs")) {
                return name;
            }
        }

        return null;
    }
}
