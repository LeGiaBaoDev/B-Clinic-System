package com.quanlynhakhoa.util;

import com.quanlynhakhoa.config.MailConfig;
import com.quanlynhakhoa.exception.BusinessException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Gửi email HTML qua SMTP; cấu hình lấy từ {@link MailConfig}. */
public final class GuiMail {
    private static final int TIMEOUT_MS = 10_000;
    private static final String TEN_NGUOI_GUI = "BClinic";

    private GuiMail() {
    }

    public static void guiOtp(String emailNhan, String otp) {
        gui(emailNhan, "BClinic - Mã xác thực đặt lại mật khẩu", noiDungOtpHtml(otp));
    }

    public static void gui(String emailNhan, String tieuDe, String noiDungHtml) {
        if (emailNhan == null || emailNhan.isBlank()) {
            throw new BusinessException("Tài khoản chưa có email nhận mã xác thực.");
        }
        if (!MailConfig.daCauHinh()) {
            throw new BusinessException(
                    "Chưa cấu hình email gửi. Vui lòng kiểm tra mail.properties hoặc biến môi trường.");
        }
        try {
            MimeMessage message = new MimeMessage(taoSession());
            message.setFrom(new InternetAddress(
                    MailConfig.from(), TEN_NGUOI_GUI, StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailNhan.trim()));
            message.setSubject(tieuDe, StandardCharsets.UTF_8.name());
            message.setContent(noiDungHtml, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new BusinessException("Không gửi được email xác thực. Vui lòng thử lại sau.", e);
        }
    }

    private static Session taoSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", MailConfig.host());
        props.put("mail.smtp.port", String.valueOf(MailConfig.port()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", String.valueOf(TIMEOUT_MS));
        props.put("mail.smtp.timeout", String.valueOf(TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(TIMEOUT_MS));
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MailConfig.username(), MailConfig.password());
            }
        });
    }

    private static String noiDungOtpHtml(String otp) {
        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;color:#333;max-width:480px">
                  <h2 style="color:#1A7A7A;margin:0 0 12px">BClinic - Đặt lại mật khẩu</h2>
                  <p>Mã xác thực của bạn là:</p>
                  <p style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#1A7A7A;margin:16px 0">%s</p>
                  <p>Mã có hiệu lực trong <b>5 phút</b>. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>
                  <p style="color:#888;font-size:12px">Email tự động từ hệ thống BClinic, vui lòng không trả lời.</p>
                </div>
                """.formatted(otp);
    }
}
