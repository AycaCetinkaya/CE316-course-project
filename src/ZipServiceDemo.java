import java.io.*;
import java.util.*;

public class ZipServiceDemo {

    public static void main(String[] args) throws Exception {
        // You can use a relative path "test-submissions" or your specific desktop path
        File submissionsDir = new File("test-submissions");

        if (!submissionsDir.exists()) {
            if (submissionsDir.mkdirs()) {
                System.out.println("Created directory: " + submissionsDir.getAbsolutePath());
                System.out.println("Please drop ZIP files into this folder and run again.");
            }
            return;
        }

        System.out.println("Reading from: " + submissionsDir.getAbsolutePath());

        ZipService zipService = new ZipService();

        // ── Step 1: Extract All (UML Method Name) ────────────────────── //
        System.out.println("\n--- Processing Submissions ---");

        // Changed from loadSubmissionsFromDirectory to extractAll
        List<StudentZipSubmission> submissions = zipService.extractAll(submissionsDir);

        if (submissions.isEmpty()) {
            System.out.println("No submissions found (ensure files end in .zip).");
            return;
        }

        for (StudentZipSubmission sub : submissions) {
            System.out.println("\n> Student ID: " + sub.getStudentId());

            if (sub.getResult() == null) {
                // Success: If result is null, it means no extraction error was recorded
                System.out.println("  Status       : SUCCESS");
                System.out.println("  Folder       : " + sub.getExtractedFolder().getName());
                System.out.println("  Files found  : " + listFiles(sub.getExtractedFolder()));
            } else {
                // Failure: The ZipService caught an error and set a Result
                System.out.println("  Status       : " + sub.getResult().getStatus());
                System.out.println("  Error        : " + sub.getResult().getErrorMessage());
            }
        }

        // ── Step 2: Test Cleanup (Optional) ─────────────────────────── //
        /*
        System.out.println("\n--- Testing Individual Cleanup ---");
        if (!submissions.isEmpty()) {
            zipService.cleanup(submissions.get(0)); // UML name: cleanup
            System.out.println("Cleaned up folder for: " + submissions.get(0).getStudentId());
        }
        */

        System.out.println("\nDone. Check your folder to see the extracted directories.");
    }

    private static String listFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return "(empty)";
        List<String> names = new ArrayList<>();
        for (File f : files) names.add(f.getName());
        return String.join(", ", names);
    }
}