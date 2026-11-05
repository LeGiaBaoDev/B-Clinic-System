package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;

import com.quanlynhakhoa.util.*;
import com.quanlynhakhoa.ui.dialog.DlgVoucher;
import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: database riêng và JVM riêng, không khởi tạo pool của DB ứng dụng trong JVM test. */
@EnabledIfSystemProperty(named = "bclinic.integration", matches = "true")
class H4ToanLuongIntegrationTest {
    @Test
    void toanLuongH4() throws Exception {
        Properties props = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/database.properties"))) { props.load(in); }
        String name = "BClinic_Test_" + UUID.randomUUID().toString().replace("-", "");
        if (!name.matches("BClinic_Test_[a-f0-9]{32}")) { throw new IllegalStateException("Tên DB test không hợp lệ."); }
        String base = "jdbc:sqlserver://" + props.getProperty("db.host", "localhost") + ":"
                + props.getProperty("db.port", "1433") + ";encrypt=true;trustServerCertificate=true;loginTimeout=10;socketTimeout=20000;"
                + (Boolean.parseBoolean(props.getProperty("db.integratedSecurity", "false"))
                        ? "integratedSecurity=true;" : "") + "databaseName=";
        Path directory = Files.createTempDirectory(Path.of("target"), "h4-toan-luong-");
        Path config = directory.resolve("database.properties");
        Path output = directory.resolve("result.log");
        Process child = null;
        boolean created = false;
        try (Connection master = DriverManager.getConnection(base + "master",
                props.getProperty("db.username"), props.getProperty("db.password"))) {
            try {
                try (var s = master.createStatement()) { s.setQueryTimeout(20); s.execute("CREATE DATABASE [" + name + "]"); created = true; }
                Flyway.configure().dataSource(base + name, props.getProperty("db.username"),
                        props.getProperty("db.password")).locations("classpath:db/migration").load().migrate();
                props.setProperty("db.name", name);
                try (var out = Files.newOutputStream(config)) { props.store(out, "Isolated H4_TOAN_LUONG verification"); }
                String cp = directory.toAbsolutePath() + java.io.File.pathSeparator
                        + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                child = new ProcessBuilder(java, "-Duser.timezone=Asia/Ho_Chi_Minh", "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-cp", cp,
                        Scenario.class.getName(), name).redirectErrorStream(true).redirectOutput(output.toFile()).start();
                assertTrue(child.waitFor(180, TimeUnit.SECONDS), "H4_TOAN_LUONG integration quá 180 giây.");
                String result = Files.readString(output);
                System.out.println(result);
                assertEquals(0, child.exitValue(), result);
                assertTrue(result.contains("H4_TOAN_LUONG_INTEGRATION_OK"), result);
            } finally {
                if (child != null && child.isAlive()) {
                    child.destroyForcibly();
                    if (!child.waitFor(10, TimeUnit.SECONDS)) { throw new IllegalStateException("JVM test chưa dừng."); }
                }
                if (created) {
                    try (var s = master.createStatement()) {
                        s.setQueryTimeout(20);
                        s.execute("ALTER DATABASE [" + name + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                        s.execute("DROP DATABASE [" + name + "]");
                    }
                    System.out.println("TEMP_TEST_DB_REMOVED");
                }
            }
        } finally {
            Files.deleteIfExists(config);
            // result.log không chứa mật khẩu; giữ lại để xem bằng chứng khi test lỗi.
        }
    }

    public static class Scenario {
        public static void main(String[] args) throws Exception { H4ToanLuongScenario.run(args[0]); }
    }
}
