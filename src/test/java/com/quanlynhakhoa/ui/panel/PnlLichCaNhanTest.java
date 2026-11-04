package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class PnlLichCaNhanTest {
    private PnlLichLamViec panel;
    private static TaiKhoan user(String id,VaiTro role) {
        return new TaiKhoan("TK"+id,"test"+id,"[Ẩn]",role,true,
                new NhanVien("NV"+id,"Nguyễn Văn Anh",true,"0912345678","test@example.com","Nội nha"));
    }
    private static LichLamViec shift(String id,NhanVien nv) {
        return new LichLamViec("LLV"+id,LocalDate.now(),CaTruc.CA_NGAY,LocalTime.of(8,0),LocalTime.of(17,30),
                TrangThaiLichLamViec.NGHI_PHEP,"Việc gia đình",nv);
    }
    @AfterEach void logout() { SessionManager.getInstance().logout(); }

    @ParameterizedTest @EnumSource(VaiTro.class)
    void caBaVaiTro_chiXemLichBanThan_khongCoNutPhanCong(VaiTro role) throws Exception {
        var me=user("001",role); var other=user("002",VaiTro.NHASI); AtomicBoolean loaded=new AtomicBoolean();
        SessionManager.getInstance().login(me);
        SwingUtilities.invokeAndWait(()->{
            panel=new PnlLichLamViec(()->List.of(me),()->{ assertFalse(SwingUtilities.isEventDispatchThread()); loaded.set(true); return List.of(shift("00001",me.getNhanVien()),shift("00002",other.getNhanVien())); },
                    x->{ fail("Lịch cá nhân không được ghi");return false; },true);
            panel.lamMoiDuLieu();
        });
        await(()->loaded.get()&&table().getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{
            assertEquals("Lịch của tôi",panel.getName());
            assertTrue(table().getValueAt(0,1).toString().contains("NV001"));
            assertEquals("Nghỉ phép",table().getValueAt(0,5));
            assertFalse(table().isCellEditable(0,0));
            assertNull(find(panel,"moPhanCa")); assertNull(find(panel,"luuPhanCong"));
            assertNull(find(panel,"moDoiCa"));assertNull(find(panel,"moNghiPhep"));
            assertNull(find(panel,"locNhanVienLich")); assertNull(find(panel,"locChucVuLich"));
            assertNotNull(find(panel,"ca-LLV00001")); assertNull(find(panel,"ca-LLV00002"));
        });
    }

    @Test void doiTaiKhoan_khongTaiHayHienLichCu() throws Exception {
        var me=user("001",VaiTro.QUANLY); AtomicInteger loads=new AtomicInteger(); SessionManager.getInstance().login(me);
        SwingUtilities.invokeAndWait(()->{
            panel=new PnlLichLamViec(()->List.of(me),()->{loads.incrementAndGet();return List.of(shift("00001",me.getNhanVien()));},x->false,true);
            panel.lamMoiDuLieu();
        });
        await(()->table().getRowCount()==1);
        SessionManager.getInstance().login(user("002",VaiTro.LETAN));
        SwingUtilities.invokeAndWait(()->{panel.lamMoiDuLieu();assertEquals(0,table().getRowCount());});
        assertEquals(1,loads.get());
    }

    @Test void render_lichCaNhan_khongCoFormQuanLy() throws Exception {
        var me=user("001",VaiTro.QUANLY); SessionManager.getInstance().login(me);
        SwingUtilities.invokeAndWait(()->{
            com.formdev.flatlaf.FlatLightLaf.setup();
            panel=new PnlLichLamViec(()->List.of(me),()->List.of(shift("00001",me.getNhanVien())),x->false,true);
            panel.lamMoiDuLieu();
        });
        await(()->table().getRowCount()==1);
        SwingUtilities.invokeAndWait(()->{
            JFrame frame=new JFrame(); frame.setUndecorated(true); frame.setContentPane(panel);frame.setSize(1100,620);frame.setVisible(true);layout(panel);
            try {
                BufferedImage img=new BufferedImage(1100,620,BufferedImage.TYPE_INT_RGB); var g=img.createGraphics();panel.printAll(g);g.dispose();
                ImageIO.write(img,"png",Path.of("target","uc012-lich-ca-nhan.png").toFile());
            } catch(Exception ex){throw new RuntimeException(ex);} finally {frame.dispose();}
        });
    }
    private JTable table() { return (JTable)find(panel,"bangLichLamViec"); }
    @Test void calendar_chonNgay_moTuanChuaNgayDo_vaThang_giaoNam() throws Exception {
        var me=user("001",VaiTro.NHASI); SessionManager.getInstance().login(me);
        var a=shift("00001",me.getNhanVien()); a.setNgayTruc(LocalDate.of(2026,12,31));
        var b=shift("00002",me.getNhanVien()); b.setNgayTruc(LocalDate.of(2027,1,3));
        var c=shift("00003",me.getNhanVien()); c.setNgayTruc(LocalDate.of(2027,1,4));
        SwingUtilities.invokeAndWait(()->{
            panel=new PnlLichLamViec(()->List.of(me),()->List.of(a,b,c),x->false,true);
            panel.lamMoiDuLieu();
        });
        await(()->((JButton)find(panel,"lamMoiLich")).isEnabled());
        SwingUtilities.invokeAndWait(()->{
            var chooser=(com.toedter.calendar.JDateChooser)find(panel,"mocLich");
            chooser.setDate(java.util.Date.from(LocalDate.of(2027,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            assertEquals("28/12/2026 – 03/01/2027",((JLabel)find(panel,"khoangNgayLich")).getText());
            assertEquals(2,table().getRowCount());
            ((AbstractButton)find(panel,"xemThang")).doClick();
            assertEquals("01/01/2027 – 31/01/2027",((JLabel)find(panel,"khoangNgayLich")).getText());
            assertEquals(2,table().getRowCount());
            ((AbstractButton)find(panel,"xemTuan")).doClick();
            chooser.setDate(java.util.Date.from(LocalDate.of(2025,11,4).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            assertEquals("03/11/2025 – 09/11/2025",((JLabel)find(panel,"khoangNgayLich")).getText());
            assertEquals(0,table().getRowCount());
            ((JButton)find(panel,"lichHomNay")).doClick();
            assertEquals(LocalDate.now(),chooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        });
    }
    private static Component find(Container root,String name) {
        for(Component c:root.getComponents()) {if(name.equals(c.getName()))return c;if(c instanceof Container next){var found=find(next,name);if(found!=null)return found;}}return null;
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container next)layout(next);}
    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        for(int i=0;i<200;i++){AtomicBoolean ready=new AtomicBoolean();SwingUtilities.invokeAndWait(()->ready.set(condition.getAsBoolean()));if(ready.get())return;Thread.sleep(30);}fail("Chưa tải được lịch cá nhân");
    }
}
