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

    public File getDefaultFile() {
        return defaultFile;
    }

    public List<Configuration> loadAll() {
        if (defaultFile == null || !defaultFile.exists()) {
            return new ArrayList<>();
        }
        return loadFrom(defaultFile);
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
        File projectLocal = new File(DEFAULT_FILE_NAME);
        if (projectLocal.exists()) {
            return projectLocal;
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            Path iaeDir = Paths.get(userHome, ".iae");
            return iaeDir.resolve(DEFAULT_FILE_NAME).toFile();
        }

        return projectLocal;
    }
}
