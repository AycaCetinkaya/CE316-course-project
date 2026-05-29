import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigStore {

    private static final String DEFAULT_FILE_NAME = "configs.json";

    private final File defaultFile;

    public ConfigStore() {
        this.defaultFile = resolveDefaultFile();
    }

    public ConfigStore(File defaultFile) {
        this.defaultFile = defaultFile;
    }

    public List<Configuration> getDefaultConfigs() {
        List<Configuration> defaults = new ArrayList<>();
        defaults.add(new Configuration(
                "Java Default",
                "JAVA",
                "javac -d . *.java",
                "java -cp . $MAIN",
                ".java",
                "public\\s+static\\s+void\\s+main"
        ));
        defaults.add(new Configuration("Python Default", "PYTHON", "", "python3 $MAIN", ".py", "if\\s+__name__\\s*==\\s*[\"']__main__[\"']"));
        defaults.add(new Configuration(
                "C Default",
                "C",
                "gcc *.c -o main",
                "./main",
                ".c",
                "int\\s+main\\s*\\("
        ));
        defaults.add(new Configuration("Haskell Default", "HASKELL", "ghc --make $MAIN -o main", "./main", ".hs", "\\bmain\\s*[:=]"));
        defaults.add(new Configuration("C++ Default", "CPP", "g++ *.cpp -o main", "./main", ".cpp", "int\\s+main\\s*\\("));
        return defaults;
    }

    public File getDefaultFile() {
        return defaultFile;
    }

    public List<Configuration> loadAll() {
        List<Configuration> loaded = new ArrayList<>();
        if (defaultFile != null && defaultFile.exists()) {
            loaded = loadFrom(defaultFile);
        }

        List<Configuration> defaults = getDefaultConfigs();
        boolean changed = false;

        for (Configuration def : defaults) {
            boolean exists = false;
            for (Configuration current : loaded) {
                if (current.getName().equalsIgnoreCase(def.getName())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                loaded.add(def);
                changed = true;
            }
        }
        if (changed) {
            saveAll(loaded);
        }
        return loaded;
    }

    public void saveAll(List<Configuration> configs) {
        saveTo(defaultFile, configs);
    }

    public List<Configuration> loadFrom(File file) {
        List<Configuration> configs = new ArrayList<>();

        if (file == null || !file.exists()) {
            return configs;
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return configs;
            List<String> objects = JsonUtil.splitTopLevelObjects(content);

            for (String obj : objects) {
                configs.add(Configuration.fromJson(obj));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read configs from " + file.getAbsolutePath() + ": " + e.getMessage(), e);
        }

        return configs;
    }

    public void saveTo(File file, List<Configuration> configs) {
        if (file == null) {
            throw new IllegalArgumentException("Target file cannot be null");
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        List<String> jsonObjects = new ArrayList<>();
        for (Configuration c : configs) {
            jsonObjects.add(c.toJson());
        }

        String content = JsonUtil.encodeArray(jsonObjects);

        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write configs to " + file.getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    private static File resolveDefaultFile() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "IAE", DEFAULT_FILE_NAME).toFile();
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            return Paths.get(userHome, ".iae", DEFAULT_FILE_NAME).toFile();
        }

        return new File(DEFAULT_FILE_NAME).getAbsoluteFile();
    }

    public static class ImportResult {
        public final List<Configuration> validConfigsWithoutConflict = new ArrayList<>();
        public final List<Configuration> conflictingConfigs = new ArrayList<>();
        public final List<String> rejectedReasons = new ArrayList<>();
    }

    public ImportResult processImportFile(File file, List<Configuration> allConfigs) throws IOException {
        ImportResult result = new ImportResult();
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();

        if (!json.startsWith("[") || !json.endsWith("]")) {
            result.rejectedReasons.add("Root structure error: The file must be a valid JSON Array enclosed in [ ].");
            return result;
        }

        List<String> rawObjects = new ArrayList<>();
        int braceCount = 0;
        int startIndex = -1;
        boolean insideQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                insideQuotes = !insideQuotes;
            }
            if (!insideQuotes) {
                if (c == '{') {
                    if (braceCount == 0) {
                        startIndex = i;
                    }
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && startIndex != -1) {
                        rawObjects.add(json.substring(startIndex, i + 1));
                        startIndex = -1;
                    }
                }
            }
        }

        if (rawObjects.isEmpty()) {
            result.rejectedReasons.add("The file contains no distinct configuration blocks.");
            return result;
        }

        for (String objectStr : rawObjects) {
            try {
                Configuration incoming = Configuration.fromJson(objectStr);

                if (incoming.getName() == null || incoming.getName().trim().isEmpty() ||
                        incoming.getName().trim().length() > 50 || incoming.getName().trim().matches(".*[\\\\/:*?\"<>|].*") ||
                        incoming.getLanguage() == null || incoming.getLanguage().trim().isEmpty() ||
                        incoming.getLanguage().trim().length() > 50 || incoming.getLanguage().trim().matches(".*[\\\\/:*?\"<>|].*") ||
                        incoming.getRunCommand() == null || incoming.getRunCommand().trim().isEmpty() ||
                        incoming.getSourceExtension() == null || !incoming.getSourceExtension().trim().startsWith(".") ||
                        incoming.getSourceExtension().trim().length() < 2 || incoming.getSourceExtension().trim().length() > 6) {

                    String identifierName = (incoming.getName() != null && !incoming.getName().isEmpty()) ? incoming.getName() : "Unnamed Entry";
                    result.rejectedReasons.add("- '" + identifierName + "' (Violated identity constraints, length boundaries, or missing mandatory values)");
                    continue;
                }

                incoming.setName(incoming.getName().trim());
                incoming.setLanguage(incoming.getLanguage().trim());
                incoming.setCompileCommand(incoming.getCompileCommand() != null ? incoming.getCompileCommand().trim() : "");
                incoming.setRunCommand(incoming.getRunCommand().trim());
                incoming.setSourceExtension(incoming.getSourceExtension().trim());
                incoming.setEntryPointPattern(incoming.getEntryPointPattern() != null ? incoming.getEntryPointPattern().trim() : "");

                String extensionBody = incoming.getSourceExtension().substring(1);
                if (!extensionBody.matches("[a-zA-Z0-9]+")) {
                    result.rejectedReasons.add("- '" + incoming.getName() + "' (Extension contains wildcards or non-alphanumeric characters)");
                    continue;
                }

                boolean conflictDetected = false;
                for (Configuration localConfig : allConfigs) {
                    if (localConfig.getName().equalsIgnoreCase(incoming.getName())) {
                        conflictDetected = true;
                        break;
                    }
                }

                if (conflictDetected) {
                    result.conflictingConfigs.add(incoming);
                } else {
                    result.validConfigsWithoutConflict.add(incoming);
                }

            } catch (Exception ex) {
                result.rejectedReasons.add("- Fragment parse fail (Invalid JSON formatting syntax layout inside item block)");
            }
        }
        return result;
    }

    public boolean checkNameCollision(String proposedName, List<Configuration> currentCollection) {
        if (proposedName == null) return false;
        String normalized = proposedName.trim();
        for (Configuration config : currentCollection) {
            if (config.getName().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }
}
