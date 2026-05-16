import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentZipSubmission {

    private String studentId;
    private File zipFile;
    private File extractedFolder;
    private Result result;
    private List<PerTestResult> perTestResults = new ArrayList<>();

    public StudentZipSubmission(String studentId, File zipFile) {
        this.studentId = studentId;
        this.zipFile = zipFile;
    }

    public String getStudentId() {
        return studentId;
    }

    public File getZipFile() {
        return zipFile;
    }

    public File getExtractedFolder() {
        return extractedFolder;
    }

    public Result getResult() {
        return result;
    }

    public List<PerTestResult> getPerTestResults() {
        return Collections.unmodifiableList(perTestResults);
    }

    public void setExtractedFolder(File extractedFolder) {
        this.extractedFolder = extractedFolder;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public void setPerTestResults(List<PerTestResult> perTestResults) {
        this.perTestResults = perTestResults == null ? new ArrayList<>() : new ArrayList<>(perTestResults);
    }
}
