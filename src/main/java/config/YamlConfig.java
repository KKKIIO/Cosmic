package config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.esotericsoftware.yamlbeans.YamlReader;

import constants.string.CharsetConstants;

public class YamlConfig {
    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static final YamlConfig config = loadConfig();

    public List<WorldConfig> worlds;
    public ServerConfig server;

    private static YamlConfig loadConfig() {
        String configPath = System.getenv("COSMIC_CONFIG_PATH");
        if (configPath == null || configPath.isEmpty()) {
            configPath = CONFIG_FILE_NAME;
        }
        try {
            YamlReader reader = new YamlReader(
                    Files.newBufferedReader(Path.of(configPath), CharsetConstants.CHARSET));
            YamlConfig config = reader.read(YamlConfig.class);
            reader.close();
            return config;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(
                    "Could not read config file " + configPath + ": " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not successfully parse config file " + configPath + ": " + e.getMessage());
        }
    }
}

