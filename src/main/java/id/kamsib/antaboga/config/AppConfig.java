package id.kamsib.antaboga.config;

import java.io.*;
import java.util.Properties;

/**
 * Application configuration manager.
 * Priority: Environment Variables > Config File > Defaults
 */
public class AppConfig {
    private final Properties props;

    public AppConfig() {
        props = new Properties();
        loadDefaults();
        loadFromFile();
        loadFromEnvironment();
    }

    private void loadDefaults() {
        props.setProperty("server.port", "1337");
        props.setProperty("server.host", "127.0.0.1");
        props.setProperty("db.path", "./data/antaboga");
        props.setProperty("db.username", "antaboga");
        props.setProperty("db.password", "antaboga_default_ch4ng3_m3");
        props.setProperty("session.timeout", "3600");
        props.setProperty("security.csrf.enabled", "true");
        props.setProperty("security.rate-limit", "100");
    }

    private void loadFromFile() {
        File configFile = new File("config/antaboga.properties");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                System.out.println("  [OK] Config loaded from " + configFile.getPath());
            } catch (IOException e) {
                System.err.println("  [WARN] Could not load config: " + e.getMessage());
            }
        } else {
            System.out.println("  [INFO] No config file found, using defaults");
        }
    }

    private void loadFromEnvironment() {
        mapEnv("ANTABOGA_PORT", "server.port");
        mapEnv("ANTABOGA_HOST", "server.host");
        mapEnv("ANTABOGA_DB_PATH", "db.path");
        mapEnv("ANTABOGA_DB_USER", "db.username");
        mapEnv("ANTABOGA_DB_PASS", "db.password");
    }

    private void mapEnv(String envKey, String propKey) {
        String val = System.getenv(envKey);
        if (val != null && !val.isEmpty()) {
            props.setProperty(propKey, val);
        }
    }

    public int getPort() {
        return Integer.parseInt(props.getProperty("server.port"));
    }

    public String getHost() {
        return props.getProperty("server.host");
    }

    public String getDbPath() {
        return props.getProperty("db.path");
    }

    public String getDbUsername() {
        return props.getProperty("db.username");
    }

    public String getDbPassword() {
        return props.getProperty("db.password");
    }

    public int getSessionTimeout() {
        return Integer.parseInt(props.getProperty("session.timeout"));
    }

    public boolean isCsrfEnabled() {
        return Boolean.parseBoolean(props.getProperty("security.csrf.enabled"));
    }

    public int getRateLimit() {
        return Integer.parseInt(props.getProperty("security.rate-limit"));
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
