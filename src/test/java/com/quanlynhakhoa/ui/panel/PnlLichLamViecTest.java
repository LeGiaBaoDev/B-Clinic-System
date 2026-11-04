package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PnlLichLamViecTest {
    @Test void caCaNgay_buoiChieu_chiHangChieuConDangTruc() {
        LocalDate day=LocalDate.of(2026,11,4);
        var shift=new LichLamViec("LLV00001",day,CaTruc.CA_NGAY,LocalTime.of(8,0),LocalTime.of(17,30),
                TrangThaiLichLamViec.DA_PHAN_CONG,null,dentist.getNhanVien());
        assertEquals("Đã qua ca",PnlLichLamViec.trangThaiTrongBuoi(shift,1,day.atTime(15,0)));
        assertEquals("Đang trực",PnlLichLamViec.trangThaiTrongBuoi(shift,2,day.atTime(15,0)));
        assertEquals("Đã qua ca",PnlLichLamViec.trangThaiTrongBuoi(shift,1,day.atTime(12,0)));
        assertEquals("Đã phân ca",PnlLichLamViec.trangThaiTrongBuoi(shift,2,day.atTime(12,30)));
        assertEquals("Đang trực",PnlLichLamViec.trangThaiTrongBuoi(shift,2,day.atTime(13,30)));
        assertEquals("Đã qua ca",PnlLichLamViec.trangThaiTrongBuoi(shift,2,day.atTime(17,30)));
        shift.nghiPhep("Việc gia đình");
        assertEquals("Nghỉ phép",PnlLichLamViec.trangThaiTrongBuoi(shift,1,day.atTime(15,0)));
    }
    private PnlLichLamViec panel;
    private final TaiKhoan manager=tk("001",VaiTro.QUANLY,true);
    private final TaiKhoan dentist=tk("002",VaiTro.NHASI,true);
    private final TaiKhoan locked=tk("003",VaiTro.NHASI,false);
    private final AtomicInteger loads=new AtomicInteger();

    private static TaiKhoan tk(String id,VaiTro role,boolean active) {
        return new TaiKhoan("TK"+id,"test"+id,"[Ẩn]",role,active,
            new NhanVien("NV"+id,"Nguyễn Văn Anh",true,"0912345678","test@example.com","Nội nha"));
    }
    @BeforeEach void login() throws Exception {
        SwingUtilities.invokeAndWait(() -> com.formdev.flatlaf.FlatLightLaf.setup());
        SessionManager.getInstance().login(manager);
    }
    @AfterEach void logout() { SessionManager.getInstance().logout(); }

    private void open(java.util.function.Predicate<LichLamViec> save) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            panel=new PnlLichLamViec(()->{ assertFalse(SwingUtilities.isEventDispatchThread()); loads.incrementAndGet(); return List.of(manager,dentist,locked); },List::of,save);
            panel.lamMoiDuLieu();
        });
        await(()->combo("nhanVienPhanCong").isEnabled());
        SwingUtilities.invokeAndWait(()->{
            combo("caPhanCong").setSelectedIndex(0);
            ((com.toedter.calendar.JDateChooser)find(panel,"ngayPhanCong")).setDate(
                    Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        });
    }
    @Test void luu_caChieu_guiDungNhanVienNgayGio_vaTaiLaiSauThanhCong() throws Exception {
        AtomicReference<LichLamViec> saved=new AtomicReference<>();
        open(x->{ assertFalse(SwingUtilities.isEventDispatchThread()); saved.set(x); return true; });
        SwingUtilities.invokeAndWait(()->{
            assertEquals(2,combo("nhanVienPhanCong").getItemCount());
            combo("viTri").setSelectedItem("Nha sĩ");
            assertEquals(1,combo("nhanVienPhanCong").getItemCount());
            combo("caPhanCong").setSelectedIndex(1); button().doClick();
        });
        await(()->loads.get()==2&&button().isEnabled());
        assertEquals("NV002",saved.get().getNhanVien().getMaNV());
        assertEquals(CaTruc.CA_CHIEU,saved.get().getCaTruc());
        assertEquals(LocalTime.of(13,30),saved.get().getGioBatDau());
        assertEquals(LocalTime.of(17,30),saved.get().getGioKetThuc());
        assertEquals(TrangThaiLichLamViec.DA_PHAN_CONG,saved.get().getTrangThai());
        assertTrue(message().contains("thành công"));
    }
    @Test void chonNgay_dungCalendar_khoaNgayQuaKhu_vaKhongNhapTay() throws Exception {
        open(x->true);
        SwingUtilities.invokeAndWait(()->{
            Component field=find(panel,"ngayPhanCong");
            assertInstanceOf(com.toedter.calendar.JDateChooser.class,field);
            var calendar=(com.toedter.calendar.JDateChooser)field;
            assertEquals(LocalDate.now(),calendar.getMinSelectableDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            assertFalse(((JTextField)calendar.getDateEditor().getUiComponent()).isEditable());
            assertNotNull(calendar.getCalendarButton());
            assertEquals(calendar.getMinSelectableDate(),calendar.getJCalendar().getMinSelectableDate());
        });
    }
    @Test void luu_ngayQuaKhu_hoacNgaySai_khongGoiDao() throws Exception {
        AtomicInteger saves=new AtomicInteger(); open(x->{saves.incrementAndGet();return true;});
        SwingUtilities.invokeAndWait(()->{
            com.toedter.calendar.JDateChooser s=(com.toedter.calendar.JDateChooser)find(panel,"ngayPhanCong");
            Date past=Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            s.setDate(past);

            button().doClick(); assertTrue(message().contains("quá khứ"));
            s.setDate(null);
            button().doClick(); assertTrue(message().contains("hợp lệ"));
        });
        assertEquals(0,saves.get());
    }
    @Test void luu_trungCa_hienLoiVaGiuNhapLieu_deThuLai() throws Exception {
        AtomicInteger saves=new AtomicInteger();
        open(x->{saves.incrementAndGet();throw new BusinessException("Nhân viên đã được phân công ca này trong ngày.");});
        SwingUtilities.invokeAndWait(()->{combo("caPhanCong").setSelectedIndex(2);button().doClick();});
        await(()->button().isEnabled()&&message().contains("đã được phân công"));
        SwingUtilities.invokeAndWait(()->assertEquals(2,combo("caPhanCong").getSelectedIndex()));
        assertEquals(1,saves.get()); assertEquals(1,loads.get());
    }
    @Test void luu_dangXuLy_chanGuiLapVaLamMoi() throws Exception {
        CountDownLatch release=new CountDownLatch(1); AtomicInteger saves=new AtomicInteger();
        open(x->{ saves.incrementAndGet();try { assertTrue(release.await(5,TimeUnit.SECONDS)); }catch(InterruptedException ex){ throw new RuntimeException(ex); }return true; });
        try {
            SwingUtilities.invokeAndWait(()->{button().doClick();button().doClick();panel.lamMoiDuLieu();assertFalse(button().isEnabled());});
            assertEquals(1,loads.get());
        } finally {release.countDown();}
        await(()->loads.get()==2&&button().isEnabled()); assertEquals(1,saves.get());
    }
    @Test void taiDuLieu_khongPhaiQuanLy_khongGoiDao() throws Exception {
        SessionManager.getInstance().login(dentist);
        SwingUtilities.invokeAndWait(()->{
            panel=new PnlLichLamViec(()->{fail("Không được tải dữ liệu");return List.of();},List::of,x->{fail();return true;});
            panel.lamMoiDuLieu(); assertFalse(button().isEnabled()); assertTrue(message().contains("Chỉ Quản lý"));
        });
    }
    @Test void luu_loiHeThong_khongLoNoiDungKyThuat() throws Exception {
        open(x->{throw new IllegalStateException("private-db-detail");});
        SwingUtilities.invokeAndWait(()->button().doClick());
        await(()->button().isEnabled()&&message().contains("Vui lòng thử lại"));
        assertFalse(message().contains("private-db-detail"));
    }
    @Test void render_kichThuocNho_formVaNutLuuConTrongKhung() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            TaiKhoan reception=tk("003",VaiTro.LETAN,true);
            dentist.getNhanVien().setHoTenNV("Lê Mai Hương"); reception.getNhanVien().setHoTenNV("Nguyễn Thảo Linh");
            TaiKhoan second=tk("004",VaiTro.NHASI,true); second.getNhanVien().setHoTenNV("Trần Hoàng Nam");
            java.util.List<LichLamViec> shifts=new java.util.ArrayList<>();
            LocalDate monday=LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for(int d=0;d<6;d++) for(TaiKhoan tk:List.of(dentist,second,reception)) {
                shifts.add(new LichLamViec(String.format("LLV%05d",shifts.size()+1),monday.plusDays(d),CaTruc.CA_NGAY,
                        LocalTime.of(8,0),LocalTime.of(17,30),TrangThaiLichLamViec.DA_PHAN_CONG,null,tk.getNhanVien()));
            }
            shifts.get(0).nghiPhep("Nghỉ phép kiểm tra màu hiển thị");
            panel=new PnlLichLamViec(()->List.of(manager,dentist,second,reception),()->shifts,x->true);
            panel.lamMoiDuLieu();
        });
        await(()->combo("nhanVienPhanCong").isEnabled());
        SwingUtilities.invokeAndWait(()->{
            JFrame frame = new JFrame(); frame.setUndecorated(true); frame.setContentPane(panel);
            frame.setSize(1100,620); frame.setVisible(true); layout(panel);
            try {
            Rectangle r=SwingUtilities.convertRectangle(find(panel,"moPhanCa").getParent(),find(panel,"moPhanCa").getBounds(),panel);
            assertTrue(new Rectangle(0,0,panel.getWidth(),panel.getHeight()).contains(r));
            BufferedImage img=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img.createGraphics(); panel.printAll(g); g.dispose();
            try {ImageIO.write(img,"png",Path.of("target","h2-phan-cong-preview.png").toFile());} catch(Exception ex){throw new RuntimeException(ex);}
            frame.setSize(900,500); frame.validate(); layout(panel);
            Rectangle small=SwingUtilities.convertRectangle(find(panel,"moPhanCa").getParent(),find(panel,"moPhanCa").getBounds(),panel);
            assertTrue(new Rectangle(0,0,900,500).contains(small));
            BufferedImage smallImage=new BufferedImage(900,500,BufferedImage.TYPE_INT_RGB);
            Graphics2D smallGraphics=smallImage.createGraphics(); panel.printAll(smallGraphics); smallGraphics.dispose();
            try {ImageIO.write(smallImage,"png",Path.of("target","h2-phan-cong-small.png").toFile());} catch(Exception ex){throw new RuntimeException(ex);}
            } finally { frame.dispose(); }
        });
    }
    @Test void locChucVu_chiHienTheDungVaiTro_vaCaNgayCoTrongHaiBuoi() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            TaiKhoan receptionist=tk("004",VaiTro.LETAN,true);
            var morning=new LichLamViec("LLV00001",LocalDate.now(),CaTruc.CA_SANG,LocalTime.of(8,0),LocalTime.NOON,TrangThaiLichLamViec.DA_PHAN_CONG,null,dentist.getNhanVien());
            var full=new LichLamViec("LLV00002",LocalDate.now(),CaTruc.CA_NGAY,LocalTime.of(8,0),LocalTime.of(17,30),TrangThaiLichLamViec.DA_PHAN_CONG,null,receptionist.getNhanVien());
            panel=new PnlLichLamViec(()->List.of(dentist,receptionist),()->List.of(morning,full),x->true); panel.lamMoiDuLieu();
        });
        await(()->combo("nhanVienPhanCong").isEnabled());
        SwingUtilities.invokeAndWait(()->{
            combo("locChucVuLich").setSelectedItem("Lễ tân");
            assertNull(find(panel,"ca-LLV00001"));
            assertNotNull(find(panel,"ca-LLV00002"));
            assertEquals("Lễ tân",((JLabel)find((Container)find(panel,"ca-LLV00002"),"chucVuCa")).getText());
            assertEquals(2,demThe((Container)find(panel,"lichTuan"),"ca-LLV00002"));
        });
    }
    private static int demThe(Container root,String name) {
        int n=0; for(Component child:root.getComponents()) { if(name.equals(child.getName()))n++; if(child instanceof Container next)n+=demThe(next,name); } return n;
    }
    @Test void chonCa_daNghiPhepHoacPhanCong_chanNgayTrenForm_theoMaNhanVien() throws Exception {
        AtomicInteger saves=new AtomicInteger(); LocalDate day=LocalDate.now().plusDays(1);
        SwingUtilities.invokeAndWait(()->{
            var leave=new LichLamViec("LLV00001",day,CaTruc.CA_SANG,LocalTime.of(8,0),LocalTime.NOON,
                    TrangThaiLichLamViec.NGHI_PHEP,"Việc gia đình",dentist.getNhanVien());
            var assigned=new LichLamViec("LLV00002",day.plusDays(1),CaTruc.CA_CHIEU,LocalTime.of(13,30),LocalTime.of(17,30),
                    TrangThaiLichLamViec.DA_PHAN_CONG,null,dentist.getNhanVien());
            panel=new PnlLichLamViec(()->List.of(manager,dentist),()->List.of(leave,assigned),x->{saves.incrementAndGet();return true;});
            panel.lamMoiDuLieu();
        });
        await(()->combo("nhanVienPhanCong").isEnabled());
        SwingUtilities.invokeAndWait(()->{
            var date=(com.toedter.calendar.JDateChooser)find(panel,"ngayPhanCong");
            combo("caPhanCong").setSelectedIndex(0);
            date.setDate(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            combo("nhanVienPhanCong").setSelectedItem(dentist);
            assertFalse(button().isEnabled()); assertTrue(message().toLowerCase().contains("nghỉ phép"));
            button().doClick();
            combo("caPhanCong").setSelectedIndex(1); assertTrue(button().isEnabled());
            combo("caPhanCong").setSelectedIndex(2); assertFalse(button().isEnabled());
            combo("nhanVienPhanCong").setSelectedItem(manager); assertTrue(button().isEnabled()); // cùng tên, khác mã
            combo("nhanVienPhanCong").setSelectedItem(dentist);
            date.setDate(Date.from(day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            combo("caPhanCong").setSelectedIndex(1);
            assertFalse(button().isEnabled()); assertTrue(message().contains("đã được phân công"));
            combo("caPhanCong").setSelectedIndex(0); assertTrue(button().isEnabled());
        });
        assertEquals(0,saves.get());
    }
    @Test void calendar_chonCaTruoc_khoaDungNgayNghiVaNgayDaPhanCong() throws Exception {
        LocalDate day=LocalDate.now().plusMonths(1).withDayOfMonth(10);
        SwingUtilities.invokeAndWait(()->{
            var leave=new LichLamViec("LLV00001",day,CaTruc.CA_SANG,LocalTime.of(8,0),LocalTime.NOON,
                    TrangThaiLichLamViec.NGHI_PHEP,"Việc gia đình",dentist.getNhanVien());
            var assigned=new LichLamViec("LLV00002",day.plusDays(1),CaTruc.CA_CHIEU,LocalTime.of(13,30),LocalTime.of(17,30),
                    TrangThaiLichLamViec.DA_PHAN_CONG,null,dentist.getNhanVien());
            panel=new PnlLichLamViec(()->List.of(manager,dentist),()->List.of(leave,assigned),x->true);
            panel.lamMoiDuLieu();
        });
        await(()->combo("nhanVienPhanCong").isEnabled());
        SwingUtilities.invokeAndWait(()->{
            var date=(com.toedter.calendar.JDateChooser)find(panel,"ngayPhanCong");
            assertEquals(-1,combo("caPhanCong").getSelectedIndex());
            assertFalse(date.isEnabled()); assertFalse(button().isEnabled());
            combo("nhanVienPhanCong").setSelectedItem(dentist);
            combo("caPhanCong").setSelectedIndex(0); assertTrue(date.isEnabled());
            date.getJCalendar().setCalendar(java.util.GregorianCalendar.from(day.atStartOfDay(ZoneId.systemDefault())));
            assertFalse(nutNgay(date,10).isEnabled()); assertTrue(nutNgay(date,11).isEnabled());
            combo("caPhanCong").setSelectedIndex(1);
            assertTrue(nutNgay(date,10).isEnabled()); assertFalse(nutNgay(date,11).isEnabled());
            combo("caPhanCong").setSelectedIndex(2);
            assertFalse(nutNgay(date,10).isEnabled()); assertFalse(nutNgay(date,11).isEnabled());
            combo("nhanVienPhanCong").setSelectedItem(manager);
            assertTrue(nutNgay(date,10).isEnabled()); assertTrue(nutNgay(date,11).isEnabled());
            var grid=(GridBagLayout)date.getParent().getLayout();
            assertTrue(grid.getConstraints(combo("caPhanCong")).gridy<grid.getConstraints(date).gridy);
        });
    }
    private static JButton nutNgay(com.toedter.calendar.JDateChooser date,int day) {
        for(Component c:date.getJCalendar().getDayChooser().getDayPanel().getComponents())
            if(c instanceof JButton b&&String.valueOf(day).equals(b.getText())&&b.isVisible())return b;
        throw new AssertionError("Không tìm thấy nút ngày "+day);
    }
    private static void layout(Container c) { c.doLayout(); for(Component x:c.getComponents()) if(x instanceof Container next) layout(next); }
    private JComboBox<?> combo(String name) { return (JComboBox<?>)find(panel,name); }
    private JButton button() { return (JButton)find(panel,"luuPhanCong"); }
    private String message() { return ((JLabel)find(panel,"thongBaoPhanCong")).getText(); }
    private static Component find(Container root,String name) {
        for(Component c:root.getComponents()) { if(name.equals(c.getName()))return c;
            if(c instanceof Container next) { Component found=find(next,name); if(found!=null)return found; }
        } return null;
    }
    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        for(int i=0;i<100;i++) {AtomicBoolean done=new AtomicBoolean();SwingUtilities.invokeAndWait(()->done.set(condition.getAsBoolean()));if(done.get())return;Thread.sleep(30);}
        fail("SwingWorker chưa hoàn tất");
    }
}







