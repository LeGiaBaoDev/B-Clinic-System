package com.quanlynhakhoa.exception;

/**
 * Ném khi có lỗi truy cập cơ sở dữ liệu (bọc SQLException).
 * DAO ném, UI bắt và hiển thị thông báo tiếng Việt cho người dùng.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
