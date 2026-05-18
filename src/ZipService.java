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

        if (zipFile == null || !zipFile.exists() || !zipFile.isFile()) {
            throw new ZipServiceException("ZIP file does not exist.");
        }

        if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
            throw new ZipServiceException("File is not a ZIP archive: " + zipFile.getName());
        }

        if (destinationDir == null) {
            throw new ZipServiceException("No extraction destination set for student: " + submission.getStudentId());
        }

        try {
            if (destinationDir.exists()) {
                deleteRecursively(destinationDir.toPath());
            }

            if (!destinationDir.mkdirs()) {
                throw new ZipServiceException("Cannot create directory: " + destinationDir.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new ZipServiceException("Cannot prepare extraction folder: " + e.getMessage(), e);
        }

        Path destPath = destinationDir.toPath().toAbsolutePath().normalize();
        int extractedFileCount = 0;

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String entryName = entry.getName().replace('\\', '/');

                if (shouldSkipZipEntry(entryName)) {
                    continue;
                }

                String cleanedName = removeOuterStudentFolder(
                        entryName,
                        submission.getStudentId()
                );

                if (cleanedName.isEmpty() || shouldSkipZipEntry(cleanedName)) {
                    continue;
                }

                Path targetPath = destPath.resolve(cleanedName).normalize();

                if (!targetPath.startsWith(destPath)) {
                    throw new ZipServiceException("Unsafe ZIP entry skipped: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());

                    try (InputStream is = zip.getInputStream(entry)) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        extractedFileCount++;
                    }
                }
            }

            if (extractedFileCount == 0) {
                throw new ZipServiceException("ZIP archive is empty or contains only ignored system files.");
            }

        } catch (ZipException e) {
            throw new ZipServiceException("Corrupt or invalid ZIP file: " + zipFile.getName(), e);
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
    private boolean shouldSkipZipEntry(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return true;
        }

        String normalized = entryName.replace('\\', '/');
        String fileName = normalized;

        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            fileName = normalized.substring(slashIndex + 1);
        }

        return normalized.startsWith("__MACOSX/")
                || normalized.contains("/__MACOSX/")
                || fileName.equals(".DS_Store")
                || fileName.startsWith("._");
    }
}
