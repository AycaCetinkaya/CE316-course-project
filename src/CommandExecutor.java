import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {

    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private final long timeoutSeconds;

    public CommandExecutor() {
        this(DEFAULT_TIMEOUT_SECONDS);
    }

    public CommandExecutor(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    public CommandResult execute(String command, File workingDirectory) {
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try {
            String platformCommand = platformizeCommand(command, workingDirectory);

            ProcessBuilder processBuilder;

            if (isWindows()) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", platformCommand);
            } else {
                processBuilder = new ProcessBuilder("bash", "-c", platformCommand);
            }

            processBuilder.directory(workingDirectory);

            Process process = processBuilder.start();

            Thread outputThread = new Thread(() ->
                    readStream(process.getInputStream(), output)
            );

            Thread errorThread = new Thread(() ->
                    readStream(process.getErrorStream(), error)
            );

            outputThread.start();
            errorThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();

                outputThread.join(1000);
                errorThread.join(1000);

                return new CommandResult(
                        -2,
                        output.toString(),
                        error + "Process timed out after " + timeoutSeconds + " seconds."
                );
            }

            outputThread.join();
            errorThread.join();

            return new CommandResult(
                    process.exitValue(),
                    output.toString(),
                    error.toString()
            );

        } catch (Exception e) {
            return new CommandResult(-1, output.toString(), e.getMessage());
        }
    }

    private void readStream(java.io.InputStream stream, StringBuilder target) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;

            while ((line = reader.readLine()) != null) {
                target.append(line).append(System.lineSeparator());
            }

        } catch (Exception e) {
            target.append(e.getMessage()).append(System.lineSeparator());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String platformizeCommand(String command, File workingDirectory) {
        if (command == null) return "";

        command = expandWildcard(command, workingDirectory, "*.java");
        command = expandWildcard(command, workingDirectory, "*.c");
        command = expandWildcard(command, workingDirectory, "*.cpp");

        if (isWindows()) {
            // Windows-specific fixes
            command = command.replaceAll("\\bpython3\\b", "python");
            command = command.replaceAll("(?<![A-Za-z0-9_./\\\\-])\\./([A-Za-z0-9_.-]+)", "$1.exe");
        }

        return command;
    }

    private String expandWildcard(String command, File workingDirectory, String wildcard) {
        if (command == null || !command.contains(wildcard)) {
            return command;
        }

        String extension = wildcard.substring(1).toLowerCase();

        List<File> matchingFiles = findFilesByExtension(workingDirectory, extension);

        if (matchingFiles.isEmpty()) {
            return command;
        }

        StringBuilder expanded = new StringBuilder();

        for (File file : matchingFiles) {
            String relativePath = workingDirectory.toPath()
                    .toAbsolutePath()
                    .normalize()
                    .relativize(file.toPath().toAbsolutePath().normalize())
                    .toString();

            expanded.append("\"")
                    .append(relativePath)
                    .append("\"")
                    .append(" ");
        }

        return command.replace(wildcard, expanded.toString().trim());
    }

    private List<File> findFilesByExtension(File folder, String extension) {
        List<File> files = new ArrayList<>();

        if (folder == null || !folder.exists()) {
            return files;
        }

        File[] children = folder.listFiles();

        if (children == null) {
            return files;
        }

        for (File child : children) {
            String name = child.getName();

            if (name.equals("__MACOSX") || name.equals(".DS_Store") || name.startsWith("._")) {
                continue;
            }

            if (child.isDirectory()) {
                files.addAll(findFilesByExtension(child, extension));
            } else if (child.isFile() && name.toLowerCase().endsWith(extension)) {
                files.add(child);
            }
        }

        return files;
    }
}