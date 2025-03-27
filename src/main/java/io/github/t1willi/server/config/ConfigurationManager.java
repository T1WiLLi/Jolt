package io.github.server.config;

import java.util.Properties;

import io.github.database.DatabaseConfiguration;

/**
 * A singleton class that manages application configuration.
 * <p>
 * Loads configuration properties from {@code META-INF/application.properties}
 * using
 * {@link ConfigLoader} and provides access to typed configuration objects such
 * as
 * {@link ServerConfig} and {@link DatabaseConfig}. It also allows retrieval of
 * raw
 * property values.
 * </p>
 */
public final class ConfigurationManager {

    /**
     * The single instance of ConfigurationManager.
     */
    private static final ConfigurationManager INSTANCE = new ConfigurationManager();

    /**
     * The loaded configuration properties.
     */
    private Properties properties;

    /**
     * The server-specific configuration object.
     */
    private ServerConfig serverConfig;

    /**
     * The database-specific configuration object.
     */
    private DatabaseConfiguration databaseConfig;

    /**
     * Private constructor to initialize the configuration manager.
     * <p>
     * Loads properties and initializes configuration objects during instantiation.
     * </p>
     */
    private ConfigurationManager() {
        loadProperties();
        loadConfigs();
    }

    /**
     * Returns the singleton instance of the ConfigurationManager.
     *
     * @return The single instance of this class.
     */
    public static ConfigurationManager getInstance() {
        return INSTANCE;
    }

    /**
     * Loads configuration properties from the default properties file.
     * <p>
     * Uses {@link ConfigLoader#load()} to populate the {@link #properties} field.
     * </p>
     */
    private void loadProperties() {
        properties = ConfigLoader.load();
    }

    /**
     * Initializes typed configuration objects from the loaded properties.
     * <p>
     * Creates instances of {@link ServerConfig} and {@link DatabaseConfig} using
     * the loaded {@link #properties}.
     * </p>
     */
    private void loadConfigs() {
        serverConfig = ServerConfig.fromProperties(properties);
        databaseConfig = DatabaseConfiguration.fromProperties(properties);
    }

    /**
     * Retrieves the server configuration.
     *
     * @return The {@link ServerConfig} instance containing server-specific
     *         settings.
     */
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    /**
     * Retrieves the database configuration.
     *
     * @return The {@link DatabaseConfig} instance containing database-specific
     *         settings.
     */
    public DatabaseConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

    /**
     * Retrieves a property value by key.
     *
     * @param key The key of the property to retrieve.
     * @return The value of the property, or {@code null} if the key is not found.
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Retrieves a property value by key with a default fallback.
     *
     * @param key          The key of the property to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The value of the property, or {@code defaultValue} if the key is not
     *         found.
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}