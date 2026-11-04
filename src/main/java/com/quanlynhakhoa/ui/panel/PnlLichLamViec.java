package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.LichLamViecDAO;
import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** UC013: Quản lý phân công ca; mọi truy cập dữ liệu chạy ngoài EDT. */
public class PnlLichLamViec extends JPanel {
    private static final Logger LOG = Logger.getLogger(PnlLichLamViec.class.getName());
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Supplier<List<TaiKhoan>> layTaiKhoan;
    private final Supplier<List<LichLamViec>> layLich;
    private final Predicate<LichLamViec> luu;
    private final boolean chiXemCaNhan;
    private final String maTKCaNhan,maNVCaNhan;
    private final JComboBox<String> cboVaiTro = new JComboBox<>(new String[]{"Tất cả vị trí", "Nha sĩ", "Lễ tân", "Quản lý"});
    private final JComboBox<TaiKhoan> cboNhanVien = new JComboBox<>();
    private final JComboBox<String> cboCa = new JComboBox<>(new String[]{"Ca sáng", "Ca chiều", "Cả ngày"});
    private final JComboBox<String> cboCheDo = new JComboBox<>(new String[]{"Theo tuần", "Theo tháng"});
    private final com.toedter.calendar.JDateChooser dpMoc = new com.toedter.calendar.JDateChooser();
    private final com.toedter.calendar.JDateChooser dpNgay = new com.toedter.calendar.JDateChooser();
    private final JLabel lblGio = new JLabel("Chọn ca để xem giờ làm");
    private final JLabel lblThongBao = new JLabel(" ");
    private final JLabel lblKhoang = new JLabel(" ");
    private final JLabel lblKetQua = new JLabel("Chưa tải lịch làm việc.");
    private final JButton btnLuu = new JButton("Lưu phân công");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Ngày", "Nhân viên", "Vị trí", "Ca trực", "Giờ làm", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblLich = new JTable(model);
    private final JButton btnPhanCa = new JButton("+ Phân ca");
    private final JButton btnDoiCa = new JButton("Đổi ca");
    private final JButton btnNghiPhep = new JButton("Đơn nghỉ phép");
    private JPanel pnlForm;
    private final JComboBox<String> cboLocNhanVien = new JComboBox<>(new String[]{"Tất cả nhân viên"});
    private final JComboBox<String> cboLocVaiTro = new JComboBox<>(new String[]{"Tất cả chức vụ", "Nha sĩ", "Lễ tân", "Quản lý"});
    private final JComboBox<String> cboLocTrangThai = new JComboBox<>(new String[]{"Tất cả trạng thái ca", "Đã phân ca", "Đang trực", "Đã qua ca", "Nghỉ phép"});
    private final JPanel pnlTuan = new BangTuan();
    private final CardLayout cheDoLich = new CardLayout();
    private final JPanel pnlLich = new JPanel(cheDoLich);
    private List<TaiKhoan> taiKhoan = List.of();
    private List<LichLamViec> lich = List.of();
    private boolean dangTai, dangLuu, dangCapNhatBoLoc;
    private final javax.swing.Timer dongBoMauCa=new javax.swing.Timer(60_000,e -> {
        if(isShowing()) hienLich();
    });
    private record DuLieu(List<TaiKhoan> taiKhoan, List<LichLamViec> lich) { }

    public PnlLichLamViec() {
        this(new TaiKhoanDAO()::layTatCa, new LichLamViecDAO()::layTatCa, new LichLamViecDAO()::them);
    }

