public class Configuration {
    private String name;
    private String compileCommand;
    private String runCommand;

    public Configuration(String name, String compileCommand, String runCommand) {
        this.name = name;
        this.compileCommand = compileCommand;
        this.runCommand = runCommand;
    }

    public String getName() {
        return name;
    }

    public String getCompileCommand() {
        return compileCommand;
    }

    public String getRunCommand() {
        return runCommand;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCompileCommand(String compileCommand) {
        this.compileCommand = compileCommand;
    }

    public void setRunCommand(String runCommand) {
        this.runCommand = runCommand;
    }
}