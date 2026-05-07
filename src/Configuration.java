import java.util.Map;

public class Configuration {
    private String name;
    private String language;
    private String compileCommand;
    private String runCommand;
    private String sourceExtension;
    private String entryPointPattern;

    public Configuration(String name, String compileCommand, String runCommand) {
        this(name, "", compileCommand, runCommand, "", "");
    }

    public Configuration(String name,
                         String language,
                         String compileCommand,
                         String runCommand,
                         String sourceExtension,
                         String entryPointPattern) {
        this.name = name;
        this.language = language;
        this.compileCommand = compileCommand;
        this.runCommand = runCommand;
        this.sourceExtension = sourceExtension;
        this.entryPointPattern = entryPointPattern;
    }

    public String getName() { return name; }
    public String getLanguage() { return language; }
    public String getCompileCommand() { return compileCommand; }
    public String getRunCommand() { return runCommand; }
    public String getSourceExtension() { return sourceExtension; }
    public String getEntryPointPattern() { return entryPointPattern; }

    public void setName(String name) { this.name = name; }
    public void setLanguage(String language) { this.language = language; }
    public void setCompileCommand(String compileCommand) { this.compileCommand = compileCommand; }
    public void setRunCommand(String runCommand) { this.runCommand = runCommand; }
    public void setSourceExtension(String sourceExtension) { this.sourceExtension = sourceExtension; }
    public void setEntryPointPattern(String entryPointPattern) { this.entryPointPattern = entryPointPattern; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":").append(JsonUtil.encodeString(name)).append(",");
        sb.append("\"language\":").append(JsonUtil.encodeString(language)).append(",");
        sb.append("\"compileCommand\":").append(JsonUtil.encodeString(compileCommand)).append(",");
        sb.append("\"runCommand\":").append(JsonUtil.encodeString(runCommand)).append(",");
        sb.append("\"sourceExtension\":").append(JsonUtil.encodeString(sourceExtension)).append(",");
        sb.append("\"entryPointPattern\":").append(JsonUtil.encodeString(entryPointPattern));
        sb.append("}");
        return sb.toString();
    }

    public static Configuration fromJson(String json) {
        Map<String, String> map = JsonUtil.parseObject(json);
        return new Configuration(
                map.getOrDefault("name", ""),
                map.getOrDefault("language", ""),
                map.getOrDefault("compileCommand", ""),
                map.getOrDefault("runCommand", ""),
                map.getOrDefault("sourceExtension", ""),
                map.getOrDefault("entryPointPattern", "")
        );
    }
}
