import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {

    private static final long TIMEOUT_SECONDS = 10;

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

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();

                outputThread.join(1000);
                errorThread.join(1000);

                return new CommandResult(
                        -2,
                        output.toString(),
                        error + "Process timed out after " + TIMEOUT_SECONDS + " seconds."
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
        if (command == null) return command;

        if (isWindows()) {
            command = command.replaceAll("(?<![A-Za-z0-9_])\\./([A-Za-z0-9_.-]+)", "$1.exe");
            command = command.replaceAll("\\bpython3\\b", "python");

            command = expandWildcard(command, workingDirectory, "*.c");
            command = expandWildcard(command, workingDirectory, "*.cpp");
            command = expandWildcard(command, workingDirectory, "*.java");
        }

        return command;
    }

    private String expandWildcard(String command, File workingDirectory, String wildcard) {
        if (!command.contains(wildcard)) {
            return command;
        }

        String extension = wildcard.substring(1); // .c, .cpp, .java

        File[] files = workingDirectory.listFiles((dir, name) ->
                name.toLowerCase().endsWith(extension.toLowerCase())
        );

        if (files == null || files.length == 0) {
            return command;
        }

        StringBuilder fileNames = new StringBuilder();

        for (File file : files) {
            fileNames.append("\"").append(file.getName()).append("\"").append(" ");
        }

        return command.replace(wildcard, fileNames.toString().trim());
    }
}