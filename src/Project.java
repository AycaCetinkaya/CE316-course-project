import java.util.List;

public class Project {
    private String name;
    private Configuration configuration;
    private List<StudentZipSubmission> submissions;
    private List<TestCase> testCases;

    public Project(String name, Configuration configuration,
                   List<StudentZipSubmission> submissions,
                   List<TestCase> testCases) {
        this.name = name;
        this.configuration = configuration;
        this.submissions = submissions;
        this.testCases = testCases;
    }

    public String getName() { return name; }
    public Configuration getConfiguration() { return configuration; }
    public List<StudentZipSubmission> getSubmissions() { return submissions; }
    public List<TestCase> getTestCases() { return testCases; }
}
