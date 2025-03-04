package ca.jolt.server.config;

import java.util.Properties;

/**
 * A simple singleton that loads application.properties from META-INF
 * and provides access to typed config objects like ServerConfig and
 * DatabaseConfig.
 */
public final class ConfigurationManager {

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
        properties = ConfigLoader.load();
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