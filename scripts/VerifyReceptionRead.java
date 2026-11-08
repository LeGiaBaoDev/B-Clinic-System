import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.util.SessionManager;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Kiểm chứng chỉ đọc DB cấu hình hiện tại; không tạo/sửa dữ liệu. */
public class VerifyReceptionRead {
 static final LocalDate DAY=LocalDate.of(2026,11,6);
 static String login(VaiTro role) throws Exception {
  try(var c=DatabaseConnection.getInstance().getConnection();var s=c.prepareStatement("SELECT TOP 1 maTK,maNV FROM TaiKhoan WHERE vaiTro=? AND trangThai=1 ORDER BY maTK")){
   s.setString(1,role.name());try(var r=s.executeQuery()){
    if(!r.next())throw new AssertionError("Missing role");
    TaiKhoan t=new TaiKhoan();t.setMaTK(r.getString(1));t.setVaiTro(role);SessionManager.getInstance().login(t);return r.getString(2);
   }
  }
 }
 static <T> List<T> measure(String name,Supplier<List<T>> read){long t=System.nanoTime();var rows=read.get();System.out.println(name+" rows="+rows.size()+" ms="+(System.nanoTime()-t)/1000000);return rows;}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static int count(String sql)throws Exception{try(var c=DatabaseConnection.getInstance().getConnection();var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getInt(1);}}
 public static void main(String[] args)throws Exception{
  try{
   login(VaiTro.LETAN);
   var ld=new LichHenDAO();var pd=new PhieuKhamDAO();var hd=new HoaDonDAO();
   var today=measure("appointments_today",()->ld.timKiem(DAY,DAY,null));
   var week=measure("appointments_7days",()->ld.timKiem(DAY,DAY.plusDays(7),null));
   var summary=measure("dashboard_appointments",()->ld.layTomTat(DAY,DAY.plusDays(7)));
   check(summary.size()==week.size(),"Summary count");
   var exams=measure("exams_today",()->pd.timKiem(DAY,DAY));
   check(pd.demDangCho(DAY)==exams.stream().filter(p0->p0.getTrangThai()==TrangThaiPhieuKham.CHO_KHAM||p0.getTrangThai()==TrangThaiPhieuKham.DANG_KHAM).count(),"Queue count");
   var allExams=measure("exam_search",()->pd.timKiem(""));
   var customers=measure("customer_search",()->new KhachHangDAO().timKiemTomTat(""));
   var invoices=measure("invoices_today",()->hd.layDanhSach(DAY,DAY,null));
   check(today.size()==count("SELECT COUNT(*) FROM LichHen WHERE ngayGioHen>='20261106' AND ngayGioHen<'20261107'"),"Appointment count");
   check(exams.size()==count("SELECT COUNT(*) FROM PhieuKham WHERE ngayLap>='20261106' AND ngayLap<'20261107'"),"Exam count");
   check(customers.size()==count("SELECT COUNT(*) FROM KhachHang"),"Customer count");
   for(var p:allExams){check(p.getLoiDan()==null&&p.getHoSoBenhAn().getTienSuBenh()==null,"Reception privacy");check(p.getHoSoBenhAn().getKhachHang()!=null,"Missing exam patient");}
   int checked=0;
   for(var l:week){
    var old=ld.layTheoMa(l.getMaLH());
    check(old.getKhachHang().getMaKH().equals(l.getKhachHang().getMaKH()),"Patient mismatch");
    check(old.getNhaSi().getMaNV().equals(l.getNhaSi().getMaNV()),"Dentist mismatch");
    check(old.getDichVu().getMaDV().equals(l.getDichVu().getMaDV()),"Service mismatch");
    check(Objects.equals(old.getPhieuKhamNguon()==null?null:old.getPhieuKhamNguon().getMaPK(),l.getPhieuKhamNguon()==null?null:l.getPhieuKhamNguon().getMaPK()),"Source mismatch");
    if(++checked==3)break;
   }
   var repeat=week.stream().filter(l->l.getPhieuKhamNguon()!=null).findFirst();
   if(repeat.isPresent())check(repeat.get().getPhieuKhamNguon().getLoiDan()==null&&repeat.get().getPhieuKhamNguon().getHoSoBenhAn().getTienSuBenh()==null,"Source privacy");
   String dentist=login(VaiTro.NHASI);
   for(var l:ld.timKiem(DAY,DAY.plusDays(7),null))check(l.getNhaSi().getMaNV().equals(dentist),"Dentist scope");
   for(var p:pd.timKiem(""))check(p.getLichHen()!=null&&p.getLichHen().getNhaSi().getMaNV().equals(dentist),"Exam scope");
   boolean denied=false;try{hd.layDanhSach(DAY,DAY,null);}catch(com.quanlynhakhoa.exception.BusinessException e){denied=true;}check(denied,"Dentist invoice permission");
   check(ld.timKiem("NO_MATCH_99999",DAY,DAY,null).isEmpty(),"Empty search");
   System.out.println("RECEPTION_READ_OK counts_details_privacy_and_permissions_verified");
  }finally{SessionManager.getInstance().logout();}
 }
}
