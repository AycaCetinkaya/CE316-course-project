import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseProjectIntegrationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("iae-db-test");
        Path dbPath = tempDir.resolve("iae-test.db");

        try {
            testDatabasePersistence(dbPath);
            testMainFileResolution(tempDir);
        } finally {
            deleteRecursively(tempDir.toFile());
        }

        System.out.println("\n" + passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testDatabasePersistence(Path dbPath) throws Exception {
        DatabaseManager db = new DatabaseManager("jdbc:sqlite:" + dbPath.toAbsolutePath());
        db.connect();
        db.initSchema();

        Configuration config = new Configuration(
                "Python Test",
                "PYTHON",
                "echo skip",
                "python $MAIN",
                ".py",
                "if\\s+__name__\\s*==\\s*[\"']__main__[\"']"
        );

        List<TestCase> firstTests = new ArrayList<>();
        firstTests.add(new TestCase("3 1 2", "1 2 3"));
        firstTests.add(new TestCase("9 4 7", "4 7 9"));

        long projectId = db.saveProject("Sorting Project", config, firstTests);
        test("projectIdCreated", projectId > 0);

        List<Project> projects = db.getProjects();
        test("projectSaved", projects.size() == 1);
        test("configLanguageLoaded", "PYTHON".equals(projects.get(0).getConfiguration().getLanguage()));
        test("configExtensionLoaded", ".py".equals(projects.get(0).getConfiguration().getSourceExtension()));
        test("configPatternLoaded", config.getEntryPointPattern().equals(projects.get(0).getConfiguration().getEntryPointPattern()));
        test("testCasesLoaded", projects.get(0).getTestCases().size() == 2);

        Configuration updated = new Configuration(
                "Python Test",
                "PYTHON",
                "echo skip",
                "python3 $MAIN",
                ".py",
                "__main__"
        );

        List<TestCase> replacementTests = new ArrayList<>();
        replacementTests.add(new TestCase("10 2 5", "2 5 10"));
        long sameProjectId = db.saveProject("Sorting Project", updated, replacementTests);
        test("projectUpsertKeepsId", projectId == sameProjectId);

        projects = db.getProjects();
        test("configUpsertUpdatesRunCommand", "python3 $MAIN".equals(projects.get(0).getConfiguration().getRunCommand()));
        test("testCasesReplaced", projects.get(0).getTestCases().size() == 1);

        StudentZipSubmission submission = new StudentZipSubmission("2024001", tempFile(dbPath.getParent(), "student.zip"));
        submission.setExtractedFolder(tempFile(dbPath.getParent(), "student"));
        submission.setResult(new Result(Status.SUCCESS, "ok", ""));
        db.upsertSubmission(projectId, submission);

        submission.setResult(new Result(Status.WRONG_OUTPUT, "bad", "mismatch"));
        db.upsertSubmission(projectId, submission);

        test("submissionUpsertDoesNotDuplicate", countRows(dbPath, "StudentSubmissions") == 1);
        test("submissionStatusUpdated", "WRONG_OUTPUT".equals(projectsReloaded(db).get(0).getSubmissions().get(0).getResult().getStatus().name()));

        db.deleteSubmissionsForProject(projectId);
        test("submissionsDeletedForRerun", countRows(dbPath, "StudentSubmissions") == 0);

        db.upsertSubmission(projectId, submission);
        db.deleteProject(projectId);
        test("deleteProjectRemovesProject", countRows(dbPath, "Projects") == 0);
        test("deleteProjectRemovesTestCases", countRows(dbPath, "TestCases") == 0);
        test("deleteProjectRemovesSubmissions", countRows(dbPath, "StudentSubmissions") == 0);

        db.disconnect();
    }

    private static void testMainFileResolution(Path tempDir) throws Exception {
        Path nested = tempDir.resolve("submission").resolve("homework").resolve("Project");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("app.py"), "if __name__ == \"__main__\":\n    print('ok')\n");

        ProjectRunnerService runner = new ProjectRunnerService();
        Method method = ProjectRunnerService.class.getDeclaredMethod(
                "findMainFileNameByConfig",
                File.class,
                Configuration.class
        );
        method.setAccessible(true);

        Configuration python = new Configuration(
                "Python Test",
                "PYTHON",
                "echo skip",
                "python $MAIN",
                ".py",
                "if\\s+__name__\\s*==\\s*[\"']__main__[\"']"
        );

        String token = (String) method.invoke(runner, tempDir.resolve("submission").toFile(), python);
        test("nestedPythonMainKeepsRelativePathAndExtension", "homework/Project/app.py".equals(token));

        Path javaDir = tempDir.resolve("java-submission");
        Files.createDirectories(javaDir);
        Files.writeString(javaDir.resolve("Algorithm.java"), "public class Algorithm { public static void main(String[] args) {} }");

        Configuration javaConfig = new Configuration(
                "Java Test",
                "JAVA",
                "javac *.java",
                "java $MAIN",
                ".java",
                "public\\s+static\\s+void\\s+main"
        );

        String javaToken = (String) method.invoke(runner, javaDir.toFile(), javaConfig);
        test("javaMainUsesClassName", "Algorithm".equals(javaToken));
    }

    private static List<Project> projectsReloaded(DatabaseManager db) throws Exception {
        return db.getProjects();
    }

    private static int countRows(Path dbPath, String table) throws Exception {
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static File tempFile(Path directory, String name) throws Exception {
        Path path = Files.createTempFile(directory, "iae-" + name, ".tmp");
        return path.toFile();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;

        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static void test(String name, boolean condition) {
        if (condition) {
            System.out.println("PASS: " + name);
            passed++;
        } else {
            System.out.println("FAIL: " + name);
            failed++;
        }
    }
}
