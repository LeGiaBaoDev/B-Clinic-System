package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class DlgDoiCaTest {
    private DlgDoiCa.NoiDung form;
    private final TaiKhoan manager=user("001",VaiTro.QUANLY),a=user("002",VaiTro.NHASI),b=user("004",VaiTro.NHASI),other=user("003",VaiTro.LETAN);
    private final LocalDate day=LocalDate.now().plusDays(10);
    private TaiKhoan user(String id,VaiTro role){return new TaiKhoan("TK"+id,"test"+id,"[Ẩn]",role,true,new NhanVien("NV"+id,"Nguyễn Văn Anh",true,"0912345678","test@example.com","Nha sĩ"));}
    private LichLamViec ca(String id,TaiKhoan tk,int offset){return new LichLamViec("LLV"+id,day.plusDays(offset),CaTruc.CA_SANG,LocalTime.of(8,0),LocalTime.NOON,TrangThaiLichLamViec.DA_PHAN_CONG,null,tk.getNhanVien());}
    private List<LichLamViec> lich(){return List.of(ca("00001",a,0),ca("00002",b,1),ca("00003",other,2));}
    @BeforeEach void login(){SessionManager.getInstance().login(manager);}
    @AfterEach void logout(){SessionManager.getInstance().logout();}
    private void ngayMoi(){((JComboBox<?>)find(form,"caMoi")).setSelectedIndex(1);((com.toedter.calendar.JDateChooser)find(form,"ngayMoi")).setDate(Date.from(day.plusDays(4).atStartOfDay(ZoneId.systemDefault()).toInstant()));}
    @Test void doiCa_kiemTraNen_xacNhanTruocLuu_vaChiGuiMotLan() throws Exception {
        AtomicInteger checked=new AtomicInteger(),saved=new AtomicInteger();AtomicBoolean done=new AtomicBoolean();
        SwingUtilities.invokeAndWait(()->{
            form=new DlgDoiCa.NoiDung(lich(),List.of(a,b,other),r->{assertFalse(SwingUtilities.isEventDispatchThread());checked.incrementAndGet();},r->{assertEquals(1,checked.get());assertEquals(day.plusDays(4),r.ngayMoi());assertEquals(CaTruc.CA_CHIEU,r.caMoi());saved.incrementAndGet();return true;},text->{assertTrue(SwingUtilities.isEventDispatchThread());assertTrue(text.contains("sang"));return true;},()->done.set(true),()->{});
            ngayMoi();button().doClick();button().doClick();assertFalse(button().isEnabled());
        });
        await(done::get);assertEquals(1,saved.get());
    }
    @Test void huyXacNhan_khongGhiDuLieu() throws Exception {
        AtomicBoolean checked=new AtomicBoolean();
        SwingUtilities.invokeAndWait(()->{form=new DlgDoiCa.NoiDung(lich(),List.of(a,b,other),r->checked.set(true),r->{fail("Không được lưu khi hủy");return true;},text->false,()->fail(),()->{});ngayMoi();button().doClick();});
        await(()->checked.get()&&button().isEnabled());assertTrue(((JLabel)find(form,"loiDoiCa")).getText().contains("hủy"));
    }
    @Test void hoanDoi_chiChonNhanVienCungChucVu() throws Exception {
        AtomicBoolean done=new AtomicBoolean();
        SwingUtilities.invokeAndWait(()->{
            form=new DlgDoiCa.NoiDung(lich(),List.of(a,b,other),r->{},r->{assertNotNull(r.caDich());assertEquals("NV004",r.caDich().getNhanVien().getMaNV());return true;},text->true,()->done.set(true),()->{});
            ((JComboBox<?>)find(form,"cachDoiCa")).setSelectedIndex(1);assertEquals(1,((JComboBox<?>)find(form,"caHoanDoi")).getItemCount());button().doClick();
        });await(done::get);
    }
    @Test void loiKiemTraLichHen_khongMoXacNhan_vaKhongLuu() throws Exception {
        SwingUtilities.invokeAndWait(()->{form=new DlgDoiCa.NoiDung(lich(),List.of(a,b,other),r->{throw new BusinessException("Còn lịch hẹn đã đặt.");},r->{fail();return true;},text->{fail();return true;},()->fail(),()->{});ngayMoi();button().doClick();});
        await(()->button().isEnabled());assertTrue(((JLabel)find(form,"loiDoiCa")).getText().contains("lịch hẹn"));
    }
    @Test void caQuaKhuVaNghiPhep_khongNamTrongDanhSachDoi() throws Exception {
        var past=ca("00001",a,0);past.setNgayTruc(LocalDate.now().minusDays(1));var leave=ca("00002",b,0);leave.nghiPhep("Nghỉ");
        SwingUtilities.invokeAndWait(()->{form=new DlgDoiCa.NoiDung(List.of(past,leave),List.of(a,b),r->fail(),r->{fail();return false;},t->true,()->{},()->{});assertEquals(0,((JComboBox<?>)find(form,"caNguon")).getItemCount());assertFalse(button().isEnabled());});
    }
    @Test void render_formDoiCa() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            com.formdev.flatlaf.FlatLightLaf.setup();form=new DlgDoiCa.NoiDung(lich(),List.of(a,b,other),r->{},r->true,t->true,()->{},()->{});
            JFrame frame=new JFrame();frame.setUndecorated(true);frame.setContentPane(form);frame.setSize(760,700);frame.setVisible(true);
            try{
                layout(form);var img=new java.awt.image.BufferedImage(760,700,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=img.createGraphics();form.printAll(g);g.dispose();javax.imageio.ImageIO.write(img,"png",java.nio.file.Path.of("target","uc014-doi-ca.png").toFile());
                ((JComboBox<?>)find(form,"cachDoiCa")).setSelectedIndex(1);layout(form);
                var swap=new java.awt.image.BufferedImage(760,700,java.awt.image.BufferedImage.TYPE_INT_RGB);var sg=swap.createGraphics();form.printAll(sg);sg.dispose();javax.imageio.ImageIO.write(swap,"png",java.nio.file.Path.of("target","uc014-hoan-doi-loc.png").toFile());
            }catch(Exception ex){throw new RuntimeException(ex);}finally{frame.dispose();}
        });
    }
    @Test void locNguon_ketHopChucVuCaNgayTenKhongDauVaMa_vaXoaLoc() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            form=new DlgDoiCa.NoiDung(lich(),List.of(a,b,other),r->{},r->true,t->true,()->{},()->{});
            ((JComboBox<?>)find(form,"nguonChucVu")).setSelectedItem("Nha sĩ");
            ((JComboBox<?>)find(form,"nguonCa")).setSelectedItem("Ca sáng");
            ((com.toedter.calendar.JDateChooser)find(form,"nguonNgay")).setDate(Date.from(day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            ((JTextField)find(form,"nguonTim")).setText("nguyen nv004");
            ((JButton)find(form,"nguonLoc")).doClick();
            var source=(JComboBox<?>)find(form,"caNguon");
            assertEquals(1,source.getItemCount());assertEquals("NV004",((LichLamViec)source.getSelectedItem()).getNhanVien().getMaNV());
            ((JButton)find(form,"nguonXoaLoc")).doClick();assertEquals(3,source.getItemCount());
            assertEquals("NV004",((LichLamViec)source.getSelectedItem()).getNhanVien().getMaNV()); // giữ ca đang chọn nếu vẫn còn
            ((JTextField)find(form,"nguonTim")).setText("khong ton tai");((JButton)find(form,"nguonLoc")).doClick();
            assertEquals(0,source.getItemCount());assertFalse(button().isEnabled());
            assertTrue(((JLabel)find(form,"nguonKetQua")).getText().contains("Không có ca phù hợp"));
            ((JButton)find(form,"nguonXoaLoc")).doClick();assertEquals(3,source.getItemCount());
        });
    }
    @Test void locDich_docLap_khongVuotQuaDieuKienCungChucVuVaKhacNhanVien() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            form=new DlgDoiCa.NoiDung(lich(),List.of(a,b,other),r->{},r->true,t->true,()->{},()->{});
            ((JComboBox<?>)find(form,"cachDoiCa")).setSelectedIndex(1);
            assertFalse(find(form,"dichChucVu").isEnabled());
            ((JTextField)find(form,"dichTim")).setText("NV003");((JButton)find(form,"dichLoc")).doClick();
            var target=(JComboBox<?>)find(form,"caHoanDoi");assertEquals(0,target.getItemCount());assertFalse(button().isEnabled());
            assertEquals(3,((JComboBox<?>)find(form,"caNguon")).getItemCount());
            ((JButton)find(form,"dichXoaLoc")).doClick();assertEquals(1,target.getItemCount());assertTrue(button().isEnabled());
            ((JTextField)find(form,"nguonTim")).setText("NV004");((JButton)find(form,"nguonLoc")).doClick();
            assertEquals("NV002",((LichLamViec)target.getSelectedItem()).getNhanVien().getMaNV());
        });
    }
    private JButton button(){return (JButton)find(form,"luuDoiCa");}
    private static Component find(Container root,String name){for(Component c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container next){Component x=find(next,name);if(x!=null)return x;}}return null;}
    private static void layout(Container c){c.doLayout();for(Component x:c.getComponents())if(x instanceof Container next)layout(next);}
    private static void await(java.util.function.BooleanSupplier ok)throws Exception{for(int i=0;i<200;i++){AtomicBoolean b=new AtomicBoolean();SwingUtilities.invokeAndWait(()->b.set(ok.getAsBoolean()));if(b.get())return;Thread.sleep(30);}fail("Chưa xử lý xong đổi ca");}
}
