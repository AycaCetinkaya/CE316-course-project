import java.util.List;

public class Project {
    private String name;
    private long id;
    private Configuration configuration;
    private List<StudentZipSubmission> submissions;
    private List<TestCase> testCases;
    private long lastModified;

    public Project(String name, Configuration configuration,
                   List<StudentZipSubmission> submissions,
                   List<TestCase> testCases) {
        this.id = -1;
        this.name = name;
        this.configuration = configuration;
        this.submissions = submissions;
        this.testCases = testCases;
        this.lastModified = 0L;
    }

    public String getName() { return name; }
    public Configuration getConfiguration() { return configuration; }
    public List<StudentZipSubmission> getSubmissions() { return submissions; }
    public List<TestCase> getTestCases() { return testCases; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    public void setName(String name) { this.name = name; }
}
