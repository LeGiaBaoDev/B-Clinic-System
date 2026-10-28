package com.quanlynhakhoa.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final String PROPS_FILE = "database.properties";

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnection() {
        Properties props = loadProperties();

        String host     = props.getProperty("db.host",     "localhost");
        String port     = props.getProperty("db.port",     "1433");
        String dbName   = props.getProperty("db.name",     "BClinic");
        boolean winAuth = Boolean.parseBoolean(
                props.getProperty("db.integratedSecurity", "false"));

        if (winAuth) {
            this.url = String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;" +
                    "integratedSecurity=true;encrypt=true;" +
                    "trustServerCertificate=true",
                    host, port, dbName);
            this.username = null;
            this.password = null;
        } else {
            this.url = String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;" +
                    "encrypt=true;trustServerCertificate=true",
                    host, port, dbName);
            this.username = props.getProperty("db.username");
            this.password = props.getProperty("db.password");
        }
    }

    private static class Holder {
        private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    }

    public static DatabaseConnection getInstance() {
        return Holder.INSTANCE;
    }

    public String getUrl()      { return url;      }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    public Connection getConnection() throws SQLException {
        if (username == null) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Kết nối thất bại: " + e.getMessage());
            return false;
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(PROPS_FILE)) {
            if (is == null) {
                throw new IllegalStateException(
                        "Không tìm thấy file cấu hình: " + PROPS_FILE);
            }
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Lỗi đọc file cấu hình: " + PROPS_FILE, e);
        }
        return props;
    }
}
