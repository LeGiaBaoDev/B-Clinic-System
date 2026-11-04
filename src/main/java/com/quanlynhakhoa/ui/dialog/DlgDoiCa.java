package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.LichLamViecDAO;
import com.quanlynhakhoa.dao.LichLamViecDAO.YeuCauDoiCa;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class DlgDoiCa extends JDialog {
    public DlgDoiCa(Window owner,List<LichLamViec> lich,List<TaiKhoan> accounts,Runnable daLuu) {
        super(owner,"Đổi ca làm việc",ModalityType.APPLICATION_MODAL);
        LichLamViecDAO dao=new LichLamViecDAO();
        NoiDung form=new NoiDung(lich,accounts,dao::kiemTraDoiCa,dao::doiCa,
                text -> JOptionPane.showConfirmDialog(this,text,"Xác nhận đổi ca",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION,
                ()->{dispose();daLuu.run();JOptionPane.showMessageDialog(owner,"Đổi ca làm việc thành công.");},this::dispose);
        setContentPane(form); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){form.dong();}});
        getRootPane().setDefaultButton(form.btnLuu);
        getRootPane().registerKeyboardAction(e->form.dong(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        Rectangle bounds=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(760,bounds.width-32),Math.min(780,bounds.height-40));setMinimumSize(new Dimension(600,540));setLocationRelativeTo(owner);
    }
    static final class NoiDung extends JPanel {
        private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private final List<LichLamViec> lich;
        private final List<TaiKhoan> accounts;
        private final Consumer<YeuCauDoiCa> check;
        private final Predicate<YeuCauDoiCa> save;
        private final Predicate<String> confirm;
        private final Runnable success,close;
        private final JComboBox<LichLamViec> cboNguon=new JComboBox<>(),cboDich=new JComboBox<>();
        private final JComboBox<String> cboKieu=new JComboBox<>(new String[]{"Dời ngày / ca làm","Hoán đổi hai nhân viên"});
        private final JComboBox<String> cboCa=new JComboBox<>(new String[]{"Ca sáng (08:00–12:00)","Ca chiều (13:30–17:30)","Cả ngày (08:00–17:30)"});
        private final com.toedter.calendar.JDateChooser dpNgay=new com.toedter.calendar.JDateChooser();
        private final CardLayout cards=new CardLayout();
        private final JPanel targets=new JPanel(cards);
        private final JLabel lblLoi=new JLabel(" ");
        private final JButton btnLuu=new JButton("Kiểm tra và đổi ca"),btnHuy=new JButton("Hủy");
        private final BoLocCa locNguon,locDich;
        private boolean busy,dangLoc;
        NoiDung(List<LichLamViec> lich,List<TaiKhoan> accounts,Consumer<YeuCauDoiCa> check,Predicate<YeuCauDoiCa> save,
                Predicate<String> confirm,Runnable success,Runnable close) {
            super(new BorderLayout(0,16));this.lich=List.copyOf(lich);this.accounts=List.copyOf(accounts);
            this.check=check;this.save=save;this.confirm=confirm;this.success=success;this.close=close;
            setBackground(SURFACE);setBorder(new EmptyBorder(22,24,22,24));targets.setOpaque(false);
            locNguon=new BoLocCa("nguon",this::napNguon);locDich=new BoLocCa("dich",this::napDich);
            JLabel title=new JLabel("Đổi ca làm việc");title.setFont(bold(22));title.setForeground(TEXT);
            JPanel header=new JPanel(new BorderLayout(0,12));header.setOpaque(false);header.add(title,BorderLayout.NORTH);header.add(locNguon,BorderLayout.CENTER);add(header,BorderLayout.NORTH);
            JPanel fields=new JPanel(new GridBagLayout());fields.setOpaque(false);
            renderer(cboNguon);renderer(cboDich);
            field(fields,0,"Ca cần đổi *",cboNguon,"caNguon");field(fields,1,"Cách đổi",cboKieu,"cachDoiCa");
            JPanel move=new JPanel(new GridBagLayout());move.setOpaque(false);
            field(move,0,"Ca mới *",cboCa,"caMoi");
            dpNgay.setLocale(java.util.Locale.forLanguageTag("vi-VN"));dpNgay.setDateFormatString("dd/MM/yyyy");
            dpNgay.setMinSelectableDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            ((JTextField)dpNgay.getDateEditor().getUiComponent()).setEditable(false);
            dpNgay.getJCalendar().getDayChooser().addDateEvaluator(new com.toedter.calendar.IDateEvaluator(){
                public boolean isInvalid(Date date){return trungNgay(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());}
                public boolean isSpecial(Date date){return false;}
                public Color getInvalidForegroundColor(){return MUTED;}public Color getInvalidBackroundColor(){return new Color(0xF5EAE6);}
                public String getInvalidTooltip(){return "Ca đã bắt đầu, trùng lịch hoặc nghỉ phép. Hãy chọn ngày khác.";}
                public Color getSpecialForegroundColor(){return null;}public Color getSpecialBackroundColor(){return null;}public String getSpecialTooltip(){return null;}
            });
            field(move,1,"Ngày mới còn trống *",dpNgay,"ngayMoi");targets.add(move,"doi");
            JPanel swap=new JPanel(new BorderLayout(0,12));swap.setOpaque(false);swap.add(locDich,BorderLayout.NORTH);
            JPanel swapFields=new JPanel(new GridBagLayout());swapFields.setOpaque(false);field(swapFields,0,"Ca của nhân viên cùng chức vụ *",cboDich,"caHoanDoi");swap.add(swapFields,BorderLayout.CENTER);targets.add(swap,"hoan");
            GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=4;g.weightx=1;g.weighty=1;g.fill=GridBagConstraints.BOTH;fields.add(targets,g);
            JScrollPane scroll=new JScrollPane(fields);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(SURFACE);add(scroll,BorderLayout.CENTER);
            JPanel bottom=new JPanel(new BorderLayout(0,12));bottom.setOpaque(false);lblLoi.setName("loiDoiCa");lblLoi.setPreferredSize(new Dimension(550,64));lblLoi.setForeground(new Color(0xC33F49));bottom.add(lblLoi,BorderLayout.CENTER);
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));buttons.setOpaque(false);
            btnLuu.setName("luuDoiCa");btnLuu.setFont(bold(13));btnLuu.setBackground(TEAL);btnLuu.setForeground(Color.WHITE);btnHuy.setName("huyDoiCa");
            buttons.add(btnHuy);buttons.add(btnLuu);bottom.add(buttons,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
            cboNguon.addActionListener(e->{if(!dangLoc)doiNguon();});cboKieu.addActionListener(e->{cards.show(targets,cboKieu.getSelectedIndex()==0?"doi":"hoan");capNhat();});
            cboCa.addActionListener(e->{dpNgay.getJCalendar().getDayChooser().setMonth(dpNgay.getJCalendar().getCalendar().get(java.util.Calendar.MONTH));capNhat();});
            cboDich.addActionListener(e->{if(!dangLoc)capNhat();});dpNgay.addPropertyChangeListener("date",e->capNhat());
            btnHuy.addActionListener(e->dong());btnLuu.addActionListener(e->luu());napNguon();
        }
        private static boolean doiDuoc(LichLamViec ca){return ca.getTrangThai()==TrangThaiLichLamViec.DA_PHAN_CONG&&ca.getNgayTruc().atTime(ca.getGioBatDau()).isAfter(LocalDateTime.now());}
        private VaiTro role(LichLamViec ca){return accounts.stream().filter(t->t.getNhanVien()!=null&&t.isTrangThai()&&t.getNhanVien().getMaNV().equals(ca.getNhanVien().getMaNV())).map(TaiKhoan::getVaiTro).findFirst().orElse(null);}
        private void doiNguon(){
            LichLamViec source=(LichLamViec)cboNguon.getSelectedItem();
            if(source!=null){
                cboCa.setSelectedIndex(source.getCaTruc().ordinal());
            }
            locDich.coDinhChucVu(source==null?null:role(source));
            dpNgay.setDate(null);napDich();capNhat();
        }
        private void napNguon(){
            if(busy)return;
            LichLamViec old=(LichLamViec)cboNguon.getSelectedItem();
            dangLoc=true;
            try{napCombo(cboNguon,lich.stream().filter(NoiDung::doiDuoc).filter(x->role(x)!=null).filter(locNguon::khop).toList(),old);}
            finally{dangLoc=false;}
            locNguon.dem(cboNguon.getItemCount());
            LichLamViec selected=(LichLamViec)cboNguon.getSelectedItem();
            if(old==null||selected==null||!old.getMaLLV().equals(selected.getMaLLV()))doiNguon();else napDich();
            capNhat();
        }
        private void napDich(){
            if(busy)return;
            LichLamViec source=(LichLamViec)cboNguon.getSelectedItem(),old=(LichLamViec)cboDich.getSelectedItem();
            List<LichLamViec> matches=source==null?List.of():lich.stream().filter(NoiDung::doiDuoc)
                .filter(x->!x.getNhanVien().getMaNV().equals(source.getNhanVien().getMaNV()))
                .filter(x->role(source)!=null&&role(x)==role(source)).filter(locDich::khop).toList();
            dangLoc=true;try{napCombo(cboDich,matches,old);}finally{dangLoc=false;}
            locDich.dem(cboDich.getItemCount());capNhat();
        }
        private void napCombo(JComboBox<LichLamViec> combo,List<LichLamViec> matches,LichLamViec old){
            combo.removeAllItems();matches.stream().sorted(java.util.Comparator.comparing(LichLamViec::getNgayTruc).thenComparing(LichLamViec::getGioBatDau)).forEach(combo::addItem);
            if(old!=null)for(int i=0;i<combo.getItemCount();i++)if(old.getMaLLV().equals(combo.getItemAt(i).getMaLLV())){combo.setSelectedIndex(i);break;}
        }
        private final class BoLocCa extends JPanel {
            private final JComboBox<String> chucVu=new JComboBox<>(new String[]{"Tất cả chức vụ","Nha sĩ","Lễ tân","Quản lý"});
            private final JComboBox<String> ca=new JComboBox<>(new String[]{"Tất cả ca","Ca sáng","Ca chiều","Cả ngày"});
            private final com.toedter.calendar.JDateChooser ngay=new com.toedter.calendar.JDateChooser();
            private final JTextField tim=new JTextField();
            private final JButton loc=new JButton("Lọc"),xoa=new JButton("Xóa lọc");
            private final JLabel ketQua=new JLabel();private VaiTro fixedRole;private boolean coDinh;
            BoLocCa(String prefix,Runnable apply){
                super(new BorderLayout(0,8));setOpaque(false);
                JPanel selectors=new JPanel(new GridLayout(1,3,8,0));selectors.setOpaque(false);
                chucVu.setName(prefix+"ChucVu");ca.setName(prefix+"Ca");ngay.setName(prefix+"Ngay");
                chucVu.setToolTipText("Chức vụ nhân viên");ca.setToolTipText("Ca làm việc");
                ngay.setDateFormatString("dd/MM/yyyy");ngay.setLocale(java.util.Locale.forLanguageTag("vi-VN"));((JTextField)ngay.getDateEditor().getUiComponent()).setEditable(false);((JTextField)ngay.getDateEditor().getUiComponent()).putClientProperty("JTextField.placeholderText","Ngày làm việc");ngay.setToolTipText("Ngày của ca cần tìm; để trống để xem mọi ngày");
                selectors.add(chucVu);selectors.add(ca);selectors.add(ngay);add(selectors,BorderLayout.NORTH);
                JPanel search=new JPanel(new BorderLayout(8,0));search.setOpaque(false);tim.setName(prefix+"Tim");tim.putClientProperty("JTextField.placeholderText","Tên hoặc mã nhân viên, ví dụ: Nguyễn Văn An / NV002");tim.setToolTipText("Tìm theo tên không dấu hoặc mã nhân viên");search.add(tim,BorderLayout.CENTER);
                JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));buttons.setOpaque(false);loc.setName(prefix+"Loc");xoa.setName(prefix+"XoaLoc");buttons.add(loc);buttons.add(xoa);search.add(buttons,BorderLayout.EAST);add(search,BorderLayout.CENTER);
                ketQua.setName(prefix+"KetQua");ketQua.setFont(font(11));ketQua.setForeground(MUTED);add(ketQua,BorderLayout.SOUTH);
                loc.addActionListener(e->apply.run());tim.addActionListener(e->apply.run());xoa.addActionListener(e->{chucVu.setSelectedIndex(coDinh?index(fixedRole):0);ca.setSelectedIndex(0);ngay.setDate(null);tim.setText("");apply.run();});
            }
            private int index(VaiTro role){return role==VaiTro.NHASI?1:role==VaiTro.LETAN?2:role==VaiTro.QUANLY?3:0;}
            void coDinhChucVu(VaiTro role){coDinh=true;fixedRole=role;chucVu.setSelectedIndex(index(role));chucVu.setEnabled(false);}
            boolean khop(LichLamViec item){
                if(chucVu.getSelectedIndex()!=0&&index(role(item))!=chucVu.getSelectedIndex())return false;
                if(ca.getSelectedIndex()!=0&&item.getCaTruc().ordinal()!=ca.getSelectedIndex()-1)return false;
                if(ngay.getDate()!=null&&!item.getNgayTruc().equals(ngay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()))return false;
                String text=boDau(item.getNhanVien().getHoTenNV()+" "+item.getNhanVien().getMaNV());
                return java.util.Arrays.stream(boDau(tim.getText()).trim().split("\\s+")).allMatch(text::contains);
            }
            void dem(int count){ketQua.setText(count==0?"Không có ca phù hợp. Hãy đổi điều kiện hoặc Xóa lọc.":count+" ca phù hợp");}
            void khoa(boolean lock){for(JComponent c:new JComponent[]{chucVu,ca,ngay,tim,loc,xoa})c.setEnabled(!lock);if(coDinh)chucVu.setEnabled(false);}
        }
        private static String boDau(String text){return java.text.Normalizer.normalize(text.toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d');}
        private boolean trungNgay(LocalDate day){
            LichLamViec source=(LichLamViec)cboNguon.getSelectedItem();if(source==null)return true;
            CaTruc ca=CaTruc.values()[cboCa.getSelectedIndex()];LocalTime start=ca==CaTruc.CA_CHIEU?LocalTime.of(13,30):LocalTime.of(8,0),end=ca==CaTruc.CA_SANG?LocalTime.NOON:LocalTime.of(17,30);
            if(!day.atTime(start).isAfter(LocalDateTime.now())||(day.equals(source.getNgayTruc())&&ca==source.getCaTruc()))return true;
            return lich.stream().filter(x->!x.getMaLLV().equals(source.getMaLLV())&&x.getNhanVien().getMaNV().equals(source.getNhanVien().getMaNV())&&x.getNgayTruc().equals(day))
                .anyMatch(x->x.getCaTruc()==ca||(x.getGioBatDau().isBefore(end)&&x.getGioKetThuc().isAfter(start)));
        }
        private void capNhat(){
            locNguon.khoa(busy);locDich.khoa(busy);
            for(JComponent field:new JComponent[]{cboNguon,cboKieu,cboCa,cboDich,dpNgay,btnHuy})field.setEnabled(!busy);
            boolean ready=cboNguon.getSelectedItem()!=null&&(cboKieu.getSelectedIndex()==1?cboDich.getSelectedItem()!=null:dpNgay.getDate()!=null&&!trungNgay(dpNgay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));
            btnLuu.setEnabled(!busy&&ready);
        }
        private void luu(){
            if(busy)return;var actor=SessionManager.getInstance().getCurrentUser();if(actor==null||actor.getVaiTro()!=VaiTro.QUANLY){loi("Chỉ Quản lý được đổi ca.");return;}
            LichLamViec source=(LichLamViec)cboNguon.getSelectedItem();if(source==null)return;
            boolean swap=cboKieu.getSelectedIndex()==1;LichLamViec other=swap?(LichLamViec)cboDich.getSelectedItem():null;
            if(swap&&other==null){loi("Không có ca của nhân viên cùng chức vụ để hoán đổi.");return;}
            if(!swap&&(dpNgay.getDate()==null||trungNgay(dpNgay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()))){loi("Hãy chọn ngày/ca mới còn trống, chưa bắt đầu.");return;}
            YeuCauDoiCa request=new YeuCauDoiCa(new LichLamViec(source),other==null?null:new LichLamViec(other),swap?null:dpNgay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),swap?null:CaTruc.values()[cboCa.getSelectedIndex()]);
            busy=true;capNhat();loi("Đang kiểm tra lịch...");
            new SwingWorker<Void,Void>(){
                protected Void doInBackground(){check.accept(request);return null;}
                protected void done(){
                    try{get();
                        String text=swap?"Hoán đổi ca của "+label(source)+"\nvới "+label(other)+"?":"Dời "+label(source)+"\nsang "+request.ngayMoi().format(DATE)+" · "+cboCa.getSelectedItem()+"?";
                        if(!confirm.test(text)){busy=false;capNhat();loi("Đã hủy xác nhận, lịch chưa thay đổi.");return;}
                        ghi(request);
                    }catch(Exception ex){ketThucLoi(ex);}
                }
            }.execute();
        }
        private void ghi(YeuCauDoiCa request){
            loi("Đang lưu đổi ca...");new SwingWorker<Boolean,Void>(){
                protected Boolean doInBackground(){return save.test(request);}
                protected void done(){try{if(!get())throw new BusinessException("Chưa đổi được ca. Vui lòng thử lại.");busy=false;capNhat();success.run();}catch(Exception ex){ketThucLoi(ex);}}
            }.execute();
        }
        private void ketThucLoi(Exception ex){busy=false;capNhat();Throwable cause=ex instanceof ExecutionException?ex.getCause():ex;if(ex instanceof InterruptedException)Thread.currentThread().interrupt();loi(cause instanceof BusinessException||cause instanceof IllegalArgumentException?cause.getMessage():"Không thể đổi ca lúc này. Vui lòng tải lại và thử lại.");}
        private void loi(String text){lblLoi.setText("<html>"+text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")+"</html>");}
        void dong(){if(!busy)close.run();}
        private static String label(LichLamViec ca){return ca.getNhanVien().getHoTenNV()+" · "+ca.getNhanVien().getMaNV()+" · "+ca.getNgayTruc().format(DATE)+" · "+ca.getGioBatDau()+"–"+ca.getGioKetThuc();}
        private static void renderer(JComboBox<LichLamViec> combo){combo.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean selected,boolean focus){super.getListCellRendererComponent(l,v,i,selected,focus);if(v instanceof LichLamViec ca)setText(label(ca));return this;}});}
        private static void field(JPanel p,int row,String title,JComponent field,String name){GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=row*2;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(0,0,6,0);JLabel label=new JLabel(title);label.setFont(font(12));label.setForeground(MUTED);p.add(label,g);g.gridy++;g.insets=new Insets(0,0,18,0);field.setName(name);field.setFont(font(13));field.setPreferredSize(new Dimension(550,36));p.add(field,g);}
    }
}
