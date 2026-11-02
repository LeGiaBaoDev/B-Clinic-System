package com.quanlynhakhoa.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final String PROPS_FILE = "database.properties";

    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        Properties props = loadProperties();

        String host     = props.getProperty("db.host",     "localhost");
        String port     = props.getProperty("db.port",     "1433");
        String dbName   = props.getProperty("db.name",     "BClinic");
        boolean winAuth = Boolean.parseBoolean(
                props.getProperty("db.integratedSecurity", "false"));

        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("BClinic-Pool");

        if (winAuth) {
            config.setJdbcUrl(String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;" +
                    "integratedSecurity=true;encrypt=true;trustServerCertificate=true",
                    host, port, dbName));
        } else {
            config.setJdbcUrl(String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;" +
                    "encrypt=true;trustServerCertificate=true",
                    host, port, dbName));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
        }

        this.dataSource = new HikariDataSource(config);
    }

    private static class Holder {
        private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    }

    public static DatabaseConnection getInstance() {
        return Holder.INSTANCE;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
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
