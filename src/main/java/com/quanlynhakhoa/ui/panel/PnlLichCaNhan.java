package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.LichLamViecDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.util.SessionManager;
import java.util.List;

public class PnlLichCaNhan extends PnlLichLamViec {
    private final javax.swing.JLabel lblCapNhat=new javax.swing.JLabel();
    private final String maTK;
    private boolean dangTaiThongBao;
    public PnlLichCaNhan() {
        super(PnlLichCaNhan::taiKhoanHienTai,new LichLamViecDAO()::layLichCaNhan,
                ca -> { throw new IllegalStateException("Lịch cá nhân chỉ được xem."); },true);
        TaiKhoan current=SessionManager.getInstance().getCurrentUser();maTK=current==null?null:current.getMaTK();
        java.awt.Component title=((java.awt.BorderLayout)getLayout()).getLayoutComponent(java.awt.BorderLayout.NORTH);
        javax.swing.JPanel header=new javax.swing.JPanel(new java.awt.BorderLayout(0,10));header.setOpaque(false);
        header.add(title,java.awt.BorderLayout.NORTH);
        javax.swing.JButton btnDon=new javax.swing.JButton("Xin nghỉ phép / Đơn của tôi");btnDon.setName("moDonNghiCaNhan");
        btnDon.addActionListener(e->new com.quanlynhakhoa.ui.dialog.DlgNghiPhep(javax.swing.SwingUtilities.getWindowAncestor(this),false,this::lamMoiDuLieu).setVisible(true));javax.swing.JPanel actions=new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,0,0));actions.setOpaque(false);
        btnDon.setBackground(com.quanlynhakhoa.ui.component.UiConstants.TEAL);btnDon.setForeground(java.awt.Color.WHITE);
        actions.add(btnDon);header.add(actions,java.awt.BorderLayout.CENTER);lblCapNhat.setName("thongBaoDoiCa");lblCapNhat.setForeground(com.quanlynhakhoa.ui.component.UiConstants.TEAL);
        lblCapNhat.setFont(com.quanlynhakhoa.ui.component.UiConstants.font(12));lblCapNhat.setVisible(false);header.add(lblCapNhat,java.awt.BorderLayout.SOUTH);add(header,java.awt.BorderLayout.NORTH);
    }
    @Override public void lamMoiDuLieu() {
        super.lamMoiDuLieu();
        TaiKhoan current=SessionManager.getInstance().getCurrentUser();
        if(current==null||maTK==null||!maTK.equals(current.getMaTK())) {lblCapNhat.setVisible(false);return;}
        if(dangTaiThongBao)return;dangTaiThongBao=true;
        new javax.swing.SwingWorker<LichLamViecDAO.ThongBaoDoiCa,Void>() {
            protected LichLamViecDAO.ThongBaoDoiCa doInBackground(){return new LichLamViecDAO().layThongBaoDoiCaGanNhat();}
            protected void done(){
                dangTaiThongBao=false;
                try {
                    var note=get();var active=SessionManager.getInstance().getCurrentUser();
                    boolean show=note!=null&&active!=null&&maTK.equals(active.getMaTK())&&maTK.equals(note.maTK());
                    lblCapNhat.setVisible(show);
                    if(show){String text=note.noiDung().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");lblCapNhat.setText("<html>"+text+" · "+note.thoiDiem().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))+"</html>");}
                    revalidate();repaint();
                } catch(InterruptedException ex){Thread.currentThread().interrupt();lblCapNhat.setVisible(false);}
                catch(java.util.concurrent.ExecutionException ex){lblCapNhat.setVisible(false);}
            }
        }.execute();
    }
    private static List<TaiKhoan> taiKhoanHienTai() {
        TaiKhoan tk=SessionManager.getInstance().getCurrentUser();
        return tk==null?List.of():List.of(tk);
    }
}
