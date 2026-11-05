package com.quanlynhakhoa.dao.support;

import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.util.List;

/** Snapshot tổng hợp Quản lý, phối hợp cùng khóa mà các DAO ghi đang sử dụng. */
public final class DocThongKe {
    private DocThongKe() { }

    public static <T> T quanLy(JdbcHelper.Viec<T> viec) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            // Lấy khóa nghiệp vụ trước khi đọc bảng, tránh đảo thứ tự khóa với
            // thanh toán (hóa đơn -> lịch) hoặc hoàn tất khám (phiếu -> hóa đơn).
            // Shared cho nhiều người cùng xem; DAO ghi dùng Exclusive trên cùng tên.
            for (String resource : List.of("BClinic:lich", "BClinic:kham", "BClinic:thanh-toan")) {
                Integer result = JdbcHelper.one(c, """
                        DECLARE @r INT;
                        EXEC @r=sys.sp_getapplock @Resource=?,@LockMode='Shared',
                            @LockOwner='Transaction',@LockTimeout=10000;
                        SELECT @r;
                        """, r -> r.getInt(1), resource);
                if (result == null || result < 0) {
                    throw new BusinessException("Dữ liệu khám hoặc thanh toán đang được cập nhật. Vui lòng làm mới để thử lại.");
                }
            }
            // Helper commit/rollback; Hikari reset isolation khi trả connection.
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            return viec.chay(c);
        });
    }
}