    PnlLichLamViec(Supplier<List<TaiKhoan>> layTaiKhoan, Supplier<List<LichLamViec>> layLich, Predicate<LichLamViec> luu) {
        this(layTaiKhoan,layLich,luu,false);
    }
    PnlLichLamViec(Supplier<List<TaiKhoan>> layTaiKhoan, Supplier<List<LichLamViec>> layLich, Predicate<LichLamViec> luu,boolean chiXemCaNhan) {
        super(new BorderLayout(16, 16));
        this.chiXemCaNhan=chiXemCaNhan;
        TaiKhoan current=SessionManager.getInstance().getCurrentUser();
        maTKCaNhan=chiXemCaNhan&&current!=null?current.getMaTK():null;
        maNVCaNhan=chiXemCaNhan&&current!=null&&current.getNhanVien()!=null?current.getNhanVien().getMaNV():null;
        this.layTaiKhoan = java.util.Objects.requireNonNull(layTaiKhoan);
        this.layLich = java.util.Objects.requireNonNull(layLich);
        this.luu = java.util.Objects.requireNonNull(luu);
        setName(chiXemCaNhan?"Lịch của tôi":"Lịch làm việc"); setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel header = new JPanel(new BorderLayout(16, 0)); header.setOpaque(false);
        JPanel titles = new JPanel(); titles.setLayout(new BoxLayout(titles,BoxLayout.Y_AXIS)); titles.setOpaque(false);
        JLabel title = new JLabel(chiXemCaNhan?"Lịch của tôi":"Lịch làm việc"); title.setFont(bold(23)); title.setForeground(TEXT);
        JLabel subtitle = new JLabel(chiXemCaNhan?"Lịch làm việc và nghỉ phép cá nhân · Chỉ xem":"Phân công và theo dõi ca làm việc của nhân viên"); subtitle.setFont(font(12)); subtitle.setForeground(MUTED);
        titles.add(title); titles.add(Box.createVerticalStrut(5)); titles.add(subtitle); header.add(titles,BorderLayout.CENTER);
        btnPhanCa.setName("moPhanCa"); btnPhanCa.setBackground(TEAL); btnPhanCa.setForeground(Color.WHITE); btnPhanCa.setFont(bold(13));
        btnPhanCa.addActionListener(e -> {
            capNhatNgayToiThieu();
            pnlForm.setVisible(!pnlForm.isVisible());
            btnPhanCa.setText(pnlForm.isVisible()?"Đóng phân ca":"+ Phân ca");
            revalidate(); repaint();
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actions.setOpaque(false); actions.add(btnPhanCa);
        btnDoiCa.setName("moDoiCa");btnDoiCa.addActionListener(e -> {
            if(chiXemCaNhan||!laQuanLy()||dangTai||dangLuu)return;
            new com.quanlynhakhoa.ui.dialog.DlgDoiCa(SwingUtilities.getWindowAncestor(this),lich,taiKhoan,this::lamMoiDuLieu).setVisible(true);
        });
        actions.add(btnDoiCa,0);
        btnNghiPhep.setName("moNghiPhep");btnNghiPhep.setToolTipText("Tiếp nhận, duyệt hoặc từ chối đơn nhân viên đã gửi.");
        btnNghiPhep.addActionListener(e->{
            if(chiXemCaNhan||!laQuanLy()||dangTai||dangLuu)return;
            new com.quanlynhakhoa.ui.dialog.DlgNghiPhep(SwingUtilities.getWindowAncestor(this),true,this::lamMoiDuLieu).setVisible(true);
        });actions.add(btnNghiPhep,1);
        header.add(actions,BorderLayout.EAST); add(header,BorderLayout.NORTH);
        SurfacePanel list = new SurfacePanel(new BorderLayout(0, 12)); list.setBorder(new EmptyBorder(14,14,12,14));
        JPanel controls = new JPanel(new BorderLayout(0,12)); controls.setOpaque(false);
        JPanel navigation = new JPanel(new BorderLayout()); navigation.setOpaque(false);
        JPanel dates = new JPanel(new FlowLayout(FlowLayout.LEFT,7,0)); dates.setOpaque(false);
        JButton prev = new JButton("‹"), next = new JButton("›"), today = new JButton("Hôm nay");
        prev.setName("lichTruoc"); next.setName("lichSau"); today.setName("lichHomNay");
        lblKhoang.setName("khoangNgayLich");
        lblKhoang.setFont(bold(14)); lblKhoang.setForeground(TEXT);
        dpMoc.setName("mocLich"); dpMoc.setLocale(java.util.Locale.forLanguageTag("vi-VN"));
        dpMoc.setDateFormatString("dd/MM/yyyy"); dpMoc.setPreferredSize(new Dimension(140,30));
        dpMoc.setDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        ((JTextField)dpMoc.getDateEditor().getUiComponent()).setEditable(false);
        dpMoc.setToolTipText("Chọn ngày để mở tuần hoặc tháng chứa ngày đó.");
        dpMoc.getJCalendar().setPreferredSize(new Dimension(330,280));
        dates.add(prev); dates.add(dpMoc); dates.add(next); dates.add(today);
        navigation.add(dates,BorderLayout.WEST);
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.RIGHT,7,0)); modes.setOpaque(false);
        JToggleButton week=new JToggleButton("Tuần",true), month=new JToggleButton("Tháng");
        week.setName("xemTuan"); month.setName("xemThang");
        ButtonGroup modeGroup=new ButtonGroup(); modeGroup.add(week); modeGroup.add(month);
        week.addActionListener(e -> cboCheDo.setSelectedIndex(0)); month.addActionListener(e -> cboCheDo.setSelectedIndex(1));
        modes.add(week); modes.add(month); modes.add(btnLamMoi); navigation.add(modes,BorderLayout.EAST);
        controls.add(navigation,BorderLayout.NORTH);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); filters.setOpaque(false);
        cboLocNhanVien.setName("locNhanVienLich"); cboLocVaiTro.setName("locChucVuLich"); cboLocTrangThai.setName("locTrangThaiLich");
        filters.add(cboLocNhanVien); filters.add(cboLocVaiTro); filters.add(cboLocTrangThai);
        JLabel hours = new JLabel("Sáng 08:00–12:00 · Chiều 13:30–17:30"); hours.setFont(font(11)); hours.setForeground(MUTED); filters.add(hours);
        controls.add(filters,BorderLayout.CENTER); controls.add(lblKhoang,BorderLayout.SOUTH); list.add(controls,BorderLayout.NORTH);
        tblLich.setName("bangLichLamViec"); tblLich.setRowHeight(42); tblLich.setFont(font(12));
        tblLich.getTableHeader().setFont(bold(12)); tblLich.setAutoCreateRowSorter(true);
        ((javax.swing.table.TableRowSorter<?>)tblLich.getRowSorter()).setComparator(0,
                (x,y) -> LocalDate.parse(x.toString(),NGAY).compareTo(LocalDate.parse(y.toString(),NGAY)));
        tblLich.setFillsViewportHeight(true); tblLich.setShowVerticalLines(false); tblLich.setGridColor(BORDER);
        tblLich.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {110, 220, 100, 100, 140, 130};
        for (int i=0;i<widths.length;i++) tblLich.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        pnlLich.setOpaque(false); pnlLich.add(new JScrollPane(pnlTuan),"tuan");
        pnlLich.add(new JScrollPane(tblLich),"thang"); list.add(pnlLich,BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        lblKetQua.setFont(font(11)); lblKetQua.setForeground(MUTED);
        lblKetQua.setName("ketQuaLich"); btnLamMoi.setName("lamMoiLich");
        footer.add(lblKetQua,BorderLayout.EAST); list.add(footer,BorderLayout.SOUTH);
        add(list,BorderLayout.CENTER);
        pnlForm=taoForm(); pnlForm.setVisible(false); add(pnlForm,BorderLayout.EAST);
        if(chiXemCaNhan) {
            actions.remove(btnPhanCa); actions.remove(btnDoiCa); actions.remove(btnNghiPhep); remove(pnlForm);
            filters.remove(cboLocNhanVien); filters.remove(cboLocVaiTro);
        }
        for(JComboBox<?> filter: new JComboBox<?>[]{cboLocNhanVien,cboLocVaiTro,cboLocTrangThai}) filter.addActionListener(e -> {
            if(!dangCapNhatBoLoc) hienLich();
        });
        prev.addActionListener(e -> doiKy(-1)); next.addActionListener(e -> doiKy(1));
        today.addActionListener(e -> dpMoc.setDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())));
        cboNhanVien.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean selected, boolean focus) {
                super.getListCellRendererComponent(l,v,i,selected,focus);
                if (v instanceof TaiKhoan tk) setText(tk.getNhanVien().getHoTenNV()+" · "+tk.getNhanVien().getMaNV());
                return this;
            }
        });
        cboVaiTro.addActionListener(e -> locNhanVien());
        cboCa.addActionListener(e -> {
            lblGio.setText(switch(cboCa.getSelectedIndex()) { case 0 -> "08:00 – 12:00"; case 1 -> "13:30 – 17:30"; case 2 -> "08:00 – 17:30"; default -> "Chọn ca để xem giờ làm"; });
            kiemTraLuaChon();
        });
        cboNhanVien.addActionListener(e -> kiemTraLuaChon());
        dpNgay.addPropertyChangeListener("date",e -> kiemTraLuaChon());
        cboCheDo.addActionListener(e -> hienLich()); dpMoc.addPropertyChangeListener("date",e -> hienLich());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu()); btnLuu.addActionListener(e -> luuPhanCong());
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5,0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() { public void actionPerformed(ActionEvent e) { lamMoiDuLieu(); }});
        capNhatNut();
    }

    private JPanel taoForm() {
        SurfacePanel form = new SurfacePanel(new BorderLayout(0, 12)); form.setPreferredSize(new Dimension(290,0));
        form.setBorder(new EmptyBorder(20,20,20,20));
        JLabel heading = new JLabel("Phân công ca mới"); heading.setFont(bold(18)); heading.setForeground(TEXT);
        form.add(heading, BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridBagLayout()); fields.setOpaque(false);
        themTruong(fields,0,"Vị trí",cboVaiTro,"viTri");
        themTruong(fields,1,"Nhân viên đang hoạt động *",cboNhanVien,"nhanVienPhanCong");
        cboCa.setSelectedIndex(-1);
        themTruong(fields,2,"Ca làm việc *",cboCa,"caPhanCong");
        dpNgay.setLocale(java.util.Locale.forLanguageTag("vi-VN"));
        dpNgay.setDateFormatString("dd/MM/yyyy");
        capNhatNgayToiThieu();
        dpNgay.setDate(null);
        ((JTextField)dpNgay.getDateEditor().getUiComponent()).setEditable(false);
        dpNgay.setToolTipText("Chọn nhân viên và ca trước. Lịch chỉ cho chọn ngày còn trống của ca đó.");
        dpNgay.getJCalendar().setPreferredSize(new Dimension(330,280));
        dpNgay.getJCalendar().getDayChooser().addDateEvaluator(new com.toedter.calendar.IDateEvaluator() {
            private String reason;
            public boolean isInvalid(Date date) {
                LocalDate day=date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                reason=day.isBefore(LocalDate.now())?"Không phân công ngày trong quá khứ.":lyDoKhongThePhanCong(day);
                return reason!=null;
            }
            public String getInvalidTooltip() { return reason; }
            public Color getInvalidForegroundColor() { return MUTED; }
            public Color getInvalidBackroundColor() { return new Color(0xF5EAE6); }
            public boolean isSpecial(Date date) { return false; }
            public Color getSpecialForegroundColor() { return null; }
            public Color getSpecialBackroundColor() { return null; }
            public String getSpecialTooltip() { return null; }
        });
        themTruong(fields,3,"Ngày làm việc còn trống *",dpNgay,"ngayPhanCong");
        lblGio.setFont(bold(16)); lblGio.setForeground(TEAL);
        GridBagConstraints g = new GridBagConstraints(); g.gridx=0; g.gridy=8; g.weightx=1; g.weighty=1;
        g.fill=GridBagConstraints.HORIZONTAL; g.anchor=GridBagConstraints.NORTHWEST;
        lblGio.setName("gioTheoCa"); lblGio.setToolTipText("Giờ làm việc tự điền theo ca đã chọn."); fields.add(lblGio,g);
        JScrollPane scroll = new JScrollPane(fields);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        form.add(scroll,BorderLayout.CENTER);
        form.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "luu");
        form.getActionMap().put("luu",new AbstractAction() { public void actionPerformed(ActionEvent e) { luuPhanCong(); }});
        JPanel bottom = new JPanel(new BorderLayout(0,12)); bottom.setOpaque(false);
        lblThongBao.setName("thongBaoPhanCong"); lblThongBao.setPreferredSize(new Dimension(240,72)); lblThongBao.setVerticalAlignment(SwingConstants.TOP);
        btnLuu.setName("luuPhanCong"); btnLuu.setFont(bold(13)); btnLuu.setBackground(TEAL); btnLuu.setForeground(Color.WHITE);
        btnLuu.setPreferredSize(new Dimension(240,40)); bottom.add(lblThongBao,BorderLayout.CENTER); bottom.add(btnLuu,BorderLayout.SOUTH);
        form.add(bottom,BorderLayout.SOUTH); return form;
    }

    private static void themTruong(JPanel p,int row,String text,JComponent field,String name) {
        GridBagConstraints g = new GridBagConstraints(); g.gridx=0; g.gridy=row*2; g.weightx=1;
        g.anchor=GridBagConstraints.WEST; g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(0,0,4,0);
        JLabel label = new JLabel(text); label.setFont(font(12)); label.setForeground(MUTED); label.setLabelFor(field); p.add(label,g);
        g.gridy++; g.insets=new Insets(0,0,10,0); field.setName(name); field.setFont(font(13));
        field.setPreferredSize(new Dimension(240,34)); p.add(field,g);
    }

    private static LocalDate docNgay(com.toedter.calendar.JDateChooser chooser) throws java.text.ParseException {
        Date date=chooser.getDate();
        if(date==null) throw new java.text.ParseException("Chưa chọn ngày xem lịch.",0);
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    private void capNhatNgayToiThieu() {
        dpNgay.setMinSelectableDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }
    private LocalDate docNgayPhanCong() throws java.text.ParseException {
        Date value=dpNgay.getDate();
        if(value==null) throw new java.text.ParseException("Chưa chọn ngày làm việc hợp lệ.",0);
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    private boolean laQuanLy() {
        TaiKhoan tk=SessionManager.getInstance().getCurrentUser(); return tk!=null && tk.getVaiTro()==VaiTro.QUANLY;
    }
    private boolean duocXemLich() {
        if(!chiXemCaNhan) return laQuanLy();
        TaiKhoan current=SessionManager.getInstance().getCurrentUser();
        return maTKCaNhan!=null&&maNVCaNhan!=null&&current!=null&&maTKCaNhan.equals(current.getMaTK())
                &&current.getNhanVien()!=null&&maNVCaNhan.equals(current.getNhanVien().getMaNV());
    }
    private void capNhatNut() {
        boolean ready=duocXemLich()&&!dangLuu&&!dangTai&&!chiXemCaNhan;
        btnDoiCa.setEnabled(ready&&lich.stream().anyMatch(x -> x.getTrangThai()==TrangThaiLichLamViec.DA_PHAN_CONG&&x.getNgayTruc().atTime(x.getGioBatDau()).isAfter(LocalDateTime.now())));
        btnNghiPhep.setEnabled(ready);
        btnPhanCa.setEnabled(ready); btnLuu.setEnabled(ready&&cboNhanVien.getItemCount()>0&&cboCa.getSelectedIndex()>=0&&dpNgay.getDate()!=null&&lyDoKhongThePhanCong()==null); btnLamMoi.setEnabled(duocXemLich()&&!dangLuu&&!dangTai);
        for (JComponent field : new JComponent[]{cboVaiTro,cboNhanVien,dpNgay,cboCa}) field.setEnabled(ready);
        dpNgay.setEnabled(ready&&cboNhanVien.getSelectedItem()!=null&&cboCa.getSelectedIndex()>=0);
        var calendar=dpNgay.getJCalendar();
        calendar.getDayChooser().setMonth(calendar.getCalendar().get(java.util.Calendar.MONTH));
    }
    public void lamMoiDuLieu() {
        if(dangTai||dangLuu) return;
        if(!duocXemLich()) {
            if(chiXemCaNhan) { taiKhoan=List.of(); lich=List.of(); hienLich(); }
            lblKetQua.setText("Vui lòng đăng nhập để xem lịch."); thongBao("Chỉ Quản lý được phân công lịch làm việc.",false); capNhatNut(); return;
        }
        dangTai=true; capNhatNut(); lblKetQua.setText("Đang tải lịch làm việc...");
        new SwingWorker<DuLieu,Void>() {
            protected DuLieu doInBackground() { return new DuLieu(layTaiKhoan.get(),layLich.get()); }
            protected void done() {
                boolean loaded=false;
                try {
                    DuLieu d=get();
                    if(!duocXemLich()) { taiKhoan=List.of(); lich=List.of(); hienLich(); lblKetQua.setText("Phiên đăng nhập đã thay đổi. Hãy mở lại Lịch của tôi."); return; }
                    taiKhoan=List.copyOf(d.taiKhoan());
                    lich=chiXemCaNhan?d.lich().stream().filter(x -> maNVCaNhan.equals(x.getNhanVien().getMaNV())).toList():List.copyOf(d.lich());
                    locNhanVien(); capNhatBoLoc(); hienLich(); loaded=true;
                }
                catch(InterruptedException ex) { Thread.currentThread().interrupt(); lblKetQua.setText("Đã gián đoạn tải lịch."); }
                catch(ExecutionException ex) {
                    if(chiXemCaNhan) { taiKhoan=List.of(); lich=List.of(); hienLich(); }
                    lblKetQua.setText("Không thể tải lịch. Nhấn Làm mới để thử lại."); lblKetQua.setToolTipText(ex.getCause() instanceof BusinessException?ex.getCause().getMessage():null); baoLoi(ex.getCause());
                }
                finally {
                    dangTai=false; capNhatNut();
                    if(loaded&&!chiXemCaNhan&&!lblThongBao.getText().contains("thành công")) kiemTraLuaChon();
                }
            }
        }.execute();
    }
    private void locNhanVien() {
        TaiKhoan old=(TaiKhoan)cboNhanVien.getSelectedItem(); cboNhanVien.removeAllItems();
        int role=cboVaiTro.getSelectedIndex();
        for(TaiKhoan tk:taiKhoan) if(tk.isTrangThai()&&tk.getNhanVien()!=null
                &&(role==0||tenVaiTro(tk.getVaiTro()).equals(cboVaiTro.getSelectedItem()))) {
            cboNhanVien.addItem(tk);
            if(old!=null&&old.getMaTK().equals(tk.getMaTK())) cboNhanVien.setSelectedItem(tk);
        }
        if(cboNhanVien.getItemCount()==0&&!dangTai) thongBao("Không có nhân viên hoạt động ở vị trí này.",false);
        capNhatNut();
    }
    private String lyDoKhongThePhanCong() {
        if(dpNgay.getDate()==null) return null;
        LocalDate day=dpNgay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return lyDoKhongThePhanCong(day);
    }
    private String lyDoKhongThePhanCong(LocalDate day) {
        TaiKhoan selected=(TaiKhoan)cboNhanVien.getSelectedItem();
        if(selected==null||cboCa.getSelectedIndex()<0) return "Vui lòng chọn nhân viên và ca làm việc trước.";
        CaTruc ca=CaTruc.values()[cboCa.getSelectedIndex()];
        LocalTime start=ca==CaTruc.CA_CHIEU?LocalTime.of(13,30):LocalTime.of(8,0);
        LocalTime end=ca==CaTruc.CA_SANG?LocalTime.NOON:LocalTime.of(17,30);
        List<LichLamViec> conflicts=lich.stream().filter(x -> x.getNhanVien().getMaNV().equals(selected.getNhanVien().getMaNV()))
                .filter(x -> day.equals(x.getNgayTruc()))
                .filter(x -> x.getCaTruc()==ca||(x.getGioBatDau().isBefore(end)&&x.getGioKetThuc().isAfter(start))).toList();
        if(conflicts.isEmpty()) return null;
        boolean leave=conflicts.stream().anyMatch(x -> x.getTrangThai()==TrangThaiLichLamViec.NGHI_PHEP);
        return leave?"Nhân viên đã nghỉ phép trong ca này ngày "+day.format(NGAY)+". Hãy chọn ca hoặc nhân viên khác."
                :"Nhân viên đã được phân công ca trùng ngày "+day.format(NGAY)+". Hãy chọn ca hoặc nhân viên khác.";
    }
    private void kiemTraLuaChon() {
        if(dangTai||dangLuu) return;
        capNhatNut();
        String reason=lyDoKhongThePhanCong();
        if(reason!=null) thongBao(reason,false);
        else if(cboCa.getSelectedIndex()<0) thongBao("Chọn ca làm việc trước, sau đó chọn ngày còn trống trên lịch.",true);
        else if(dpNgay.getDate()==null) thongBao("Vui lòng chọn ngày làm việc hợp lệ trên lịch.",true);
        else thongBao(" ",true);
    }
    private void hienLich() {
        try {
            LocalDate moc=docNgay(dpMoc);
            LocalDate tu=cboCheDo.getSelectedIndex()==0?moc.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)):moc.withDayOfMonth(1);
            LocalDate den=cboCheDo.getSelectedIndex()==0?tu.plusDays(6):tu.with(TemporalAdjusters.lastDayOfMonth());
            cheDoLich.show(pnlLich,cboCheDo.getSelectedIndex()==0?"tuan":"thang");
            veTuan(tu);
            lblKhoang.setText(tu.format(NGAY)+" – "+den.format(NGAY)); model.setRowCount(0);
            lich.stream().filter(this::quaBoLoc).filter(x -> !x.getNgayTruc().isBefore(tu)&&!x.getNgayTruc().isAfter(den))
                .sorted(java.util.Comparator.comparing(LichLamViec::getNgayTruc).thenComparing(LichLamViec::getGioBatDau))
                .forEach(x -> model.addRow(new Object[]{x.getNgayTruc().format(NGAY), x.getNhanVien().getHoTenNV()+" · "+x.getNhanVien().getMaNV(),
                    taiKhoan.stream().filter(t->t.getNhanVien()!=null&&t.getNhanVien().getMaNV().equals(x.getNhanVien().getMaNV())).map(t->tenVaiTro(t.getVaiTro())).findFirst().orElse("—"),
                    switch(x.getCaTruc()) {case CA_SANG->"Ca sáng";case CA_CHIEU->"Ca chiều";case CA_NGAY->"Cả ngày";},
                    x.getGioBatDau()+" – "+x.getGioKetThuc(),trangThaiCa(x)}));
            lblKetQua.setText(model.getRowCount()==0?"Chưa có ca làm việc trong khoảng này.":model.getRowCount()+" ca làm việc");
        } catch(java.text.ParseException ex) { lblKhoang.setText("Chọn ngày để xem lịch."); }
    }
    private void capNhatBoLoc() {
        dangCapNhatBoLoc=true;
        try {
        Object selected=cboLocNhanVien.getSelectedItem();
        cboLocNhanVien.removeAllItems(); cboLocNhanVien.addItem("Tất cả nhân viên");
        taiKhoan.stream().filter(t -> t.getNhanVien()!=null).forEach(t -> cboLocNhanVien.addItem(
                t.getNhanVien().getHoTenNV()+" · "+t.getNhanVien().getMaNV()));
        for(int i=0;i<cboLocNhanVien.getItemCount();i++) if(cboLocNhanVien.getItemAt(i).equals(selected)) cboLocNhanVien.setSelectedIndex(i);
        } finally { dangCapNhatBoLoc=false; }
    }
    private String chucVu(LichLamViec ca) {
        return taiKhoan.stream().filter(t -> t.getNhanVien()!=null&&t.getNhanVien().getMaNV().equals(ca.getNhanVien().getMaNV()))
                .map(t -> tenVaiTro(t.getVaiTro())).findFirst().orElse("Chưa có chức vụ");
    }
    private boolean quaBoLoc(LichLamViec ca) {
        return quaBoLoc(ca,trangThaiCa(ca));
    }
    private boolean quaBoLoc(LichLamViec ca,String status) {
        String nv=ca.getNhanVien().getHoTenNV()+" · "+ca.getNhanVien().getMaNV();
        return (cboLocNhanVien.getSelectedIndex()<=0||nv.equals(cboLocNhanVien.getSelectedItem()))
                &&(cboLocVaiTro.getSelectedIndex()<=0||chucVu(ca).equals(cboLocVaiTro.getSelectedItem()))
                &&(cboLocTrangThai.getSelectedIndex()<=0||status.equals(cboLocTrangThai.getSelectedItem()));
    }
    static String trangThaiTrongBuoi(LichLamViec ca,int buoi,LocalDateTime now) {
        LocalTime start=ca.getGioBatDau(), end=ca.getGioKetThuc();
        if(buoi==1&&end.isAfter(LocalTime.NOON)) end=LocalTime.NOON;
        if(buoi==2&&start.isBefore(LocalTime.of(13,30))) start=LocalTime.of(13,30);
        if(ca.getTrangThai()==TrangThaiLichLamViec.NGHI_PHEP) return "Nghỉ phép";
        if(now.isBefore(ca.getNgayTruc().atTime(start))) return "Đã phân ca";
        return now.isBefore(ca.getNgayTruc().atTime(end))?"Đang trực":"Đã qua ca";
    }
    private static String trangThaiCa(LichLamViec ca) {
        if(ca.getTrangThai()==TrangThaiLichLamViec.NGHI_PHEP) return "Nghỉ phép";
        LocalDateTime now=LocalDateTime.now();
        if(now.isBefore(ca.getNgayTruc().atTime(ca.getGioBatDau()))) return "Đã phân ca";
        return now.isBefore(ca.getNgayTruc().atTime(ca.getGioKetThuc()))?"Đang trực":"Đã qua ca";
    }
    private static Color mauTrangThai(String status) {
        return switch(status) { case "Đang trực" -> GREEN; case "Đã phân ca" -> TEAL; case "Nghỉ phép" -> new Color(0xB87833); default -> MUTED; };
    }
    private void veTuan(LocalDate first) {
        pnlTuan.removeAll();
        String[] days={"Thứ Hai","Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy","Chủ nhật"};
        for(int col=0;col<8;col++) {
            LocalDate date=first.plusDays(Math.max(0,col-1));
            JLabel head=new JLabel(col==0?"Ca làm việc":"<html>"+days[col-1]+"<br>"+date.format(DateTimeFormatter.ofPattern("dd/MM"))+"</html>");
            head.setFont(bold(12)); head.setForeground(date.equals(LocalDate.now())&&col>0?TEAL:TEXT);
            JPanel cell=new JPanel(new BorderLayout()); cell.setBackground(new Color(0xF0F4F8)); cell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0,0,1,1,BORDER),new EmptyBorder(12,10,12,8))); cell.add(head);
            themO(cell,col,0,0);
        }
        for(int row=1;row<=2;row++) {
            final int period=row;
            JPanel label=new JPanel(); label.setLayout(new BoxLayout(label,BoxLayout.Y_AXIS)); label.setBackground(new Color(0xF0F4F8));
            label.setBorder(new EmptyBorder(18,10,12,8));
            for(String text: new String[]{row==1?"Ca sáng":"Ca chiều",row==1?"08:00–12:00":"13:30–17:30"}) {
                JLabel l=new JLabel(text); l.setFont(row==1&&text.equals("Ca sáng")?bold(12):font(11)); l.setForeground(MUTED); label.add(l); label.add(Box.createVerticalStrut(9));
            }
            themO(label,0,row,1);
            for(int col=1;col<=7;col++) {
                LocalDate date=first.plusDays(col-1);
                JPanel cell=new JPanel(); cell.setLayout(new BoxLayout(cell,BoxLayout.Y_AXIS));
                cell.setBackground(date.equals(LocalDate.now())?new Color(0xF0FAF8):Color.WHITE);
                cell.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,1,BORDER),new EmptyBorder(6,6,6,6)));
                List<LichLamViec> shifts=lich.stream().filter(x -> quaBoLoc(x,trangThaiTrongBuoi(x,period,LocalDateTime.now()))).filter(x -> date.equals(x.getNgayTruc()))
                        .filter(x -> period==1?x.getGioBatDau().isBefore(LocalTime.NOON):x.getGioKetThuc().isAfter(LocalTime.of(13,30)))
                        .sorted(java.util.Comparator.comparing(x -> x.getNhanVien().getHoTenNV())).toList();
                for(LichLamViec ca:shifts) { cell.add(theNhanVien(ca,period)); cell.add(Box.createVerticalStrut(4)); }
                if(shifts.isEmpty()) { JLabel empty=new JLabel("<html>Chưa phân ca</html>"); empty.setFont(font(10)); empty.setForeground(MUTED); cell.add(Box.createVerticalGlue()); cell.add(empty); }
                cell.add(Box.createVerticalGlue()); themO(cell,col,row,1);
            }
        }
        pnlTuan.revalidate(); pnlTuan.repaint();
    }
    private JPanel theNhanVien(LichLamViec ca,int buoi) {
        String status=trangThaiTrongBuoi(ca,buoi,LocalDateTime.now());
        JPanel card=new JPanel(); card.setName("ca-"+ca.getMaLLV()); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE); card.setAlignmentX(LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,2,0,0,
                        ca.getTrangThai()==TrangThaiLichLamViec.NGHI_PHEP?mauTrangThai("Nghỉ phép"):"Đang trực".equals(status)?GREEN:BORDER),new EmptyBorder(4,5,4,4))));
        String name=ca.getNhanVien().getHoTenNV();
        JLabel ten=new JLabel(name); ten.putClientProperty("html.disable",Boolean.TRUE); ten.setFont(bold(11)); ten.setForeground(TEXT);
        JLabel role=new JLabel(chucVu(ca)); role.setName("chucVuCa"); role.setFont(font(10)); role.setForeground(TEAL);
        card.add(ten); card.add(Box.createVerticalStrut(2)); card.add(role);
        if(ca.getTrangThai()==TrangThaiLichLamViec.NGHI_PHEP) {
            card.setBackground(new Color(0xFFF5E8));
        } else if("Đang trực".equals(status)) {
            card.setBackground(new Color(0xE7F5EC));
        }
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,60));
        card.setToolTipText(name+" · "+chucVu(ca)+" · "+ca.getGioBatDau()+"–"+ca.getGioKetThuc()+" · "+(buoi==1?"Buổi sáng: ":"Buổi chiều: ")+status);
        return card;
    }
    private static String html(String value) { return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    @Override public void addNotify() { super.addNotify(); dongBoMauCa.start(); }
    @Override public void removeNotify() { dongBoMauCa.stop(); super.removeNotify(); }
    private void themO(JComponent cell,int col,int row,double weightY) {
        GridBagConstraints g=new GridBagConstraints(); g.gridx=col; g.gridy=row; g.weightx=col==0?0:1;
        g.weighty=weightY; g.fill=GridBagConstraints.BOTH;
        cell.setMinimumSize(new Dimension(col==0?95:110,row==0?50:150)); pnlTuan.add(cell,g);
    }
    private static final class BangTuan extends JPanel implements Scrollable {
        BangTuan() { super(new GridBagLayout()); setName("lichTuan"); setBackground(Color.WHITE); }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(950,430); }
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction) { return 150; }
        public boolean getScrollableTracksViewportWidth() { return getParent()!=null&&getParent().getWidth()>=getMinimumSize().width; }
        public boolean getScrollableTracksViewportHeight() { return getParent()!=null&&getParent().getHeight()>=getPreferredSize().height; }
    }
    private void doiKy(int direction) {
        try { LocalDate d=docNgay(dpMoc); d=cboCheDo.getSelectedIndex()==0?d.plusWeeks(direction):d.plusMonths(direction);
            dpMoc.setDate(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } catch(java.text.ParseException ex) { lblKhoang.setText("Chọn ngày để xem lịch."); }
    }
    private void luuPhanCong() {
        if(chiXemCaNhan) return;
        if(dangLuu||dangTai) return;
        if(!laQuanLy()) { thongBao("Chỉ Quản lý được phân công lịch làm việc.",false); return; }
        final LichLamViec ca;
        try {
            TaiKhoan tk=(TaiKhoan)cboNhanVien.getSelectedItem();
            if(tk==null) throw new IllegalArgumentException("Vui lòng chọn nhân viên đang hoạt động.");
            if(cboCa.getSelectedIndex()<0) throw new IllegalArgumentException("Vui lòng chọn ca làm việc trước.");
            LocalDate d=docNgayPhanCong();
            if(d.isBefore(LocalDate.now())) throw new IllegalArgumentException("Không phân công ca trong quá khứ.");
            String reason=lyDoKhongThePhanCong();
            if(reason!=null) throw new IllegalArgumentException(reason);
            ca=new LichLamViec(); ca.setNhanVien(tk.getNhanVien()); ca.setNgayTruc(d);
            ca.setCaTruc(CaTruc.values()[cboCa.getSelectedIndex()]); ca.ganGioTheoCa();
        } catch(java.text.ParseException ex) { thongBao("Vui lòng chọn ngày làm việc hợp lệ trên lịch.",false); dpNgay.getCalendarButton().requestFocusInWindow(); return; }
        catch(IllegalArgumentException ex) { thongBao(ex.getMessage(),false); return; }
        dangLuu=true; capNhatNut(); thongBao("Đang lưu phân công...",true);
        new SwingWorker<Boolean,Void>() {
            protected Boolean doInBackground() { return luu.test(ca); }
            protected void done() {
                boolean saved=false;
                try { if(!get()) throw new BusinessException("Chưa lưu được phân công. Vui lòng thử lại."); saved=true; }
                catch(InterruptedException ex) { Thread.currentThread().interrupt(); thongBao("Đã gián đoạn lưu. Hãy tải lại lịch để kiểm tra.",false); }
                catch(ExecutionException ex) { baoLoi(ex.getCause()); }
                catch(BusinessException ex) { thongBao(ex.getMessage(),false); }
                finally { dangLuu=false; capNhatNut(); }
                if(saved) {
                    dpMoc.setDate(Date.from(ca.getNgayTruc().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    thongBao("Phân công lịch làm việc thành công.",true); lamMoiDuLieu();
                }
            }
        }.execute();
    }
    private void baoLoi(Throwable ex) {
        LOG.log(Level.WARNING,"Không thể xử lý lịch làm việc",ex);
        thongBao(ex instanceof BusinessException||ex instanceof IllegalArgumentException?ex.getMessage():"Không thể xử lý lúc này. Vui lòng thử lại.",false);
    }
    private void thongBao(String message,boolean success) {
        String safe=message.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        lblThongBao.setText("<html>"+safe+"</html>"); lblThongBao.setForeground(success?GREEN:new Color(0xC33F49));
    }
    private static String tenVaiTro(VaiTro role) { return switch(role) {case QUANLY->"Quản lý";case NHASI->"Nha sĩ";case LETAN->"Lễ tân";}; }
}
