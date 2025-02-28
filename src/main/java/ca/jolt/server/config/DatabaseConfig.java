package ca.jolt.server.config;

import java.util.Properties;

public class DatabaseConfig {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final String url;
    private final String username;
    private final String password;
    private final String driver;

    private DatabaseConfig(String url, String username, String password, String driver) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = driver;
    }

    public static DatabaseConfig fromProperties(Properties props) {
        String url = props.getProperty("db.url", DEFAULT_URL);
        String username = props.getProperty("db.username", DEFAULT_USERNAME);
        String password = props.getProperty("db.password", DEFAULT_PASSWORD);
        String driver = props.getProperty("db.driver", DEFAULT_DRIVER);
        return new DatabaseConfig(url, username, password, driver);
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriver() {
        return driver;
    }
}