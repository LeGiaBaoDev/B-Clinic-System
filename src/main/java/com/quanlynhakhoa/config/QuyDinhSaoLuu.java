package com.quanlynhakhoa.config;
import java.nio.file.Path;

/** Đường dẫn backup là phía SQL Server; file lịch sử là phía ứng dụng. */
public record QuyDinhSaoLuu(String thuMucMayChu,Path fileLichSu,int timeoutGiay) {
    public QuyDinhSaoLuu {
        if(thuMucMayChu==null || thuMucMayChu.isBlank() || fileLichSu==null || timeoutGiay<1
                || thuMucMayChu.contains("'") || thuMucMayChu.contains(";") || thuMucMayChu.contains("..")) {
            throw new IllegalArgumentException("Cấu hình sao lưu không hợp lệ.");
        }
    }
}
