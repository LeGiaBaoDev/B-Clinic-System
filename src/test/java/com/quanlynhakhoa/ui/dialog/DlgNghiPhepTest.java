package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.dao.DonNghiPhepDAO.Don;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class DlgNghiPhepTest {
    private DlgNghiPhep.NoiDung form;
    private final TaiKhoan manager=user("001",VaiTro.QUANLY),dentist=user("002",VaiTro.NHASI);
    private final LocalDate day=LocalDate.now().plusDays(10);
    private Don current;private boolean affected,failCheck;private final AtomicInteger approved=new AtomicInteger(),sent=new AtomicInteger();private String refused;
    private TaiKhoan user(String id,VaiTro role){return new TaiKhoan("TK"+id,"test"+id,"[Ẩn]",role,true,new NhanVien("NV"+id,"Nguyễn Văn Anh",true,"0912345678","test@example.com","Nha sĩ"));}
    private LichLamViec ca(){return new LichLamViec("LLV00001",day,CaTruc.CA_SANG,LocalTime.of(8,0),LocalTime.NOON,TrangThaiLichLamViec.DA_PHAN_CONG,null,dentist.getNhanVien());}
    private final DonNghiPhepDAO dao=new DonNghiPhepDAO(){
        public List<Don> layDonQuanLy(){assertFalse(SwingUtilities.isEventDispatchThread());return List.of(current);}
        public List<Don> layDonCuaToi(){assertFalse(SwingUtilities.isEventDispatchThread());return current==null?List.of():List.of(current);}
        public List<LichLamViecDAO.LichHenAnhHuong> kiemTra(String id){assertFalse(SwingUtilities.isEventDispatchThread());assertEquals("DNP000001",id);if(failCheck)throw new BusinessException("Ca đã thay đổi");return affected?List.of(new LichLamViecDAO.LichHenAnhHuong("LH261114001","Khách Kiểm Thử","Khám",day.atTime(9,0),day.atTime(9,30),"DA_DAT")):List.of();}
        public boolean duyet(String id){assertFalse(SwingUtilities.isEventDispatchThread());approved.incrementAndGet();current=new Don(id,ca(),"Việc gia đình",LocalDateTime.now(),"DA_DUYET","Quản lý",LocalDateTime.now(),null);return true;}
        public boolean tuChoi(String id,String reason){assertFalse(SwingUtilities.isEventDispatchThread());refused=reason;current=new Don(id,ca(),"Việc gia đình",LocalDateTime.now(),"TU_CHOI","Quản lý",LocalDateTime.now(),reason);return true;}
        public String guiDon(String id,String reason){assertFalse(SwingUtilities.isEventDispatchThread());assertEquals("LLV00001",id);assertEquals("Việc gia đình",reason);sent.incrementAndGet();current=new Don("DNP000001",ca(),reason,LocalDateTime.now(),"CHO_DUYET",null,null,null);return current.maDon();}
    };
    @BeforeEach void setup(){SessionManager.getInstance().login(manager);current=new Don("DNP000001",ca(),"Việc gia đình",LocalDateTime.now(),"CHO_DUYET",null,null,null);}
    @AfterEach void logout(){SessionManager.getInstance().logout();}
    private void open(boolean management,boolean confirm)throws Exception{SwingUtilities.invokeAndWait(()->form=new DlgNghiPhep.NoiDung(management,dao,()->List.of(ca()),text->confirm,()->{}));await(()->find(form,"danhSachDon").isEnabled());}
    private JButton btn(String name){return (JButton)find(form,name);}
    private void select()throws Exception{SwingUtilities.invokeAndWait(()->((JTable)find(form,"danhSachDon")).setRowSelectionInterval(0,0));}
    @Test void manager_checksPersistedRequest_beforeApproval()throws Exception{
        open(true,true);select();SwingUtilities.invokeAndWait(()->{assertNull(find(form,"kiemTraNghi"));assertFalse(((JTextArea)find(form,"chiTietDon")).isEditable());});await(()->btn("duyetNghi").isEnabled());SwingUtilities.invokeAndWait(()->{btn("duyetNghi").doClick();btn("duyetNghi").doClick();});await(()->approved.get()==1);assertEquals("DA_DUYET",current.trangThai());
    }
    @Test void affectedAppointments_blockApproval_butAllowRejectionWithReason()throws Exception{
        affected=true;open(true,true);select();await(()->((JTable)find(form,"lichHenAnhHuong")).getRowCount()==1);SwingUtilities.invokeAndWait(()->{assertFalse(btn("duyetNghi").isEnabled());assertFalse(btn("tuChoiNghi").isEnabled());((JTextArea)find(form,"lyDoTuChoi")).setText("Thiếu người trực");btn("tuChoiNghi").doClick();});await(()->refused!=null);assertEquals("Thiếu người trực",refused);assertEquals(0,approved.get());
    }
    @Test void staleShift_canBeRejectedWithoutSuccessfulPreview()throws Exception{
        failCheck=true;open(true,true);select();await(()->((JLabel)find(form,"thongBaoDon")).getText().contains("Ca đã thay đổi"));SwingUtilities.invokeAndWait(()->{assertFalse(btn("duyetNghi").isEnabled());((JTextArea)find(form,"lyDoTuChoi")).setText("Vui lòng gửi lại theo ca mới");btn("tuChoiNghi").doClick();});await(()->refused!=null);
    }
    @Test void employee_submitsOwnShift_andPendingRemovesItFromChoices()throws Exception{
        SessionManager.getInstance().login(dentist);current=null;open(false,true);SwingUtilities.invokeAndWait(()->{assertNull(find(form,"duyetNghi"));assertFalse(btn("guiDonNghi").isEnabled());((JTextArea)find(form,"lyDoXinNghi")).setText("  Việc gia đình  ");btn("guiDonNghi").doClick();btn("guiDonNghi").doClick();});await(()->sent.get()==1&&((JComboBox<?>)find(form,"caXinNghi")).getItemCount()==0);assertEquals("CHO_DUYET",current.trangThai());assertEquals(TrangThaiLichLamViec.DA_PHAN_CONG,current.ca().getTrangThai());
    }
    @Test void guiDon_khiDangLocNgayKhac_vanHienDonChoDuyet()throws Exception {
        SessionManager.getInstance().login(dentist);current=null;open(false,true);
        SwingUtilities.invokeAndWait(()->{
            ((com.toedter.calendar.JDateChooser)find(form,"locNgayNghi")).setDate(java.util.Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            ((JTextField)find(form,"timDon")).setText("không khớp");
            ((JComboBox<?>)find(form,"locTrangThaiDon")).setSelectedIndex(2);
            ((JTextArea)find(form,"lyDoXinNghi")).setText("Việc gia đình");btn("guiDonNghi").doClick();
        });
        await(()->sent.get()==1&&find(form,"danhSachDon").isEnabled());
        SwingUtilities.invokeAndWait(()->{
            JTable table=(JTable)find(form,"danhSachDon");assertEquals(1,table.getRowCount());
            assertEquals("Chờ duyệt",table.getValueAt(0,5));assertEquals(0,table.getSelectedRow());
            assertTrue(((JTextArea)find(form,"chiTietDon")).getText().contains("Chờ duyệt"));
        });
    }
    @Test void lamMoiDonCaNhan_boLocCu_deHienDonDaGui()throws Exception {
        SessionManager.getInstance().login(dentist);open(false,true);
        SwingUtilities.invokeAndWait(()->{
            ((com.toedter.calendar.JDateChooser)find(form,"locNgayNghi")).setDate(java.util.Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            assertEquals(0,((JTable)find(form,"danhSachDon")).getRowCount());btn("lamMoiDon").doClick();
        });
        await(()->find(form,"danhSachDon").isEnabled());
        SwingUtilities.invokeAndWait(()->assertEquals("Chờ duyệt",((JTable)find(form,"danhSachDon")).getValueAt(0,5)));
    }
    @Test void employee_seesRejectionReasonAndCanResubmit()throws Exception{
        SessionManager.getInstance().login(dentist);current=new Don("DNP000001",ca(),"Việc gia đình",LocalDateTime.now(),"TU_CHOI","Quản lý",LocalDateTime.now(),"Thiếu người trực");open(false,true);select();SwingUtilities.invokeAndWait(()->{assertTrue(((JTextArea)find(form,"chiTietDon")).getText().contains("Thiếu người trực"));assertEquals(1,((JComboBox<?>)find(form,"caXinNghi")).getItemCount());});
    }
    @Test void cancelConfirmation_doesNotApprove()throws Exception{
        open(true,false);select();await(()->btn("duyetNghi").isEnabled());SwingUtilities.invokeAndWait(()->btn("duyetNghi").doClick());assertEquals(0,approved.get());
    }
    @Test void changingSelectedRequestOrFilter_invalidatesPreview()throws Exception{
        open(true,true);select();await(()->btn("duyetNghi").isEnabled());SwingUtilities.invokeAndWait(()->{((JTextField)find(form,"timDon")).setText("Không tồn tại");assertFalse(btn("duyetNghi").isEnabled());assertEquals(0,((JTable)find(form,"danhSachDon")).getRowCount());});
    }
    @Test void rejectionRequiresNonblankReasonAtMost255()throws Exception{
        open(true,true);select();await(()->btn("duyetNghi").isEnabled());SwingUtilities.invokeAndWait(()->{var reason=(JTextArea)find(form,"lyDoTuChoi");reason.setText("   ");assertFalse(btn("tuChoiNghi").isEnabled());reason.setText("x".repeat(256));assertFalse(btn("tuChoiNghi").isEnabled());reason.setText("x".repeat(255));assertTrue(btn("tuChoiNghi").isEnabled());});
    }
    @Test void employee_doesNotDisplayForeignRowsFromAnOldSession()throws Exception {
        SessionManager.getInstance().login(dentist);
        var foreign=new LichLamViec(ca());foreign.setNhanVien(manager.getNhanVien());
        current=new Don("DNP000001",foreign,"Riêng tư",LocalDateTime.now(),"CHO_DUYET",null,null,null);
        open(false,true);SwingUtilities.invokeAndWait(()->assertEquals(0,((JTable)find(form,"danhSachDon")).getRowCount()));
    }
    @Test void employee_formAboveList_andStatusHasColor()throws Exception {
        SessionManager.getInstance().login(dentist);open(false,true);
        SwingUtilities.invokeAndWait(()->{
            form.setSize(1050,700);layout(form);
            Component source=find(form,"khuTaoDon"),list=find(form,"khuDanhSachDon");
            assertTrue(SwingUtilities.convertPoint(source,0,source.getHeight(),form).y<=SwingUtilities.convertPoint(list,0,0,form).y);
            JTable table=(JTable)find(form,"danhSachDon");Component cell=table.prepareRenderer(table.getCellRenderer(0,5),0,5);
            assertEquals(new Color(0xFFF4DA),cell.getBackground());
        });
    }
    @Test void render_inboxAndEmployeeAt1366x768()throws Exception{
        com.formdev.flatlaf.FlatLightLaf.setup();open(true,true);select();await(()->btn("duyetNghi").isEnabled());render("uc015-hop-thu.png");SessionManager.getInstance().login(dentist);open(false,true);select();render("uc015-don-cua-toi.png");
    }
    private void render(String name)throws Exception{SwingUtilities.invokeAndWait(()->{JFrame frame=new JFrame();frame.setUndecorated(true);frame.setContentPane(form);frame.setSize(1050,700);frame.setVisible(true);try{layout(form);var img=new java.awt.image.BufferedImage(form.getWidth(),form.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=img.createGraphics();form.printAll(g);g.dispose();javax.imageio.ImageIO.write(img,"png",java.nio.file.Path.of("target",name).toFile());}catch(Exception ex){throw new RuntimeException(ex);}finally{frame.dispose();}});}
    private static Component find(Container root,String name){for(Component c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container next){var found=find(next,name);if(found!=null)return found;}}return null;}
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container next)layout(next);}
    private static void await(java.util.function.BooleanSupplier ok)throws Exception{for(int i=0;i<200;i++){AtomicBoolean done=new AtomicBoolean();SwingUtilities.invokeAndWait(()->done.set(ok.getAsBoolean()));if(done.get())return;Thread.sleep(30);}fail("Chưa hoàn tất xử lý đơn nghỉ phép");}
}
