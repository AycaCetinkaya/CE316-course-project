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
        File projectLocal = new File(DEFAULT_FILE_NAME).getAbsoluteFile();
        if (projectLocal.exists()) {
            return projectLocal;
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isEmpty()) {
            File cwdFile = Paths.get(userDir, DEFAULT_FILE_NAME).toFile();
            if (cwdFile.exists()) {
                return cwdFile;
            }
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            File homeFile = Paths.get(userHome, ".iae", DEFAULT_FILE_NAME).toFile();
            if (homeFile.exists()) {
                return homeFile;
            }
        }

        return projectLocal;
    }
}
