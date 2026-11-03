package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.*;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.exception.DataAccessException;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class PnlTongQuanQuanLyTest {
    private TongQuanQuanLy snapshot(boolean empty) {
        LocalDateTime now=LocalDateTime.of(2026,11,2,9,20);
        List<DoanhThuNgay> chart=new ArrayList<>();
        for(int i=6;i>=0;i--) { chart.add(new DoanhThuNgay(now.toLocalDate().minusDays(i),BigDecimal.ZERO)); }
        List<LichHenTrongNgay> rows=empty?List.of():List.of(
                new LichHenTrongNgay("LH261102001",now.plusMinutes(10),"Khách thực A","Bác sĩ A",TrangThaiLichHen.DA_DAT,null),
                new LichHenTrongNgay("LH261102002",now.minusMinutes(20),"Khách thực B","Bác sĩ B",TrangThaiLichHen.DA_HUY,null));
        return new TongQuanQuanLy(now,rows.size(),empty?0:1,0,0,empty?0:1,0,0,BigDecimal.ZERO,0,BigDecimal.ZERO,
                BigDecimal.ZERO,BigDecimal.ZERO,chart,rows,List.of(),null);
    }
    private static <T> T find(Container c,Class<T> type) {
        for(Component child:c.getComponents()) {
            if(type.isInstance(child)) { return type.cast(child); }
            if(child instanceof Container nested) { T value=find(nested,type); if(value!=null) { return value; } }
        }
        return null;
    }
    @Test void taiDuLieu_chayNgoaiEDT_vaLocBangDuLieuMoi() throws Exception {
        var background=new AtomicBoolean(); var calls=new AtomicInteger();
        var result=new LinkedBlockingQueue<PnlTongQuanQuanLy.TrangThaiTai>();
        var ref=new AtomicReference<PnlTongQuanQuanLy>();
        SwingUtilities.invokeAndWait(() -> {
            var panel=new PnlTongQuanQuanLy("Quản lý",s -> { },() -> {
                calls.incrementAndGet(); background.set(!SwingUtilities.isEventDispatchThread()); return snapshot(false);
            });
            ref.set(panel); panel.langNgheTaiDuLieu(state -> { if(!state.dangTai()) { result.offer(state); } });
            panel.lamMoiDuLieu(); panel.lamMoiDuLieu();
        });
        var state=result.poll(10,TimeUnit.SECONDS); assertNotNull(state); assertNull(state.loi());
        assertTrue(background.get()); assertEquals(1,calls.get());
        SwingUtilities.invokeAndWait(() -> {
            JTable table=find(ref.get(),JTable.class); assertEquals(2,table.getRowCount());
            assertFalse(table.isCellEditable(0,0)); ref.get().timKiem("Khách thực B"); assertEquals(1,table.getRowCount());
            assertEquals("Đã hủy",table.getValueAt(0,3));
        });
    }
    @Test void taiLaiBiLoi_giuDuLieuCu_vaBaoLoiKhongGiaSoLieu() throws Exception {
        var count=new AtomicInteger(); var result=new LinkedBlockingQueue<PnlTongQuanQuanLy.TrangThaiTai>();
        var ref=new AtomicReference<PnlTongQuanQuanLy>();
        SwingUtilities.invokeAndWait(() -> {
            var panel=new PnlTongQuanQuanLy("Quản lý",s -> { },() -> {
                if(count.incrementAndGet()>1) { throw new DataAccessException("Lỗi kết nối test"); }
                return snapshot(false);
            });
            ref.set(panel); panel.langNgheTaiDuLieu(state -> { if(!state.dangTai()) { result.offer(state); } }); panel.lamMoiDuLieu();
        });
        assertNotNull(result.poll(10,TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> ref.get().lamMoiDuLieu());
        var error=result.poll(10,TimeUnit.SECONDS); assertNotNull(error); assertNotNull(error.loi()); assertNotNull(error.duLieu());
        SwingUtilities.invokeAndWait(() -> assertEquals(2,find(ref.get(),JTable.class).getRowCount()));
    }
    @Test void databaseChuaCoDuLieu_bangRongVaKhongCoThongBaoGia() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel=new PnlTongQuanQuanLy("Quản lý",s -> { },() -> snapshot(true));
            panel.hienThiDuLieu(snapshot(true));
            assertEquals(0,find(panel,JTable.class).getRowCount());
            assertFalse(panel.coLichDangCho());
            assertEquals("Không có lịch hẹn đang chờ hoặc sắp tới trong ngày.",panel.thongBaoLichTiep());
        });
    }
}
