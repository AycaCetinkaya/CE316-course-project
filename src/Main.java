import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        try {
            File submissionsDir = new File("test-submissions");

            ZipService zipService = new ZipService();
            List<StudentZipSubmission> submissions = zipService.extractAll(submissionsDir);

            Configuration config = new Configuration(
                    "C Config",
                    "gcc *.c -o main",
                    "./main"
            );
            List<TestCase> testCases = new ArrayList<>();

            testCases.add(new TestCase("3 1 2", "1 2 3"));
            testCases.add(new TestCase("9 4 7", "4 7 9"));
            testCases.add(new TestCase("10 2 5", "2 5 10"));
            Project project = new Project(
                    "Sorting Project",
                    config,
                    submissions,
                    testCases
            );

            EvaluationService evaluationService = new EvaluationService();
            evaluationService.evaluateProject(project);

            System.out.println("\n--- FINAL RESULTS ---");

            for (StudentZipSubmission submission : project.getSubmissions()) {
                Result result = submission.getResult();

                System.out.println("Student: " + submission.getStudentId());
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
}