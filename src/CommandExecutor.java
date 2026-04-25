import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class CommandExecutor {

    public CommandResult execute(String command, File workingDirectory) {
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try {
            ProcessBuilder processBuilder;

            if (isWindows()) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                processBuilder = new ProcessBuilder("bash", "-c", command);
            }

            processBuilder.directory(workingDirectory);

            Process process = processBuilder.start();

            BufferedReader outputReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
            );

            String line;

            while ((line = outputReader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            while ((line = errorReader.readLine()) != null) {
                error.append(line).append(System.lineSeparator());
            }

            int exitCode = process.waitFor();

            return new CommandResult(exitCode, output.toString(), error.toString());

        } catch (Exception e) {
            return new CommandResult(-1, output.toString(), e.getMessage());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}