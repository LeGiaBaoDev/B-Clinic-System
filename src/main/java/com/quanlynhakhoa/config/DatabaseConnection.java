package com.quanlynhakhoa.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton quản lý kết nối JDBC tới SQL Server.
 *
 * <p>Cách dùng:
 * <pre>
 *   try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
 *       // dùng conn
 *   }
 * </pre>
 *
 * <p>Cấu hình qua {@code src/main/resources/database.properties}.
 */
public class DatabaseConnection {

    private static final String PROPS_FILE = "database.properties";

    private final String url;
    private final String username;
    private final String password;

    // ------------------------------------------------------------------ //
    //  Singleton — khởi tạo một lần, thread-safe theo Initialization-on-  //
    //  demand holder idiom                                                  //
    // ------------------------------------------------------------------ //
    private DatabaseConnection() {
        Properties props = loadProperties();

        String host     = props.getProperty("db.host",     "localhost");
        String port     = props.getProperty("db.port",     "1433");
        String dbName   = props.getProperty("db.name",     "BClinic");
        boolean winAuth = Boolean.parseBoolean(
                props.getProperty("db.integratedSecurity", "false"));

        if (winAuth) {
            // Xác thực Windows (không cần username/password)
            this.url = String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;" +
                    "integratedSecurity=true;encrypt=true;" +
                    "trustServerCertificate=true",
                    host, port, dbName);
            this.username = null;
            this.password = null;
        } else {
            // Xác thực SQL Server
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

    /** Trả về instance duy nhất của {@code DatabaseConnection}. */
    public static DatabaseConnection getInstance() {
        return Holder.INSTANCE;
    }

    /** JDBC URL — dùng nội bộ bởi Flyway. */
    public String getUrl()      { return url;      }

    /** Username — dùng nội bộ bởi Flyway. Có thể {@code null} nếu dùng Windows Auth. */
    public String getUsername() { return username; }

    /** Password — dùng nội bộ bởi Flyway. Có thể {@code null} nếu dùng Windows Auth. */
    public String getPassword() { return password; }

    // ------------------------------------------------------------------ //
    //  Mở kết nối                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Mở và trả về một {@link Connection} mới tới SQL Server.
     * Người gọi có trách nhiệm đóng connection (dùng try-with-resources).
     *
     * @return connection đang mở
     * @throws SQLException nếu kết nối thất bại
     */
    public Connection getConnection() throws SQLException {
        if (username == null) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }

    // ------------------------------------------------------------------ //
    //  Tiện ích                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Kiểm tra kết nối tới database còn sống hay không.
     *
     * @return {@code true} nếu ping thành công
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Kết nối thất bại: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ //
    //  Nội bộ                                                              //
    // ------------------------------------------------------------------ //

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
