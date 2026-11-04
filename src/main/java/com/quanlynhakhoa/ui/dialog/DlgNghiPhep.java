package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.dao.DonNghiPhepDAO.Don;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

/** Hộp thư đơn nghỉ phép; Quản lý chỉ xử lý đơn đã được nhân viên gửi. */
public class DlgNghiPhep extends JDialog {
    public DlgNghiPhep(Window owner,boolean quanLy,Runnable refresh) {
        super(owner,quanLy?"Tiếp nhận đơn nghỉ phép":"Xin nghỉ phép và theo dõi đơn",ModalityType.APPLICATION_MODAL);
        NoiDung form=new NoiDung(quanLy,new DonNghiPhepDAO(),new LichLamViecDAO()::layLichCaNhan,
            text->JOptionPane.showConfirmDialog(this,text,"Xác nhận",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION,refresh);
        setContentPane(form);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){if(!form.busy)dispose();}});
        getRootPane().registerKeyboardAction(e->{if(!form.busy)dispose();},KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        Rectangle b=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1050,b.width-32),Math.min(800,b.height-40));setMinimumSize(new Dimension(680,580));setLocationRelativeTo(owner);
    }
    static final class NoiDung extends JPanel {
        private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy"),STAMP=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        private final boolean manager;private final DonNghiPhepDAO dao;private final Supplier<List<LichLamViec>> shifts;private final Predicate<String> confirm;private final Runnable refresh;
        private final String account;
        private List<Don> rows=List.of(),visible=List.of();private List<LichLamViec> own=List.of();
        private final DefaultTableModel model=new DefaultTableModel(new String[]{"Mã đơn","Nhân viên","Ngày nghỉ","Ca","Ngày gửi","Trạng thái"},0){public boolean isCellEditable(int r,int c){return false;}};
        private final JTable tbl=new JTable(model);
        private final JComboBox<String> cboStatus=new JComboBox<>(new String[]{"Chờ duyệt","Đã duyệt","Từ chối","Tất cả"});
        private final JTextField txtTim=new JTextField(18);
        private final com.toedter.calendar.JDateChooser dpNgay=new com.toedter.calendar.JDateChooser();
        private final JComboBox<LichLamViec> cboCa=new JComboBox<>();
        private final JTextArea txtReason=new JTextArea(3,30),txtReject=new JTextArea(2,30),txtDetail=new JTextArea(5,30);
        private final JButton btnGui=new JButton("Gửi đơn xin nghỉ"),btnApprove=new JButton("Duyệt đơn"),btnReject=new JButton("Từ chối"),btnReload=new JButton("Làm mới"),btnClear=new JButton("Xóa lọc");
        private final JLabel lbl=new JLabel(" ");
        private final DefaultTableModel affected=new DefaultTableModel(new String[]{"Mã lịch hẹn","Khách hàng","Dịch vụ","Bắt đầu","Kết thúc","Trạng thái"},0){public boolean isCellEditable(int r,int c){return false;}};
        private boolean busy;private String checked;private long selectionVersion;
        NoiDung(boolean manager,DonNghiPhepDAO dao,Supplier<List<LichLamViec>> shifts,Predicate<String> confirm,Runnable refresh) {
            super(new BorderLayout(0,12));this.manager=manager;this.dao=dao;this.shifts=shifts;this.confirm=confirm;this.refresh=refresh;
            var tk=SessionManager.getInstance().getCurrentUser();account=tk==null?null:tk.getMaTK();
            setBorder(new EmptyBorder(20,24,20,24));setBackground(BACKGROUND);
            JLabel title=heading(manager?"Đơn nghỉ phép của nhân viên":"Xin nghỉ phép",22);
            JPanel header=new JPanel(new BorderLayout(0,12));header.setOpaque(false);header.add(title,BorderLayout.NORTH);
            if(!manager)header.add(taoFormGui(),BorderLayout.CENTER);
            add(header,BorderLayout.NORTH);

            cboStatus.setName("locTrangThaiDon");txtTim.setName("timDon");
            txtTim.putClientProperty("JTextField.placeholderText",manager?"Tên / mã nhân viên / mã đơn":"Tìm mã đơn");
            txtTim.setColumns(manager?18:12);
            dpNgay.setName("locNgayNghi");btnReload.setName("lamMoiDon");dpNgay.setDateFormatString("dd/MM/yyyy");
            dpNgay.setPreferredSize(new Dimension(140,34));dpNgay.setToolTipText("Lọc theo ngày nghỉ. Để trống để xem mọi ngày.");
            dpNgay.setDate(null);((JTextField)dpNgay.getDateEditor().getUiComponent()).setEditable(false);
            if(!manager)cboStatus.setSelectedIndex(3);
            JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));filters.setOpaque(false);
            filters.add(new JLabel("Trạng thái"));filters.add(cboStatus);filters.add(new JLabel("Ngày nghỉ"));filters.add(dpNgay);
            filters.add(txtTim);filters.add(btnClear);filters.add(btnReload);
            JPanel listHeader=new JPanel(new BorderLayout(0,12));listHeader.setOpaque(false);
            listHeader.add(heading(manager?"Danh sách đơn":"Đơn nghỉ phép của tôi",16),BorderLayout.NORTH);listHeader.add(filters,BorderLayout.CENTER);
            JPanel listCard=card();listCard.setName("khuDanhSachDon");listCard.add(listHeader,BorderLayout.NORTH);
            tbl.setName("danhSachDon");tbl.setRowHeight(38);tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tbl.setAutoCreateRowSorter(true);tbl.setFillsViewportHeight(true);tbl.setFont(font(13));tbl.getTableHeader().setFont(bold(12));
            int[] widths={110,230,110,130,180,140};for(int i=0;i<widths.length;i++)tbl.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            tbl.setSelectionBackground(TEAL_LIGHT);tbl.setSelectionForeground(TEXT);tbl.setShowVerticalLines(false);tbl.setGridColor(BORDER);
            tbl.setDefaultRenderer(Object.class,new javax.swing.table.DefaultTableCellRenderer(){
                public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col){
                    super.getTableCellRendererComponent(table,value,selected,focus,row,col);setBorder(new EmptyBorder(6,10,6,10));
                    setFont(col==5?bold(12):font(13));setForeground(TEXT);setBackground(selected?TEAL_LIGHT:row%2==0?SURFACE:new Color(0xF8FAFC));
                    if(col==5){String state=String.valueOf(value);setForeground(state.equals("Chờ duyệt")?new Color(0xA86A11):state.equals("Đã duyệt")?GREEN:new Color(0xBA4852));
                        setBackground(state.equals("Chờ duyệt")?new Color(0xFFF4DA):state.equals("Đã duyệt")?new Color(0xE7F5ED):new Color(0xFCECEE));}
                    return this;
                }
            });
            JScrollPane list=new JScrollPane(tbl);list.setBorder(BorderFactory.createLineBorder(BORDER));list.setPreferredSize(new Dimension(800,160));
            listCard.add(list,BorderLayout.CENTER);
            JPanel detail=new JPanel(new BorderLayout(0,8));detail.setOpaque(false);
            txtDetail.setName("chiTietDon");txtDetail.setEditable(false);txtDetail.setLineWrap(true);txtDetail.setWrapStyleWord(true);
            txtDetail.setFont(font(13));txtDetail.setForeground(TEXT);txtDetail.setBackground(TEAL_LIGHT);txtDetail.setBorder(new EmptyBorder(10,12,10,12));
            txtDetail.setRows(manager?4:3);JScrollPane info=new JScrollPane(txtDetail);info.setBorder(BorderFactory.createLineBorder(BORDER));detail.add(info,BorderLayout.NORTH);
            if(manager){
                JTable ap=new JTable(affected);ap.setName("lichHenAnhHuong");ap.setRowHeight(30);ap.setFont(font(12));ap.getTableHeader().setFont(bold(12));
                JScrollPane sp=new JScrollPane(ap);sp.setPreferredSize(new Dimension(800,105));
                JPanel impact=new JPanel(new BorderLayout(0,6));impact.setOpaque(false);impact.add(heading("Lịch hẹn bị ảnh hưởng · tự kiểm tra khi chọn đơn",13),BorderLayout.NORTH);impact.add(sp,BorderLayout.CENTER);detail.add(impact,BorderLayout.CENTER);
                JPanel decision=new JPanel(new BorderLayout(0,6));decision.setOpaque(false);decision.add(new JLabel("Lý do từ chối * (tối đa 255 ký tự)"),BorderLayout.NORTH);
                txtReject.setName("lyDoTuChoi");txtReject.setLineWrap(true);txtReject.setFont(font(13));decision.add(new JScrollPane(txtReject),BorderLayout.CENTER);
                JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));buttons.setOpaque(false);
                btnApprove.setName("duyetNghi");primary(btnApprove);btnReject.setName("tuChoiNghi");btnReject.setForeground(new Color(0xBA4852));
                buttons.add(btnReject);buttons.add(btnApprove);decision.add(buttons,BorderLayout.SOUTH);detail.add(decision,BorderLayout.SOUTH);
            }
            listCard.add(detail,BorderLayout.SOUTH);add(listCard,BorderLayout.CENTER);
            lbl.setName("thongBaoDon");lbl.setFont(font(12));lbl.setForeground(TEAL_DARK);lbl.setBorder(new EmptyBorder(6,10,6,10));
            lbl.setOpaque(true);lbl.setBackground(TEAL_LIGHT);add(lbl,BorderLayout.SOUTH);
            cboStatus.addActionListener(e->filter());txtTim.getDocument().addDocumentListener(listener(this::filter));dpNgay.addPropertyChangeListener("date",e->filter());btnClear.addActionListener(e->{txtTim.setText("");dpNgay.setDate(null);cboStatus.setSelectedIndex(manager?0:3);});
            tbl.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())selection();});txtReject.getDocument().addDocumentListener(listener(this::controls));txtReason.getDocument().addDocumentListener(listener(this::controls));cboCa.addActionListener(e->controls());
            btnReload.addActionListener(e->{if(!busy){if(!manager)xoaLocCaNhan();load();}});btnGui.addActionListener(e->send());btnApprove.addActionListener(e->decide(true));btnReject.addActionListener(e->decide(false));controls();load();
        }
        private static JLabel heading(String text,int size){JLabel label=new JLabel(text);label.setFont(bold(size));label.setForeground(TEXT);return label;}
        private static JPanel card(){JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(SURFACE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(14,16,14,16)));return p;}
        private static void primary(JButton button){button.setBackground(TEAL);button.setForeground(Color.WHITE);button.setFont(bold(13));button.setMargin(new Insets(8,16,8,16));}
        private JPanel taoFormGui(){
            JPanel form=card();form.setName("khuTaoDon");form.add(heading("Tạo đơn xin nghỉ phép",16),BorderLayout.NORTH);
            JPanel fields=new JPanel(new GridBagLayout());fields.setOpaque(false);
            GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=0;g.weightx=0.45;g.fill=GridBagConstraints.HORIZONTAL;g.anchor=GridBagConstraints.NORTHWEST;g.insets=new Insets(0,0,6,18);
            fields.add(heading("Ca làm muốn xin nghỉ *",12),g);g.gridx=1;g.weightx=0.55;g.insets=new Insets(0,0,6,0);fields.add(heading("Lý do xin nghỉ *",12),g);
            cboCa.setName("caXinNghi");cboCa.setFont(font(13));cboCa.setPreferredSize(new Dimension(350,38));
            cboCa.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean selected,boolean focus){super.getListCellRendererComponent(l,v,i,selected,focus);setFont(font(13));if(v==null)setText("Không có ca hợp lệ để xin nghỉ");if(v instanceof LichLamViec ca)setText(ca.getNgayTruc().format(DATE)+" · "+ca.getGioBatDau()+"–"+ca.getGioKetThuc());return this;}});
            g.gridx=0;g.gridy=1;g.weightx=0.45;g.insets=new Insets(0,0,0,18);fields.add(cboCa,g);
            txtReason.setName("lyDoXinNghi");txtReason.setFont(font(13));txtReason.setLineWrap(true);txtReason.setWrapStyleWord(true);txtReason.setBorder(new EmptyBorder(6,8,6,8));
            g.gridx=1;g.weightx=0.55;g.insets=new Insets(0,0,0,0);fields.add(new JScrollPane(txtReason),g);form.add(fields,BorderLayout.CENTER);
            JPanel bottom=new JPanel(new BorderLayout(12,0));bottom.setOpaque(false);
            JLabel hint=new JLabel("<html>Chỉ chọn ca của bạn chưa kết thúc và chưa có đơn chờ.<br>Lý do tối đa 255 ký tự. Ca chỉ đổi khi Quản lý duyệt.</html>");hint.setFont(font(12));hint.setForeground(MUTED);bottom.add(hint,BorderLayout.CENTER);
            btnGui.setName("guiDonNghi");primary(btnGui);bottom.add(btnGui,BorderLayout.EAST);form.add(bottom,BorderLayout.SOUTH);return form;
        }
        private static DocumentListener listener(Runnable r){return new DocumentListener(){public void insertUpdate(DocumentEvent e){r.run();}public void removeUpdate(DocumentEvent e){r.run();}public void changedUpdate(DocumentEvent e){r.run();}};}
        private boolean session(){var tk=SessionManager.getInstance().getCurrentUser();return tk!=null&&account!=null&&account.equals(tk.getMaTK())&&(!manager||tk.getVaiTro()==VaiTro.QUANLY);}
        private void xoaLocCaNhan() {
            dpNgay.setDate(null);txtTim.setText("");cboStatus.setSelectedIndex(3);
        }
        private void load() {load(null);}
        private void load(String maDonChon) {
            if(busy)return;
            run(()->new Loaded(manager?dao.layDonQuanLy():dao.layDonCuaToi(),
                    manager?List.of():shifts.get()),data->{
                var tk=SessionManager.getInstance().getCurrentUser();
                // Không hiện dữ liệu của phiên khác nếu tài khoản đổi trong lúc tải.
                rows=manager?data.requests():data.requests().stream()
                        .filter(d->tk.getNhanVien()!=null&&tk.getNhanVien().getMaNV()
                                .equals(d.ca().getNhanVien().getMaNV())).toList();
                own=data.shifts();filter();cboCa.removeAllItems();
                for(var ca:own) {
                    boolean owner=tk.getNhanVien()!=null&&tk.getNhanVien().getMaNV().equals(ca.getNhanVien().getMaNV());
                    boolean pending=rows.stream().anyMatch(d->d.trangThai().equals("CHO_DUYET")
                            &&d.ca().getMaLLV().equals(ca.getMaLLV()));
                    if(owner&&!pending&&ca.getTrangThai()==TrangThaiLichLamViec.DA_PHAN_CONG
                            &&ca.getNgayTruc().atTime(ca.getGioKetThuc()).isAfter(LocalDateTime.now()))cboCa.addItem(ca);
                }
                if(maDonChon!=null)for(int i=0;i<visible.size();i++) {
                    if(maDonChon.equals(visible.get(i).maDon())) {
                        int view=tbl.convertRowIndexToView(i);
                        tbl.setRowSelectionInterval(view,view);tbl.scrollRectToVisible(tbl.getCellRect(view,0,true));break;
                    }
                }
                controls();
            });
        }
        private record Loaded(List<Don> requests,List<LichLamViec> shifts){}
        private void filter(){if(busy)return;checked=null;affected.setRowCount(0);model.setRowCount(0);String[] statuses={"CHO_DUYET","DA_DUYET","TU_CHOI"};var date=dpNgay.getDate()==null?null:dpNgay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();String q=normalize(txtTim.getText());visible=rows.stream().filter(d->cboStatus.getSelectedIndex()==3||d.trangThai().equals(statuses[cboStatus.getSelectedIndex()])).filter(d->date==null||d.ca().getNgayTruc().equals(date)).filter(d->normalize(d.maDon()+" "+d.ca().getNhanVien().getMaNV()+" "+d.ca().getNhanVien().getHoTenNV()).contains(q)).toList();for(var d:visible)model.addRow(new Object[]{d.maDon(),d.ca().getNhanVien().getHoTenNV()+" · "+d.ca().getNhanVien().getMaNV(),d.ca().getNgayTruc().format(DATE),d.ca().getGioBatDau()+"–"+d.ca().getGioKetThuc(),d.ngayGui().format(STAMP),status(d.trangThai())});selection();}
        private Don selected(){int r=tbl.getSelectedRow();return r<0?null:visible.get(tbl.convertRowIndexToModel(r));}
        private void selection(){long version=++selectionVersion;checked=null;affected.setRowCount(0);txtReject.setText("");var d=selected();txtDetail.setText(d==null?(visible.isEmpty()?(rows.isEmpty()?"Chưa có đơn nghỉ phép.":"Có "+rows.size()+" đơn nhưng không khớp bộ lọc. Chọn Xóa lọc để xem lại."):"Chọn đơn để xem lý do và kết quả."):"Đơn "+d.maDon()+" · "+status(d.trangThai())+"\nLý do xin nghỉ: "+d.lyDo()+"\n"+(d.ngayXuLy()==null?"Chờ Quản lý tiếp nhận và xử lý. Ca làm việc vẫn giữ nguyên.":"Người xử lý: "+d.nguoiXuLy()+" · "+d.ngayXuLy().format(STAMP)+(d.lyDoTuChoi()==null?"":"\nLý do từ chối: "+d.lyDoTuChoi())));controls();
            if(manager)lbl.setText(d==null?"Chọn đơn để xem chi tiết và kiểm tra lịch ảnh hưởng.":d.trangThai().equals("CHO_DUYET")?"Đang chuẩn bị kiểm tra lịch ảnh hưởng...":"Đơn đã được xử lý.");
            if(manager&&d!=null&&d.trangThai().equals("CHO_DUYET"))SwingUtilities.invokeLater(()->{
                if(version==selectionVersion&&!busy&&selected()!=null&&selected().maDon().equals(d.maDon()))check();
            });
        }
        private void controls(){var d=selected();boolean pending=d!=null&&d.trangThai().equals("CHO_DUYET");btnGui.setEnabled(!busy&&cboCa.getSelectedItem()!=null&&valid(txtReason.getText()));btnApprove.setEnabled(!busy&&pending&&d.maDon().equals(checked)&&affected.getRowCount()==0);btnReject.setEnabled(!busy&&pending&&valid(txtReject.getText()));for(JComponent c:new JComponent[]{tbl,cboStatus,txtTim,dpNgay,cboCa,txtReason,txtReject,btnReload,btnClear})c.setEnabled(!busy);}
        private void send(){var ca=(LichLamViec)cboCa.getSelectedItem();if(busy||ca==null||!valid(txtReason.getText()))return;String reason=txtReason.getText().strip();if(!confirm.test("Gửi đơn xin nghỉ ngày "+ca.getNgayTruc().format(DATE)+" · "+ca.getGioBatDau()+"–"+ca.getGioKetThuc()+"?\nLý do: "+reason))return;run(()->dao.guiDon(ca.getMaLLV(),reason),id->{txtReason.setText("");xoaLocCaNhan();lbl.setText("Đã gửi "+id+" — chờ Quản lý duyệt. Ca làm chưa thay đổi.");load(id);});}
        private void check(){var d=selected();if(busy||d==null||!d.trangThai().equals("CHO_DUYET"))return;checked=null;lbl.setText("Đang tự động kiểm tra lịch hẹn ảnh hưởng...");run(()->dao.kiemTra(d.maDon()),list->{if(selected()==null||!selected().maDon().equals(d.maDon())){selection();return;}affected.setRowCount(0);for(var a:list)affected.addRow(new Object[]{a.maLH(),a.tenKhachHang(),a.dichVu(),a.batDau().format(STAMP),a.ketThuc().format(STAMP),a.trangThai().equals("DA_DEN")?"Đã đến":"Đã đặt"});checked=d.maDon();lbl.setText(list.isEmpty()?"Không có lịch hẹn ảnh hưởng. Có thể duyệt.":"Cần dời hoặc hủy các lịch hẹn ảnh hưởng trước khi duyệt.");controls();});}
        private void decide(boolean approved){var d=selected();if(busy||d==null||!d.trangThai().equals("CHO_DUYET")||(approved&&(!d.maDon().equals(checked)||affected.getRowCount()>0))||(!approved&&!valid(txtReject.getText())))return;String reason=txtReject.getText().strip();if(!confirm.test((approved?"Duyệt ":"Từ chối ")+d.maDon()+" của "+d.ca().getNhanVien().getHoTenNV()+"?\nNgày nghỉ: "+d.ca().getNgayTruc().format(DATE)+" · "+d.ca().getGioBatDau()+"–"+d.ca().getGioKetThuc()+"\nLý do xin nghỉ: "+d.lyDo()+(approved?"":"\nLý do từ chối: "+reason)))return;run(()->approved?dao.duyet(d.maDon()):dao.tuChoi(d.maDon(),reason),ok->{if(!ok)throw new BusinessException("Chưa lưu được kết quả.");lbl.setText(approved?"Đã duyệt đơn. Ca chuyển sang nghỉ phép.":"Đã từ chối đơn. Ca làm việc giữ nguyên.");refresh.run();load();});}
        private <T> void run(Supplier<T> job,Consumer<T> done) {
            if(!session()){lbl.setText("Phiên đăng nhập đã thay đổi. Hãy đóng và mở lại.");return;}
            busy=true;controls();
            new SwingWorker<T,Void>() {
                protected T doInBackground() {
                    if(!session())throw new BusinessException("Phiên đăng nhập đã thay đổi.");
                    return job.get();
                }
                protected void done() {
                    busy=false;
                    try {
                        T value=get();
                        if(!session()) {
                            rows=List.of();filter();cboCa.removeAllItems();txtDetail.setText("");
                            lbl.setText("Phiên đăng nhập đã thay đổi.");return;
                        }
                        done.accept(value);
                    } catch(Exception ex) {
                        checked=null;affected.setRowCount(0);
                        Throwable cause=ex instanceof ExecutionException?ex.getCause():ex;
                        if(ex instanceof InterruptedException)Thread.currentThread().interrupt();
                        lbl.setText(cause instanceof BusinessException||cause instanceof IllegalArgumentException
                                ?cause.getMessage():"Không thể xử lý lúc này. Hãy làm mới và thử lại.");
                    } finally {controls();}
                }
            }.execute();
        }
        private static boolean valid(String s){return s!=null&&!s.isBlank()&&s.strip().length()<=255;}
        private static String normalize(String s){return java.text.Normalizer.normalize(s.toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').strip();}
        private static String status(String s){return switch(s){case "CHO_DUYET"->"Chờ duyệt";case "DA_DUYET"->"Đã duyệt";default->"Từ chối";};}
    }
}
