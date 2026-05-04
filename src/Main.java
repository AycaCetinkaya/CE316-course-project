import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        try {
            File submissionsDir = new File("test-submissions");

            ZipService zipService = new ZipService();
            List<StudentZipSubmission> submissions = zipService.extractAll(submissionsDir);

            List<TestCase> testCases = new ArrayList<>();
            testCases.add(new TestCase("3 1 2", "1 2 3"));
            testCases.add(new TestCase("9 4 7", "4 7 9"));
            testCases.add(new TestCase("10 2 5", "2 5 10"));

            EvaluationService evaluationService = new EvaluationService();

            Language selectedLanguage = Language.AUTO;
            // GUI gelince burası dropdown'dan gelecek
            // AUTO, C, JAVA, PYTHON
            // Kullanıcıya auto seçenegi de gui de config kısmında sorulacak.

            for (StudentZipSubmission submission : submissions) {

                Language actualLanguage;

                if (selectedLanguage == Language.AUTO) {
                    actualLanguage = detectLanguage(submission.getExtractedFolder());
                } else {
                    actualLanguage = selectedLanguage;
                }

                Configuration config = getConfig(actualLanguage, submission.getExtractedFolder());

                if (config == null) {
                    submission.setResult(new Result(
                            Status.RUNTIME_ERROR,
                            "",
                            "Unsupported language or no source file found."
                    ));
                    continue;
                }

                List<StudentZipSubmission> singleSubmissionList = new ArrayList<>();
                singleSubmissionList.add(submission);

                Project project = new Project(
                        "Sorting Project",
                        config,
                        singleSubmissionList,
                        testCases
                );

                evaluationService.evaluateProject(project);
            }

            System.out.println("\n--- FINAL RESULTS ---");

            for (StudentZipSubmission submission : submissions) {
                Result result = submission.getResult();

                System.out.println("Student: " + submission.getStudentId());

                if (result == null) {
                    System.out.println("Status : NOT_EVALUATED");
                    System.out.println();
                    continue;
                }

                System.out.println("Status : " + result.getStatus());

                if (result.getErrorMessage() != null && !result.getErrorMessage().isBlank()) {
                    System.out.println("Error  : " + result.getErrorMessage().split("\n")[0]);
                }

                System.out.println();
            }

        } catch (ZipServiceException e) {
            System.out.println("ZIP service error: " + e.getMessage());
        }
    }

    private static Language detectLanguage(File folder) {
        if (folder == null || !folder.exists()) {
            return Language.UNKNOWN;
        }

        List<File> files = getAllFiles(folder);

        for (File file : files) {
            String name = file.getName().toLowerCase();

            if (name.endsWith(".c")) {
                return Language.C;
            }

            if (name.endsWith(".java")) {
                return Language.JAVA;
            }

            if (name.endsWith(".py")) {
                return Language.PYTHON;
            }
        }

        return Language.UNKNOWN;
    }

    private static Configuration getConfig(Language language, File folder) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        switch (language) {

            case C:
                return new Configuration(
                        "C Config",
                        "gcc *.c -o main",
                        isWindows ? "main.exe" : "./main"
                );

            case JAVA:

                String mainClass = findMainClass(folder);

                if (mainClass == null) {
                    return null; // main yok → error
                }

                return new Configuration(
                        "Java Config",
                        "javac *.java",
                        "java " + mainClass
                );

            case PYTHON:

                // main.py varsa onu çalıştır
                return new Configuration(
                        "Python Config",
                        "echo skip",
                        isWindows ? "python main.py" : "python3 main.py"
                );

            default:
                return null;
        }
    }

    private static List<File> getAllFiles(File folder) {
        List<File> files = new ArrayList<>();

        File[] items = folder.listFiles();
        if (items == null) {
            return files;
        }

        for (File item : items) {
            if (item.isFile()) {
                files.add(item);
            } else if (item.isDirectory()) {
                files.addAll(getAllFiles(item));
            }
        }

        return files;
    }
    private static String findMainClass(File folder) {
        List<File> files = getAllFiles(folder);

        for (File file : files) {
            if (file.getName().endsWith(".java")) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));

                    if (content.contains("public static void main")) {
                        return file.getName().replace(".java", "");
                    }

                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}