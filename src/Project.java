import java.util.List;

public class Project {
    public static final String COMPARATOR_EXACT_MATCH = "Exact Match";
    public static final String COMPARATOR_WHITESPACE_INSENSITIVE = "Whitespace Insensitive";

    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private String name;
    private long id;
    private Configuration configuration;
    private List<StudentZipSubmission> submissions;
    private List<TestCase> testCases;
    private long lastModified;
    private String comparatorType;
    private int timeoutSeconds;

    public Project(String name, Configuration configuration,
                   List<StudentZipSubmission> submissions,
                   List<TestCase> testCases) {
        this.id = -1;
        this.name = name;
        this.configuration = configuration;
        this.submissions = submissions;
        this.testCases = testCases;
        this.lastModified = 0L;
        this.comparatorType = COMPARATOR_EXACT_MATCH;
        this.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
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

    public String getComparatorType() {
        return comparatorType;
    }

    public void setComparatorType(String comparatorType) {
        if (comparatorType == null || comparatorType.trim().isEmpty()) {
            this.comparatorType = COMPARATOR_EXACT_MATCH;
        } else {
            this.comparatorType = comparatorType;
        }
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }
}