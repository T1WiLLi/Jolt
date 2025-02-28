package ca.jolt.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A simple singleton that loads application.properties from META-INF
 * and provides access to typed config objects like ServerConfig and
 * DatabaseConfig.
 */
public final class ConfigurationManager {

    private static final String DEFAULT_CONFIG_FILE = "/META-INF/application.properties";
    private static final ConfigurationManager INSTANCE = new ConfigurationManager();

    private Properties properties;
    private ServerConfig serverConfig;
    private DatabaseConfig databaseConfig;

    private ConfigurationManager() {
        loadProperties();
        loadConfigs();
    }

    public static ConfigurationManager getInstance() {
        return INSTANCE;
    }

    private void loadProperties() {
        properties = new Properties();
        try (InputStream in = ConfigurationManager.class.getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (in != null) {
                properties.load(in);
            } else {
                System.err.println("No configuration file found at " + DEFAULT_CONFIG_FILE + ". Using defaults.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfigs() {
        serverConfig = ServerConfig.fromProperties(properties);
        databaseConfig = DatabaseConfig.fromProperties(properties);
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}