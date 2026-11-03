package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.util.SessionManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Supplier;
import javax.sql.DataSource;

/** Nguồn kết nối, phiên và thời gian; có thể thay bằng database test riêng. */
public final class DaoContext {
    private final DataSource dataSource;
    private final Supplier<TaiKhoan> nguoiDung;
    private final Clock clock;

    public DaoContext(DataSource dataSource, Supplier<TaiKhoan> nguoiDung, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.nguoiDung = Objects.requireNonNull(nguoiDung);
        this.clock = Objects.requireNonNull(clock);
    }

    public static DaoContext macDinh() {
        return new DaoContext(DatabaseConnection.getInstance().getDaoDataSource(),
                () -> SessionManager.getInstance().getCurrentUser(), Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    public DataSource dataSource() { return dataSource; }
    public TaiKhoan nguoiDung() { return nguoiDung.get(); }
    public LocalDateTime bayGio() { return LocalDateTime.now(clock); }

    @FunctionalInterface
    public interface CongViec<T> { T chay(Connection connection) throws SQLException; }

    /** Chỉ đọc, tự đóng connection sau khi trả kết quả đã map. */
    public <T> T doc(CongViec<T> viec) {
        try (Connection c = dataSource.getConnection()) {
            return viec.chay(c);
        } catch (SQLException e) {
            throw loiSQL(e);
        }
    }

    /** Rollback cả SQLException lẫn lỗi nghiệp vụ; trả trạng thái connection về pool. */
    public <T> T transaction(CongViec<T> viec) {
        try (Connection c = dataSource.getConnection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            Throwable loi = null;
            try {
                T result = viec.chay(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                loi = e;
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            } finally {
                try { c.setAutoCommit(auto); } catch (SQLException reset) {
                    if (loi != null) { loi.addSuppressed(reset); } else { throw reset; }
                }
            }
        } catch (SQLException e) {
            throw loiSQL(e);
        }
    }

    static RuntimeException loiSQL(SQLException e) {
        if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
            return new BusinessException("Dữ liệu bị trùng mã, số điện thoại, email hoặc tên đăng nhập.", e);
        }
        if (e.getErrorCode() == 547) {
            return new BusinessException("Dữ liệu không hợp lệ hoặc đang được sử dụng bởi bản ghi khác.", e);
        }
        return new DataAccessException("Không thể truy cập cơ sở dữ liệu.", e);
    }
}
