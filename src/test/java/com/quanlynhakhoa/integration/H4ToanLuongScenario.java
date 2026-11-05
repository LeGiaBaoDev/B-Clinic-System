package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.dao.BaoCaoQuanLyDAO.*;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Toàn luồng DAO thật; chỉ precondition/khối lượng lớn/điểm đồng bộ dùng fixture SQL. */
final class H4ToanLuongScenario {
    private static final LocalDate DAY = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LichHenDAO LH = new LichHenDAO();
    private static final PhieuKhamDAO PK = new PhieuKhamDAO();
    private static final HoaDonDAO HD = new HoaDonDAO();
    private static final TongQuanQuanLyDAO DASH = new TongQuanQuanLyDAO();
    private static final BaoCaoQuanLyDAO REPORT = new BaoCaoQuanLyDAO();
    private static final KyBaoCao PERIOD = KyBaoCao.tao(CheDo.NGAY, DAY, null);
    private record Bill(String lh, String pk, String hd) { }

    static void run(String expectedDb) throws Exception {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            assertTrue(expectedDb.matches("BClinic_Test_[a-f0-9]{32}"));
            assertEquals(expectedDb, scalar(c, "SELECT DB_NAME()"));
            sql(c, "INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES('LDV01',N'Điều trị bệnh lý',0),('LDV02',N'Vật tư/thiết bị bán kèm',5),('LDV03',N'Thẩm mỹ',10)");
            sql(c, "INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong) VALUES('DV701','LDV01',N'Khám test',100000,15),('DV702','LDV02',N'Vật tư test',200000,15),('DV703','LDV03',N'Thẩm mỹ test',300000,15)");
            sql(c, "INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc) VALUES('LLV00701','NV002',?,'CA_NGAY','07:00','17:30')", DAY);
            LocalDateTime first = LocalDateTime.now().plusMinutes(10).withSecond(0).withNano(0);
            if (first.toLocalTime().isBefore(LocalTime.of(7, 0))) first = DAY.atTime(7, 0);
            assertTrue(first.toLocalDate().equals(DAY) && first.plusMinutes(105).toLocalTime().isBefore(LocalTime.of(17, 30)),
                    "Test đặt/tiếp nhận cùng ngày cần chạy trước khoảng 15:35 giờ Việt Nam (không giả đổi ngày lịch).");
            login(VaiTro.QUANLY); money("0", REPORT.taiBaoCao(PERIOD).doanhThu());
            Bill a = bill(c, 1, first), b = bill(c, 2, first.plusMinutes(45)), pending = bill(c, 3, first.plusMinutes(90));
            login(VaiTro.QUANLY);
            var before = DASH.layTongQuanQuanLy();
            assertEquals(3, before.tongLichHen()); assertEquals(3, before.hoaDonChuaTra());
            assertEquals(3, before.daDen()); assertEquals(0, before.hoanTat());
            money("1920000", before.doanhThuDuKien()); money("0", REPORT.taiBaoCao(PERIOD).doanhThu());
            Voucher fixed = new Voucher(null, "Giảm test toàn luồng", false, 50000, 0, DAY.minusDays(1), DAY.plusDays(1), true);
            Voucher percent = new Voucher(null, "Phần trăm toàn luồng", true, 10, 0, DAY.minusDays(1), DAY.plusDays(1), true);
            assertTrue(new VoucherDAO().them(fixed)); assertTrue(new VoucherDAO().them(percent));
            login(VaiTro.LETAN);
            money("10000", HD.thanhToan(a.hd(), HoaDon.TIEN_MAT, new BigDecimal("600000"), fixed.getMaVoucher()));
            assertThrows(BusinessException.class, () -> HD.thanhToan(a.hd(), HoaDon.TIEN_MAT, new BigDecimal("600000")));
            // Gây lỗi sau cập nhật hóa đơn/lịch để kiểm chứng rollback không lên báo cáo.
            sql(c, "CREATE TRIGGER trg_H4_AuditFail ON NhatKyHeThong AFTER INSERT AS BEGIN IF EXISTS(SELECT 1 FROM inserted WHERE hanhDong='THANH_TOAN') THROW 51000,'H4 forced rollback',1; END");
            try { assertThrows(RuntimeException.class, () -> HD.thanhToan(b.hd(), HoaDon.CHUYEN_KHOAN, new BigDecimal("576000"), percent.getMaVoucher())); }
            finally { sql(c, "DROP TRIGGER trg_H4_AuditFail"); }
            assertEquals("0", scalar(c, "SELECT CAST(trangThai AS INT) FROM HoaDon WHERE maHD=?", b.hd()));
            assertEquals("DA_DEN", scalar(c, "SELECT trangThai FROM LichHen WHERE maLH=?", b.lh()));
            assertEquals("0.00", scalar(c, "SELECT tongKhuyenMai FROM HoaDon WHERE maHD=?", b.hd()));
            login(VaiTro.QUANLY);
            money("590000", REPORT.taiBaoCao(PERIOD).doanhThu());
            money("590000", DASH.layTongQuanQuanLy().daThu());
            login(VaiTro.LETAN); money("0", HD.thanhToan(b.hd(), HoaDon.CHUYEN_KHOAN, new BigDecimal("576000"), percent.getMaVoucher()));
            login(VaiTro.QUANLY);
            var report = REPORT.taiBaoCao(PERIOD); var dashboard = DASH.layTongQuanQuanLy();
            money("1166000", report.doanhThu()); money("590000", report.tienMat()); money("576000", report.chuyenKhoan());
            money("1166000", dashboard.daThu()); money("640000", dashboard.conPhaiThu()); money("1806000", dashboard.doanhThuDuKien());
            assertEquals(2, report.lichHienTai().hoanTat()); assertEquals(2, dashboard.hoanTat());
            assertEquals(6, report.dichVu().stream().mapToLong(DichVuSuDung::soLuong).sum());
            money("1280000", report.dichVu().stream().map(DichVuSuDung::tienTruocGiam).reduce(BigDecimal.ZERO, BigDecimal::add));
            System.out.println("H4_REAL_BOOK_RECEIVE_EXAM_PAYMENT_ROLLBACK_RECONCILED");
            concurrency(c, pending);
            performance(c);
            uiLoad();
            System.out.println("H4_TOAN_LUONG_INTEGRATION_OK");
        } finally { SessionManager.getInstance().logout(); DatabaseConnection.getInstance().getDataSource().close(); }
    }

    private static Bill bill(Connection c, int index, LocalDateTime time) throws Exception {
        login(VaiTro.LETAN);
        KhachHang customer = new KhachHang(null, "Khách toàn luồng " + index, LocalDate.of(1995,1,1), true,
                "090077700" + index, "Dữ liệu test", "flow" + index + "@example.com");
        assertTrue(new KhachHangDAO().them(customer, "Tiền sử mô phỏng"));
        LichHen appointment = new LichHen(null, time, TrangThaiLichHen.DA_DAT, "H4 full flow", null, customer,
                new NhanVienDAO().layTheoMa("NV002"), new DichVuDAO().layTheoMa("DV701"), null);
        assertTrue(LH.them(appointment)); assertNotNull(appointment.getMaLH());
        assertThrows(BusinessException.class, () -> LH.them(new LichHen(appointment)));
        PhieuKham pk = PK.taoTuLichHen(appointment.getMaLH());
        assertEquals("DA_DEN", scalar(c, "SELECT trangThai FROM LichHen WHERE maLH=?", appointment.getMaLH()));
        assertThrows(BusinessException.class, () -> PK.taoTuLichHen(appointment.getMaLH()));
        login(VaiTro.NHASI); assertTrue(PK.batDauKham(pk.getMaPK())); pk = PK.layTheoMa(pk.getMaPK());
        List<ChiTietPhieuKham> details = new ArrayList<>();
        for (String id : List.of("DV701", "DV702", "DV703")) {
            DichVu service = new DichVuDAO().layTheoMa(id);
            int tax = switch (service.getLoaiDichVu().getMucThueGTGT()) { case THUE_0 -> 0; case THUE_5 -> 5; case THUE_10 -> 10; };
            details.add(new ChiTietPhieuKham(0, "Triệu chứng", "Chẩn đoán", service.getDonGia(), 1, "Lần", tax, "Kết quả", pk, service, pk.getLichHen().getNhaSi()));
        }
        PK.hoanTatPhieuKham(pk, details);
        String invoice = scalar(c, "SELECT maHD FROM HoaDon WHERE maPK=?", pk.getMaPK());
        login(VaiTro.LETAN); money("640000", HD.tinhTongTien(invoice));
        assertEquals("HOAN_TAT", scalar(c, "SELECT trangThai FROM PhieuKham WHERE maPK=?", pk.getMaPK()));
        return new Bill(appointment.getMaLH(), pk.getMaPK(), invoice);
    }

    private static void concurrency(Connection c, Bill pending) throws Exception {
        // Trigger chỉ ở DB test: giữ transaction giữa cập nhật tiền và cập nhật lịch.
        sql(c, "CREATE TABLE H4TestSignal(id INT NOT NULL)");
        sql(c, "CREATE TRIGGER trg_H4_SlowPayment ON HoaDon AFTER UPDATE AS BEGIN IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON i.maHD=d.maHD WHERE i.trangThai=1 AND d.trangThai=0) BEGIN INSERT INTO H4TestSignal VALUES(1); WAITFOR DELAY '00:00:02'; END END");
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future<BigDecimal> payment = executor.submit(() -> HD.thanhToan(pending.hd(), HoaDon.TIEN_MAT, new BigDecimal("640000")));
        try {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (Integer.parseInt(scalar(c, "SELECT COUNT(*) FROM H4TestSignal WITH(NOLOCK)")) == 0 && System.nanoTime() < end) Thread.sleep(20);
            assertEquals("1", scalar(c, "SELECT COUNT(*) FROM H4TestSignal WITH(NOLOCK)"));
            Future<BaoCao> reportDuringPayment = executor.submit(() -> REPORT.taiBaoCao(PERIOD));
            var snapshot = DASH.layTongQuanQuanLy();
            money("0", payment.get(15, TimeUnit.SECONDS));
            assertEquals(snapshot.hoaDonDaTra(), snapshot.hoanTat(), "Dashboard không được ghép tiền sau commit với lịch trước commit");
            var report = reportDuringPayment.get(15, TimeUnit.SECONDS);
            money("1806000", report.doanhThu()); assertEquals(3, report.lichHienTai().hoanTat());
            assertEquals(9, report.dichVu().stream().mapToLong(DichVuSuDung::soLuong).sum());
            assertThrows(BusinessException.class, () -> HD.thanhToan(pending.hd(), HoaDon.TIEN_MAT, new BigDecimal("640000")));
            System.out.println("H4_PAYMENT_DASHBOARD_REPORT_CONCURRENT_SNAPSHOT_OK");
        } finally {
            try { payment.get(15, TimeUnit.SECONDS); } finally {
                executor.shutdownNow(); executor.awaitTermination(5, TimeUnit.SECONDS);
                sql(c, "DROP TRIGGER trg_H4_SlowPayment; DROP TABLE H4TestSignal");
            }
        }
    }

    private static void performance(Connection c) throws Exception {
        // Chỉ fixture khối lượng ở DB test, không tính là lượt đặt lịch nghiệp vụ.
        String prefix = """
                WITH N AS (SELECT TOP(1440) ROW_NUMBER() OVER(ORDER BY (SELECT NULL))-1 AS n FROM sys.all_objects a CROSS JOIN sys.all_objects b),
                F AS (SELECT DATEADD(DAY,-1-CAST(n/8 AS INT),CAST(? AS DATE)) AS d,
                    RIGHT('000'+CAST(701+n%8 AS VARCHAR(3)),3) AS suffix,CAST(n%8 AS INT) AS slot FROM N)
                """;
        sql(c, prefix + "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,ghiChu) SELECT 'LH'+CONVERT(CHAR(6),d,12)+suffix,'KH00001','NV002','DV701',DATEADD(HOUR,7+slot,CAST(d AS DATETIME2)),'HOAN_TAT',N'H4 performance fixture' FROM F", DAY);
        sql(c, prefix + "INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap) SELECT 'PK'+CONVERT(CHAR(6),d,12)+suffix,'HS00001','LH'+CONVERT(CHAR(6),d,12)+suffix,'NV003',DATEADD(HOUR,7+slot,CAST(d AS DATETIME2)) FROM F", DAY);
        sql(c, "INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,donViTinh,soLuong,donGia,mucThueGTGT) SELECT pk.maPK,dv.maDV,'NV002',N'Test',N'Test',N'Lần',1,dv.donGia,l.mucThueGTGT FROM PhieuKham pk JOIN LichHen lh ON lh.maLH=pk.maLH CROSS JOIN DichVu dv JOIN LoaiDichVu l ON l.maLoaiDV=dv.maLoaiDV WHERE lh.ghiChu=N'H4 performance fixture'");
        sql(c, "UPDATE pk SET trangThai='HOAN_TAT' FROM PhieuKham pk JOIN LichHen lh ON lh.maLH=pk.maLH WHERE lh.ghiChu=N'H4 performance fixture'");
        sql(c, "INSERT INTO HoaDon(maHD,maPK,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,ngayThanhToan) SELECT 'HD'+SUBSTRING(pk.maPK,3,9),pk.maPK,'NV003',pk.ngayLap,1,N'Tiền mặt',DATEADD(MINUTE,30,pk.ngayLap) FROM PhieuKham pk JOIN LichHen lh ON lh.maLH=pk.maLH WHERE lh.ghiChu=N'H4 performance fixture'");
        assertEquals("1443", scalar(c, "SELECT COUNT(*) FROM HoaDon"));
        List<Long> dashMs = new ArrayList<>(), reportMs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            long start = System.nanoTime(); var dashboard = DASH.layTongQuanQuanLy();
            dashMs.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            start = System.nanoTime(); var report = REPORT.taiBaoCao(KyBaoCao.tao(CheDo.TUY_CHON, DAY.minusDays(89), DAY));
            reportMs.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            assertEquals(3, dashboard.tongLichHen()); money("1806000", dashboard.daThu());
            assertEquals(90, report.hienTai().size()); assertEquals(90, report.kyTruoc().size());
            money("457486000", report.doanhThu()); money("460800000", report.doanhThuTruoc());
        }
        long maxDash = Collections.max(dashMs), maxReport = Collections.max(reportMs);
        System.out.println("H4_PERFORMANCE rows=1443_invoices/4329_details/180_history_days dashboard_ms=" + dashMs + " report_90days_ms=" + reportMs);
        assertTrue(maxDash <= 3000, "Dashboard vượt 3 giây trên bộ test: " + maxDash);
        assertTrue(maxReport <= 60000, "Báo cáo vượt 60 giây trên bộ test: " + maxReport);
        System.out.println("H4_PERFORMANCE_TARGETS_OK");
    }
    private static void login(VaiTro role) { SessionManager.getInstance().login(new TaiKhoanDAO().dangNhap("hieu." + role.name().toLowerCase(Locale.ROOT), "Clinic@123")); }
    private static void uiLoad() throws Exception {
        var panel = new java.util.concurrent.atomic.AtomicReference<com.quanlynhakhoa.ui.panel.PnlTongQuanQuanLy>();
        var failure = new java.util.concurrent.atomic.AtomicReference<String>();
        CountDownLatch ready = new CountDownLatch(1);
        long start = System.nanoTime();
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            com.formdev.flatlaf.FlatLightLaf.setup();
            var view = new com.quanlynhakhoa.ui.panel.PnlTongQuanQuanLy("Kiểm thử toàn luồng", s -> {}, ngay -> DASH.layTongQuanQuanLy(ngay));
            panel.set(view);
            view.langNgheTaiDuLieu(state -> {
                if (state.dangTai()) return;
                if (state.loi() != null) failure.set(state.loi());
                try { render(view, "target/h4-full-dashboard.png", 1158, 612); }
                catch (Exception e) { failure.set(e.toString()); }
                ready.countDown();
            });
            view.lamMoiDuLieu();
        });
        try {
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertNull(failure.get());
            System.out.println("H4_DASHBOARD_PANEL_LOAD_RENDER_MS=" + elapsed);
            assertTrue(elapsed <= 3000, "Panel vượt 3 giây gồm tạo component/tải/render: " + elapsed);
        } finally { javax.swing.SwingUtilities.invokeAndWait(() -> panel.get().removeNotify()); }
        var report = new java.util.concurrent.atomic.AtomicReference<com.quanlynhakhoa.ui.panel.PnlBaoCaoQuanLy>();
        start = System.nanoTime();
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            var view = new com.quanlynhakhoa.ui.panel.PnlBaoCaoQuanLy(REPORT::taiBaoCao, Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));
            report.set(view); view.lamMoiDuLieu();
        });
        try {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            var done = new java.util.concurrent.atomic.AtomicBoolean();
            while (!done.get() && System.nanoTime() < end) {
                javax.swing.SwingUtilities.invokeAndWait(() -> done.set(find(report.get(), "xuatBaoCao").isEnabled()));
                if (!done.get()) Thread.sleep(20);
            }
            assertTrue(done.get(), "Panel báo cáo phải tải xong");
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try { render(report.get(), "target/h4-full-report.png", 1158, 612); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("H4_REPORT_PANEL_LOAD_RENDER_MS=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            System.out.println("H4_REAL_PANEL_TO_SQL_RENDER_OK");
        } finally { javax.swing.SwingUtilities.invokeAndWait(() -> report.get().removeNotify()); }
    }
    private static java.awt.Component find(java.awt.Container parent, String name) {
        for (var child : parent.getComponents()) { if (name.equals(child.getName())) return child;
            if (child instanceof java.awt.Container next) { var result = find(next, name); if (result != null) return result; } }
        return null;
    }
    private static void layout(java.awt.Container parent) { parent.doLayout(); for (var child : parent.getComponents()) if (child instanceof java.awt.Container next) layout(next); }
    private static void render(javax.swing.JComponent panel, String path, int width, int height) throws Exception {
        panel.setSize(width,height); layout(panel);
        var image = new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics(); panel.printAll(graphics); graphics.dispose();
        javax.imageio.ImageIO.write(image,"png",java.nio.file.Path.of(path).toFile());
    }
    private static void money(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
    private static void sql(Connection c, String query, Object... args) throws SQLException { try (var ps = c.prepareStatement(query)) { ps.setQueryTimeout(30); JdbcHelper.bind(ps,args); ps.executeUpdate(); } }
    private static String scalar(Connection c, String query, Object... args) throws SQLException { try (var ps=c.prepareStatement(query)) { ps.setQueryTimeout(15); JdbcHelper.bind(ps,args); try (var r=ps.executeQuery()) { assertTrue(r.next()); return r.getString(1); } } }
}
