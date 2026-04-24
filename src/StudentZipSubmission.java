import java.io.File;

public class StudentZipSubmission {

    private String studentId;
    private File zipFile;
    private File extractedFolder;
    private Result result;

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

    public void setExtractedFolder(File extractedFolder) {
        this.extractedFolder = extractedFolder;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}