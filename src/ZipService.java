import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ZipService {

    /**
     * Matches UML: extractAll(directory: File): List<StudentZipSubmission>
     */
    public List<StudentZipSubmission> extractAll(File directory) throws ZipServiceException {
        validateDirectory(directory);

        File[] found = directory.listFiles(
                f -> f.isFile() && f.getName().toLowerCase().endsWith(".zip")
        );

        if (found == null) {
            throw new ZipServiceException("Could not list files in directory: " + directory.getAbsolutePath());
        }

        List<File> zipFiles = Arrays.asList(found);
        zipFiles.sort(Comparator.comparing(File::getName));

        List<StudentZipSubmission> submissions = new ArrayList<>();

        for (File zipFile : zipFiles) {
            String studentId = stripZipExtension(zipFile.getName());
            StudentZipSubmission submission = new StudentZipSubmission(studentId, zipFile);

            // Set the target folder based on student ID
            File extractionTarget = new File(directory, studentId);
            submission.setExtractedFolder(extractionTarget);

            try {
                // Call the UML-defined extract method
                extract(submission);
            } catch (ZipServiceException e) {
                // As per your logic: record failure but continue processing
                submission.setResult(new Result(
                        Status.EXTRACTION_ERROR,
                        "",
                        "ZIP extraction failed: " + e.getMessage()
                ));
            }
            submissions.add(submission);
        }

        return submissions;
    }

    /**
     * Matches UML: extract(submission: StudentZipSubmission): void
     */
    public void extract(StudentZipSubmission submission) throws ZipServiceException {
        File zipFile = submission.getZipFile();
        File destinationDir = submission.getExtractedFolder();

        if (destinationDir == null) {
            throw new ZipServiceException("No extraction destination set for student: " + submission.getStudentId());
        }

        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw new ZipServiceException("Cannot create directory: " + destinationDir.getAbsolutePath());
        }

        Path destPath = destinationDir.toPath().toAbsolutePath().normalize();

        try (ZipFile zip = new ZipFile(zipFile)) {

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String cleanedName = removeOuterStudentFolder(
                        entry.getName(),
                        submission.getStudentId()
                );

                if (cleanedName.isEmpty()) {
                    continue;
                }

                Path targetPath = destPath.resolve(cleanedName).normalize();

                if (!targetPath.startsWith(destPath)) {
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());

                    try (InputStream is = zip.getInputStream(entry)) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

        } catch (IOException e) {
            throw new ZipServiceException(
                    "Failed to extract " + zipFile.getName() + ": " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Matches UML: cleanup(submission: StudentZipSubmission): void
     */
    public void cleanup(StudentZipSubmission submission) throws ZipServiceException {
        File folder = submission.getExtractedFolder();
        if (folder != null && folder.exists()) {
            try {
                deleteRecursively(folder.toPath());
            } catch (IOException e) {
                throw new ZipServiceException("Cleanup failed for " + submission.getStudentId() + ": " + e.getMessage());
            }
        }
    }


    private String stripZipExtension(String filename) {
        if (filename.toLowerCase().endsWith(".zip")) {
            return filename.substring(0, filename.length() - 4);
        }
        return filename;
    }

    private void validateDirectory(File directory) throws ZipServiceException {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new ZipServiceException("Invalid directory: " + (directory == null ? "null" : directory.getPath()));
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    deleteRecursively(child);
                }
            }
        }
        Files.delete(path);
    }
    private String removeOuterStudentFolder(String entryName, String studentId) {
        entryName = entryName.replace('\\', '/');
        String prefix = studentId + "/";

        if (entryName.startsWith(prefix)) {
            return entryName.substring(prefix.length());
        }

        return entryName;
    }
}
