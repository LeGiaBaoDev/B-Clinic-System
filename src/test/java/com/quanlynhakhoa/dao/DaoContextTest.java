package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

class DaoContextTest {
    @Test void transaction_loiNghiepVu_rollbackVaKhoiPhucConnection() {
        var calls = new ArrayList<String>();
        Connection c = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class},
                (p,m,a) -> { calls.add(m.getName() + (a == null ? "" : java.util.Arrays.toString(a)));
                    return switch(m.getName()) { case "getAutoCommit" -> true; case "isClosed" -> false;
                        default -> null; }; });
        DataSource ds = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{DataSource.class},
                (p,m,a) -> c);
        var context = new DaoContext(ds, () -> null, Clock.systemUTC());
        assertThrows(BusinessException.class, () -> context.transaction(conn -> { throw new BusinessException("Lỗi thử"); }));
        assertTrue(calls.contains("rollback"));
        assertFalse(calls.contains("commit"));
        assertTrue(calls.contains("setAutoCommit[true]"));
        assertTrue(calls.contains("close"));
    }
}
