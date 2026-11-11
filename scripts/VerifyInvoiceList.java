import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.HoaDonDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.ui.panel.TraCuuHoaDon;
import com.quanlynhakhoa.util.SessionManager;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

public class VerifyInvoiceList {
 public static void main(String[] args) throws Exception {
  try {
   TaiKhoan actor=new TaiKhoan();
   try(var c=DatabaseConnection.getInstance().getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT TOP 1 maTK FROM TaiKhoan WHERE vaiTro='LETAN' AND trangThai=1 ORDER BY maTK")) {
    if(!r.next())throw new AssertionError("No receptionist"); actor.setMaTK(r.getString(1));actor.setVaiTro(VaiTro.LETAN);
   }
   SessionManager.getInstance().login(actor);
   var from=LocalDate.of(2026,10,8);var to=LocalDate.of(2026,11,6);
   long start=System.nanoTime();
   var rows=new TraCuuHoaDon().tim("",TraCuuHoaDon.TrangThai.TAT_CA,from,to);
   long elapsed=(System.nanoTime()-start)/1000000;
   Map<String,BigDecimal> expected=new HashMap<>();
   try(var c=DatabaseConnection.getInstance().getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT hd.maHD,COALESCE((SELECT SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0)) FROM ChiTietPhieuKham ct WHERE ct.maPK=hd.maPK),0)-hd.tongKhuyenMai FROM HoaDon hd WHERE hd.ngayLap>='20261008' AND hd.ngayLap<'20261107'")) {
    while(r.next())expected.put(r.getString(1),r.getBigDecimal(2).max(BigDecimal.ZERO));
   }
   if(rows.size()!=expected.size())throw new AssertionError("Count mismatch");
   for(var row:rows)if(row.tongThanhToan().compareTo(expected.get(row.hoaDon().getMaHD()))!=0)throw new AssertionError("Amount mismatch");
   var unpaid=new HoaDonDAO().layDanhSach(from,to,false);
   var paid=new HoaDonDAO().layDanhSach(from,to,true);
   if(unpaid.size()+paid.size()!=rows.size())throw new AssertionError("Status mismatch");
   if(new TraCuuHoaDon().tim("KH00004",TraCuuHoaDon.TrangThai.TAT_CA,from,to).isEmpty())throw new AssertionError("Customer search missing");
   System.out.println("INVOICE_LIST_OK count="+rows.size()+" elapsed_ms="+elapsed+" amounts_verified="+expected.size()+" paid="+paid.size()+" unpaid="+unpaid.size());
  } finally {SessionManager.getInstance().logout();}
 }
}
