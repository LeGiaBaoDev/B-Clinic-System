package com.quanlynhakhoa.exception;

/**
 * Ném khi vi phạm quy định nghiệp vụ (tài khoản bị khóa,
 * sai mật khẩu, trùng lịch...).
 * UI bắt và hiển thị thông báo cảnh báo cho người dùng.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
