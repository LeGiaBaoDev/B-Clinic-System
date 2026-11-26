import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;

/** Công cụ bảo trì chỉ chạy khi được yêu cầu rõ ràng; không được gọi từ App/Flyway tự động. */
public class ApplyFinanceSchema {
 static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
 static void command(Connection c,String sql)throws Exception{try(var s=c.createStatement()){boolean more=s.execute(sql);while(more || s.getUpdateCount()!=-1){if(more)try(var r=s.getResultSet()){while(r.next()){} }more=s.getMoreResults();}}}
 static Map<String,List<String>> kept(Connection c)throws Exception{
  Map<String,List<String>> result=new HashMap<>();
  try(var s=c.prepareStatement("SELECT t.vaiTro,t.maTK,t.matKhau,n.email,t.trangThai FROM TaiKhoan t JOIN NhanVien n ON n.maNV=t.maNV WHERE n.hoTenNV=?")){
   s.setNString(1,"Nguyễn Trọng Hiếu");try(var r=s.executeQuery()){while(r.next()){String role=r.getString(1);check(!result.containsKey(role),"Duplicate retained role");result.put(role,List.of(r.getString(2),r.getString(3),r.getString(4),r.getString(5)));}}
  }
  check(result.size()==3 && result.keySet().containsAll(List.of("QUANLY","NHASI","LETAN")),"Expected three Hieu roles");return result;
 }
 public static void main(String[] args)throws Exception{
  check(args.length==2 && args[0].equals("--apply-reset"),"Explicit --apply-reset and verified backup path required");
  Properties p=new Properties();try(var in=Files.newInputStream(Path.of("src/main/resources/database.properties"))){p.load(in);}
  check("BClinic_Dev".equals(p.getProperty("db.name")),"Only BClinic_Dev is allowed");
  check(Files.isRegularFile(Path.of(args[1])),"Backup not found");
  String url="jdbc:sqlserver://"+p.getProperty("db.host")+":"+p.getProperty("db.port")+";encrypt=true;trustServerCertificate=true;databaseName=BClinic_Dev";
  String user=p.getProperty("db.username"),pass=p.getProperty("db.password");Map<String,List<String>> before;
  try(var c=DriverManager.getConnection(url,user,pass)){
   before=kept(c);
   try(var s=c.prepareStatement("RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM")){s.setString(1,Path.of(args[1]).toAbsolutePath().toString());s.execute();}
  }
  Flyway.configure().dataSource(url,user,pass).locations("filesystem:src/main/resources/db/migration").group(true).load().migrate();
  try(var c=DriverManager.getConnection(url,user,pass)){
   command(c,Files.readString(Path.of("scripts/reset_keep_three_hieu.sql")).replace("\uFEFF",""));
   check(before.equals(kept(c)),"Retained credential/email changed");
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM TaiKhoan WHERE tenTK=maNV AND maNV IN('QL001','NS001','LT001')")){r.next();check(r.getInt(1)==3,"Unexpected accounts");}
   for(String table:List.of("KhachHang","HoSoBenhAn","LichHen","PhieuKham","ChiTietPhieuKham","HoaDon","ChiTietHoaDon","PhieuThu","Voucher","VoucherDichVu","DichVu","LoaiDichVu","LichLamViec","DonNghiPhep","NhatKyHeThong")){
    try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM dbo."+table)){r.next();check(r.getInt(1)==0,"Old rows remain in "+table);}
   }
   try(var s=c.createStatement()){boolean more=s.execute("DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS");while(more || s.getUpdateCount()!=-1){if(more)try(var r=s.getResultSet()){check(!r.next(),"Constraint violation");}more=s.getMoreResults();}}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM sys.foreign_keys WHERE is_disabled=1 OR is_not_trusted=1")){r.next();check(r.getInt(1)==0,"Disabled or untrusted FK");}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM sys.triggers WHERE parent_class=1 AND is_disabled=1")){r.next();check(r.getInt(1)==0,"Disabled trigger");}
   try(var s=c.createStatement();var r=s.executeQuery("SELECT n.maNV,t.tenTK,t.vaiTro,n.email FROM NhanVien n JOIN TaiKhoan t ON t.maNV=n.maNV ORDER BY t.vaiTro")){
    while(r.next())System.out.println(r.getString(1)+" | "+r.getString(2)+" | "+r.getString(3)+" | "+r.getString(4));
   }
  }
  System.out.println("FINANCE_RESET_OK old_business_rows=0 retained_accounts=3 passwords_and_emails_unchanged=true constraints_verified=true");
 }
}
