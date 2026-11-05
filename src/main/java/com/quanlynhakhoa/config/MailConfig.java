package com.quanlynhakhoa.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Cấu hình SMTP. Ưu tiên biến môi trường, sau đó mới đọc file mail.properties
 * đặt cạnh thư mục chạy (ngoài jar) để không nhúng mật khẩu vào bản phát hành.
 */
public final class MailConfig {
    private static final String FILE = "mail.properties";
    private static final String ENV_USER = "BCLINIC_SMTP_USER";
    private static final String ENV_PASS = "BCLINIC_SMTP_PASS";

    private static final Properties FILE_PROPS = docFileNgoaiJar();

    private MailConfig() {
    }

    public static String host() {
        return giaTri("mail.host", "smtp.gmail.com");
    }

    public static int port() {
        String giaTri = giaTri("mail.port", "587");
        try {
            return Integer.parseInt(giaTri);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("mail.port không hợp lệ: " + giaTri);
        }
    }

    public static String username() {
        String ten = System.getenv(ENV_USER);
        return ten != null && !ten.isBlank() ? ten.trim() : giaTri("mail.username", null);
    }

    /** App Password Google hiển thị theo nhóm 4 ký tự; bỏ khoảng trắng trước khi dùng. */
    public static String password() {
        String matKhau = System.getenv(ENV_PASS);
        if (matKhau == null || matKhau.isBlank()) {
            matKhau = giaTri("mail.password", null);
        }
        return matKhau == null ? null : matKhau.replaceAll("\\s+", "");
    }

    public static String from() {
        String nguoiGui = giaTri("mail.from", null);
        return nguoiGui != null && !nguoiGui.isBlank() ? nguoiGui.trim() : username();
    }

    public static boolean daCauHinh() {
        String ten = username();
        String matKhau = password();
        return ten != null && !ten.isBlank() && matKhau != null && !matKhau.isBlank();
    }

    private static String giaTri(String khoa, String macDinh) {
        String giaTri = FILE_PROPS.getProperty(khoa);
        return giaTri == null || giaTri.isBlank() ? macDinh : giaTri.trim();
    }

    private static Properties docFileNgoaiJar() {
        Properties props = new Properties();
        Path duongDan = Path.of(FILE);
        if (!Files.isRegularFile(duongDan)) {
            return props;
        }
        try (InputStream is = Files.newInputStream(duongDan)) {
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Lỗi đọc file cấu hình: " + duongDan.toAbsolutePath(), e);
        }
        return props;
    }
}
