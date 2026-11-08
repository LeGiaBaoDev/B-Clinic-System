package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.config.AppConfig;
import com.quanlynhakhoa.dao.BackupDAO;
import com.quanlynhakhoa.entity.enums.LoaiSaoLuu;
import com.quanlynhakhoa.ui.dialog.DlgXacNhanKhoiPhuc;
import com.quanlynhakhoa.util.*;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.concurrent.Callable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** UC039–042: chỉ được tích hợp vào menu quản lý. Mọi I/O chạy ngoài EDT. */
public final class PnlSaoLuu extends JPanel {
    private final BackupDAO dao=new BackupDAO();
    private final JTextField dir=new JTextField(48),file=new JTextField(48),gio=new JTextField("22:00",6);
    private final JCheckBox auto=new JCheckBox("Bật sao lưu tự động khi ứng dụng mở");
    private final JComboBox<String> period=new JComboBox<>(new String[]{"Hằng ngày","Hằng tuần (thứ Hai)"});
    private final JSpinner keep=new JSpinner(new SpinnerNumberModel(14,1,365,1));
    private final JButton backup=new JButton("Sao lưu ngay"),inspect=new JButton("Kiểm tra bản sao lưu"),restore=new JButton("Khôi phục"),save=new JButton("Lưu lịch tự động"),refresh=new JButton("Làm mới lịch sử");
    private final JProgressBar progress=new JProgressBar();
    private final JLabel status=new JLabel("Sẵn sàng"),info=new JLabel("Chọn file để kiểm tra trước khi khôi phục."),space=new JLabel(" ");
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"Thời gian","Loại","File","Byte","Kết quả","Người thực hiện"},0){public boolean isCellEditable(int r,int c){return false;}};
    private boolean busy;
    private String checkedFile;
    private BackupDAO.ThongTinBanSaoLuu checkedInfo;
    private final Runnable sauKhoiPhuc;
    public PnlSaoLuu(Runnable sauKhoiPhuc){
        this.sauKhoiPhuc=sauKhoiPhuc;setName("Sao lưu & Khôi phục");setLayout(new BorderLayout(12,16));setBackground(BACKGROUND);setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        var title=new JLabel("Sao lưu & Khôi phục");title.setFont(bold(26));title.setForeground(TEXT);add(title,BorderLayout.NORTH);
        var tabs=new JTabbedPane();tabs.setFont(font(13));var backupTab=box();dir.setEditable(false);backupTab.add(new JLabel("Tạo bản sao toàn bộ dữ liệu, kiểm tra tính hợp lệ và lưu vào lịch sử."));backupTab.add(new JLabel("Thư mục lưu phía SQL Server"));backupTab.add(dir);backupTab.add(space);backupTab.add(backup);tabs.addTab("Sao lưu",backupTab);
        var restoreTab=box();restoreTab.add(new JLabel("File .bak trong thư mục sao lưu đã cấu hình"));restoreTab.add(file);
        var choose=new JButton("Chọn file...");choose.addActionListener(e->{var picker=new JFileChooser();picker.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL Server backup (*.bak)","bak"));if(picker.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)file.setText(picker.getSelectedFile().getAbsolutePath());});restoreTab.add(choose);restoreTab.add(inspect);restoreTab.add(info);restoreTab.add(restore);tabs.addTab("Khôi phục",restoreTab);restore.setEnabled(false);
        file.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){clear();}public void removeUpdate(javax.swing.event.DocumentEvent e){clear();}public void changedUpdate(javax.swing.event.DocumentEvent e){clear();}private void clear(){checkedFile=null;restore.setEnabled(false);}});
        var schedule=box();var settingDir=new JTextField(48);schedule.add(new JLabel("Thư mục sao lưu trên máy chủ SQL Server"));schedule.add(settingDir);schedule.add(auto);var row=new JPanel(new FlowLayout(FlowLayout.LEFT));row.add(new JLabel("Giờ Việt Nam (HH:mm)"));row.add(gio);row.add(period);row.add(new JLabel("Giữ bản AUTO"));row.add(keep);schedule.add(row);schedule.add(save);schedule.add(new JLabel("Khi ứng dụng đóng, dùng Task Scheduler theo hướng dẫn kèm dự án."));tabs.addTab("Lịch tự động",schedule);
        var table=new JTable(model);table.setRowHeight(30);table.setAutoCreateRowSorter(true);var history=new JPanel(new BorderLayout(8,8));var scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());history.add(scroll);var bottom=new JPanel(new FlowLayout(FlowLayout.LEFT));var select=new JButton("Chọn bản này để khôi phục");bottom.add(select);bottom.add(refresh);history.add(bottom,BorderLayout.SOUTH);tabs.addTab("Lịch sử",history);
        select.addActionListener(e->{int r=table.getSelectedRow();if(r<0)return;r=table.convertRowIndexToModel(r);file.setText(String.valueOf(model.getValueAt(r,2)));tabs.setSelectedIndex(1);});
        add(tabs);var footer=new JPanel(new BorderLayout(12,0));footer.setOpaque(false);footer.add(status);footer.add(progress,BorderLayout.EAST);progress.setPreferredSize(new Dimension(220,20));add(footer,BorderLayout.SOUTH);
        for(var panel:new JPanel[]{backupTab,restoreTab,schedule}){
            Component[] controls=panel.getComponents();panel.removeAll();
            for(Component component:controls)if(component instanceof JComponent control){
                control.setAlignmentX(LEFT_ALIGNMENT);control.setFont(font(13));int height=control instanceof JLabel?26:40;
                control.setPreferredSize(new Dimension(control.getPreferredSize().width,height));control.setMaximumSize(new Dimension(control instanceof JButton?240:Integer.MAX_VALUE,height));
                panel.add(control);panel.add(Box.createVerticalStrut(10));
            }
            panel.add(Box.createVerticalGlue());
        }
        backup.setBackground(TEAL);backup.setForeground(Color.WHITE);restore.setBackground(new Color(0xB42318));restore.setForeground(Color.WHITE);
        backup.addActionListener(e->run(()->dao.backup(LoaiSaoLuu.FULL),v->{JOptionPane.showMessageDialog(this,"Sao lưu thành công:\n"+v);loadHistory();}));
        inspect.addActionListener(e->{String path=file.getText().trim();run(()->dao.docThongTin(path),v->{checkedInfo=v;checkedFile=path;info.setText("DB: "+v.database()+" | Ngày tạo: "+v.ngay()+" | "+v.kichThuoc()+" byte | Full backup hợp lệ");restore.setEnabled(true);});});
        restore.addActionListener(e->{if(checkedFile==null||!checkedFile.equals(file.getText().trim()))return;
            char[] pass=new DlgXacNhanKhoiPhuc(SwingUtilities.getWindowAncestor(this),dao).hoi();if(pass==null)return;
            if(JOptionPane.showConfirmDialog(this,"Khôi phục về "+checkedInfo.ngay()+"?\nDữ liệu mới hơn sẽ được thay thế. Hệ thống tạo bản an toàn trước khi thực hiện.","Xác nhận lần cuối",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION){Arrays.fill(pass,'\0');return;}
            String path=checkedFile;run(()->dao.restore(path,pass),v->{JOptionPane.showMessageDialog(this,"Khôi phục hoàn tất.\nBản an toàn: "+v.banAnToan()+"\n"+v.canhBao()+"\nVui lòng đăng nhập lại.");sauKhoiPhuc.run();});});
        save.addActionListener(e->{try{var user=SessionManager.getInstance().getCurrentUser();var cfg=new AppConfig.LichTuDong(auto.isSelected(),LocalTime.parse(gio.getText().trim()),period.getSelectedIndex()==1,(int)keep.getValue(),user.getMaTK());
            run(()->{dao.luuCauHinh(settingDir.getText().trim(),cfg);BackupScheduler.getInstance().khoiDongLai();return dao.thuMuc();},v->{dir.setText(v);JOptionPane.showMessageDialog(this,"Đã lưu lịch sao lưu.");});}catch(Exception ex){error(ex);}});
        refresh.addActionListener(e->loadHistory());
        tabs.addChangeListener(e->{if(tabs.getSelectedIndex()==3)loadHistory();});
        run(()->{var cfg=new AppConfig();String folder=dao.thuMuc(),free;
            try{Path path=Path.of(folder);free=Files.isDirectory(path)?"Dung lượng trống (máy này): "+Files.getFileStore(path).getUsableSpace()/1024/1024+" MB":"Thư mục phía máy chủ; không đo dung lượng từ máy này.";}catch(Exception ignored){free="Không đo được dung lượng thư mục.";}
            return new Object[]{folder,cfg.lich(),free};},v->{dir.setText((String)v[0]);settingDir.setText((String)v[0]);var cfg=(AppConfig.LichTuDong)v[1];auto.setSelected(cfg.bat());gio.setText(cfg.gio().toString());period.setSelectedIndex(cfg.hangTuan()?1:0);keep.setValue(cfg.soBanGiu());space.setText((String)v[2]);loadHistory();});
    }
    private JPanel box(){var p=new JPanel();p.setBackground(Color.WHITE);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));return p;}
    private void loadHistory(){run(dao::lichSu,rows->{model.setRowCount(0);for(var r:rows)model.addRow(new Object[]{r.getThoiGian().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),switch(r.getLoai()){case "FULL"->"Thủ công";case "AUTO"->"Tự động";case "PRE_RESTORE"->"Trước khôi phục";case "RESTORE"->"Khôi phục";default->r.getLoai();},r.getDuongDan(),r.getKichThuoc(),switch(r.getKetQua()){case "THANH_CONG"->"Thành công";case "THAT_BAI"->"Thất bại";default->"Cần kiểm tra";},r.getNguoiThucHien()});status.setText(rows.isEmpty()?"Chưa có lịch sử. Bấm Sao lưu ngay để tạo bản đầu tiên.":"Đã tải "+rows.size()+" bản ghi lịch sử.");});}
    private void error(Exception e){Throwable cause=e instanceof java.util.concurrent.ExecutionException?e.getCause():e;JOptionPane.showMessageDialog(this,cause.getMessage(),"Không hoàn tất thao tác",JOptionPane.ERROR_MESSAGE);}
    private <T> void run(Callable<T> action,java.util.function.Consumer<T> done){if(busy)return;busy=true;for(var b:new JButton[]{backup,inspect,restore,save,refresh})b.setEnabled(false);progress.setIndeterminate(true);status.setText("Đang xử lý, vui lòng chờ...");
        new SwingWorker<T,Void>(){protected T doInBackground()throws Exception{return action.call();}protected void done(){busy=false;progress.setIndeterminate(false);status.setText("Sẵn sàng");for(var b:new JButton[]{backup,inspect,save,refresh})b.setEnabled(true);restore.setEnabled(checkedFile!=null);try{done.accept(get());}catch(Exception ex){error(ex);}}}.execute();}
}
