package com.mycompany.pharmacypos;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnection {

    // Fallback values so the app still runs out-of-the-box during development.
    // For real deployment, set these via environment variables (DB_URL, DB_USER,
    // DB_PASSWORD) or a config.properties file next to the jar instead of
    // editing this file - see loadConfig() below.
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/pharmacy_pos";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "pharmacy2026@";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Properties props = loadConfig();
            String url = props.getProperty("db.url", DEFAULT_URL);
            String user = props.getProperty("db.user", DEFAULT_USER);
            String password = props.getProperty("db.password", DEFAULT_PASSWORD);

            Connection con = DriverManager.getConnection(url, user, password);
            System.out.println("Database Connected!");
            return con;
        } catch (Exception e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads DB settings with this priority, so you can override the
     * hardcoded defaults above without touching source code:
     *   1. Environment variables: DB_URL, DB_USER, DB_PASSWORD
     *   2. config.properties file placed next to the running jar
     *   3. The DEFAULT_* constants above (local development fallback)
     */
    private static Properties loadConfig() {
        Properties props = new Properties();

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPassword = System.getenv("DB_PASSWORD");
        if (envUrl != null) props.setProperty("db.url", envUrl);
        if (envUser != null) props.setProperty("db.user", envUser);
        if (envPassword != null) props.setProperty("db.password", envPassword);

        try (InputStream in = new FileInputStream("config.properties")) {
            Properties fileProps = new Properties();
            fileProps.load(in);
            for (String key : fileProps.stringPropertyNames()) {
                props.putIfAbsent(key, fileProps.getProperty(key));
            }
        } catch (IOException ignored) {
            // No config.properties found next to the jar - that's fine,
            // env vars or the hardcoded defaults will be used instead.
        }

        return props;
    }
}