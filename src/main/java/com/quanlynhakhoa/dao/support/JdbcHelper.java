package com.quanlynhakhoa.dao.support;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Hàm JDBC dùng chung: mở connection, transaction, bind tham số, query/update,
 * map ResultSet.
 */
public final class JdbcHelper {

    private JdbcHelper() {
    }

    public interface Viec<T> {
        T chay(Connection c) throws SQLException;
    }

    public interface Mapper<T> {
        T map(ResultSet r) throws SQLException;
    }

    /**
     * Chỉ đọc: tự mở và đóng connection.
     */
    public static <T> T doc(Viec<T> viec) {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            return viec.chay(c);
        } catch (SQLException e) {
            throw loiSQL(e);
        }
    }

    /**
     * Ghi: commit khi xong, rollback khi lỗi, luôn trả connection về pool.
     */
    public static <T> T transaction(Viec<T> viec) {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            Throwable loi = null;
            try {
                T result = viec.chay(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                loi = e;
                try {
                    c.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            } finally {
                try {
                    c.setAutoCommit(auto);
                } catch (SQLException reset) {
                    if (loi != null) {
                        loi.addSuppressed(reset);
                    } else {
                        throw reset;
                    }
                }
            }
        } catch (SQLException e) {
            throw loiSQL(e);
        }
    }

    public static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object value = args[i] instanceof Enum<?> e ? e.name() : args[i];
            if (value instanceof LocalTime) {
                Connection connection = ps.getConnection();
                if (connection.isWrapperFor(com.microsoft.sqlserver.jdbc.ISQLServerConnection.class)) {
                    connection.unwrap(com.microsoft.sqlserver.jdbc.ISQLServerConnection.class)
                            .setSendTimeAsDatetime(false);
                }
                ps.setObject(i + 1, value, Types.TIME);
            } else if (value instanceof LocalDate) {
                ps.setObject(i + 1, value, Types.DATE);
            } else if (value instanceof LocalDateTime) {
                ps.setObject(i + 1, value, Types.TIMESTAMP);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    public static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setQueryTimeout(60);
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    public static <T> List<T> query(Connection c, String sql, Mapper<T> mapper, Object... args)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setQueryTimeout(60);
            bind(ps, args);
            try (ResultSet r = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (r.next()) {
                    rows.add(mapper.map(r));
                }
                return rows;
            }
        }
    }

    public static <T> T one(Connection c, String sql, Mapper<T> mapper, Object... args) throws SQLException {
        List<T> rows = query(c, sql, mapper, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public static boolean exists(Connection c, String sql, Object... args) throws SQLException {
        return one(c, sql, r -> true, args) != null;
    }

    public static String tuKhoa(String value) {
        return "%" + (value == null ? "" : value.trim()) + "%";
    }

    public static RuntimeException loiSQL(SQLException e) {
        if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
            return new BusinessException("Dữ liệu bị trùng mã, số điện thoại, email hoặc tên đăng nhập.", e);
        }
        if (e.getErrorCode() == 547) {
            return new BusinessException("Dữ liệu không hợp lệ hoặc đang được sử dụng bởi bản ghi khác.", e);
        }
        return new DataAccessException("Không thể truy cập cơ sở dữ liệu.", e);
    }
}
