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

    private HikariDataSource dataSource;
    private boolean dangKhoiPhuc;
    private final Properties thuocTinh = loadProperties();

    private DatabaseConnection() {
        dataSource = taoPool();
    }

    private HikariDataSource taoPool() {
        Properties props = thuocTinh;

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

        return new HikariDataSource(config);
    }

    private static class Holder {
        private static final DatabaseConnection INSTANCE = new DatabaseConnection();
    }

    public static DatabaseConnection getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized HikariDataSource getDataSource() {
        return dataSource;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (dangKhoiPhuc) { throw new SQLException("Database đang khôi phục; vui lòng thử lại sau."); }
        if (dataSource.isClosed()) { dataSource = taoPool(); }
        return dataSource.getConnection();
    }

    /** Kết nối quản trị tới master, không đưa vào pool của database ứng dụng. */
    public synchronized Connection getMasterConnection() throws SQLException {
        String url = dataSource.getJdbcUrl().replaceFirst("(?i)databaseName=[^;]+", "databaseName=master");
        if (Boolean.parseBoolean(thuocTinh.getProperty("db.integratedSecurity", "false"))) {
            return java.sql.DriverManager.getConnection(url);
        }
        return java.sql.DriverManager.getConnection(url,thuocTinh.getProperty("db.username"),thuocTinh.getProperty("db.password"));
    }

    public String getDatabaseName() { return thuocTinh.getProperty("db.name", "BClinic"); }

    /** Chặn restore nếu còn giao dịch, đóng pool và mở lại khi kết thúc bảo trì. */
    public synchronized AutoCloseable tamDungDeKhoiPhuc() throws SQLException {
        if (dangKhoiPhuc || dataSource.getHikariPoolMXBean().getActiveConnections() > 0) {
            throw new SQLException("Còn thao tác đang dùng database. Vui lòng đợi trước khi khôi phục.");
        }
        dangKhoiPhuc = true;
        dataSource.close();
        return () -> {
            synchronized (DatabaseConnection.this) {
                try { dataSource = taoPool(); } finally { dangKhoiPhuc = false; }
            }
        };
    }

    /** DataSource ổn định cho DAO: instance DAO cũ vẫn lấy pool mới sau restore. */
    public javax.sql.DataSource getDaoDataSource() {
        return new javax.sql.DataSource() {
            public Connection getConnection() throws SQLException { return DatabaseConnection.this.getConnection(); }
            public Connection getConnection(String u,String p) throws SQLException { throw new java.sql.SQLFeatureNotSupportedException(); }
            public java.io.PrintWriter getLogWriter() { return null; }
            public void setLogWriter(java.io.PrintWriter writer) { }
            public void setLoginTimeout(int seconds) { }
            public int getLoginTimeout() { return 30; }
            public java.util.logging.Logger getParentLogger() { return java.util.logging.Logger.getLogger(DatabaseConnection.class.getName()); }
            public <T> T unwrap(Class<T> type) throws SQLException { throw new java.sql.SQLFeatureNotSupportedException(); }
            public boolean isWrapperFor(Class<?> type) { return false; }
        };
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
        java.nio.file.Path external = java.nio.file.Path.of(System.getProperty("bclinic.config", PROPS_FILE));
        if (!java.nio.file.Files.isRegularFile(external) && System.getProperty("bclinic.config") == null) {
            try {
                java.nio.file.Path location = java.nio.file.Path.of(DatabaseConnection.class
                        .getProtectionDomain().getCodeSource().getLocation().toURI());
                if (java.nio.file.Files.isRegularFile(location)) external = location.getParent().resolve(PROPS_FILE);
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException("Không xác định được thư mục ứng dụng", e);
            }
        }
        try (InputStream is = java.nio.file.Files.isRegularFile(external)
                ? java.nio.file.Files.newInputStream(external)
                : DatabaseConnection.class
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
