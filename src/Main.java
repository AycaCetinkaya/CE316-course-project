import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class Main {

    public static void main(String[] args) {

        Configuration config = new Configuration(
                "C Config",
                "gcc *.c -o main",
                "./main"
        );

        TestCase test = new TestCase("3 1 2", "1 2 3");
        List<TestCase> tests = new ArrayList<>();
        tests.add(test);

        StudentZipSubmission submission = new StudentZipSubmission("123456", null);
        List<StudentZipSubmission> submissions = new ArrayList<>();
        submissions.add(submission);

        Project project = new Project(
                "Sorting Project",
                config,
                submissions,
                tests
        );

        Result result = new Result(Status.SUCCESS, "1 2 3", "");
        submission.setResult(result);

        System.out.println("Project: " + project.getName());
        System.out.println("Config: " + project.getConfiguration().getName());

        System.out.println("Student ID: " + submission.getStudentId());
        System.out.println("Result: " + submission.getResult().getStatus());
        System.out.println("Output: " + submission.getResult().getOutput());

        System.out.println("Test Expected: " + tests.get(0).getExpectedOutput());
        CommandExecutor executor = new CommandExecutor();

        CommandResult commandResult = executor.execute("echo Hello", new java.io.File("."));

        System.out.println("Command Output:");
        System.out.println(commandResult.getOutput());

        System.out.println("Command Error:");
        System.out.println(commandResult.getError());

        System.out.println("Exit Code: " + commandResult.getExitCode());

    }
}