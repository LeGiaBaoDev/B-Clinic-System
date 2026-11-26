import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.util.SessionManager;
import java.math.BigDecimal;

/** Chỉ kiểm thử trên DB tên ngẫu nhiên biệt lập; KHÔNG sửa dữ liệu DB ứng dụng. */
public class VerifyFinanceSchema {
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 static int number(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getInt(1);}}
 static void command(Connection c,String sql)throws Exception{try(var s=c.createStatement()){boolean more=s.execute(sql);while(more || s.getUpdateCount()!=-1){if(more)try(var r=s.getResultSet()){while(r.next()){} }more=s.getMoreResults();}}}
 static void reject(Connection c,String sql)throws Exception{boolean failed=false;try(var s=c.createStatement()){s.execute(sql);}catch(SQLException e){failed=true;}check(failed,"Expected rejection");}
 public static void main(String[] args)throws Exception{
  Properties p=new Properties();try(var in=Files.newInputStream(Path.of("src/main/resources/database.properties"))){p.load(in);}
  String name="BClinic_Test_Finance_"+UUID.randomUUID().toString().replace("-","").substring(0,10);
  String base="jdbc:sqlserver://"+p.getProperty("db.host")+":"+p.getProperty("db.port")+";encrypt=true;trustServerCertificate=true;databaseName=";
  String user=p.getProperty("db.username"),password=p.getProperty("db.password");boolean created=false;Path tempConfig=null;
  try(Connection master=DriverManager.getConnection(base+"master",user,password)){
   try{
    if(args.length==1){
     String dataPath,logPath;
     try(var s=master.createStatement();var r=s.executeQuery("SELECT CAST(SERVERPROPERTY('InstanceDefaultDataPath') AS nvarchar(260)),CAST(SERVERPROPERTY('InstanceDefaultLogPath') AS nvarchar(260))")){r.next();dataPath=r.getString(1);logPath=r.getString(2);}
     String dataLogical=null,logLogical=null;
     try(var s=master.prepareStatement("RESTORE FILELISTONLY FROM DISK=?")){s.setString(1,args[0]);try(var r=s.executeQuery()){while(r.next()){if("D".equals(r.getString("Type")))dataLogical=r.getString("LogicalName");if("L".equals(r.getString("Type")))logLogical=r.getString("LogicalName");}}}
     try(var s=master.prepareStatement("RESTORE DATABASE ["+name+"] FROM DISK=? WITH MOVE ? TO ?,MOVE ? TO ?")){s.setString(1,args[0]);s.setString(2,dataLogical);s.setString(3,dataPath+name+".mdf");s.setString(4,logLogical);s.setString(5,logPath+name+"_log.ldf");s.execute();created=true;}
    }else{try(var s=master.createStatement()){s.execute("CREATE DATABASE ["+name+"]");created=true;}}
    Flyway.configure().dataSource(base+name,user,password).locations("filesystem:src/main/resources/db/migration").load().migrate();
    try(var c=DriverManager.getConnection(base+name,user,password);var s=c.createStatement()){
     command(c,Files.readString(Path.of("scripts/reset_keep_three_hieu.sql")).replace("\uFEFF",""));
     check(number(c,"SELECT COUNT(*) FROM TaiKhoan WHERE (tenTK='QL001' AND maNV='QL001' AND vaiTro='QUANLY') OR (tenTK='NS001' AND maNV='NS001' AND vaiTro='NHASI') OR (tenTK='LT001' AND maNV='LT001' AND vaiTro='LETAN')")==3,"Account mapping mismatch");
     check(number(c,"SELECT COUNT(*) FROM KhachHang")==0,"Reset not empty");
     command(c,Files.readString(Path.of("scripts/seed_finance_schema_test.sql")).replace("\uFEFF",""));
     check(number(c,"SELECT CAST(tongThanhToan AS int) FROM HoaDon WHERE maHD='HD261120001'")==1000000,"Discount not after VAT");
     s.execute("INSERT PhieuThu(maPT,maHD,ngayThu,soTienThu,hinhThucThanhToan,maNVThu,khoaYeuCau) VALUES('PT261120001','HD261120001','20261120 10:00',300000,N'Tiền mặt','LT001','first')");
     check(number(c,"SELECT COUNT(*) FROM HoaDon WHERE trangThaiThanhToan=N'THANH_TOAN_MOT_PHAN' AND tongDaThu=300000 AND conNo=700000")==1,"Partial state");
     reject(c,"INSERT PhieuThu(maPT,maHD,ngayThu,soTienThu,hinhThucThanhToan,maNVThu,khoaYeuCau) VALUES('PT261120002','HD261120001','20261120 10:01',700001,N'Tiền mặt','LT001','overpay')");
     reject(c,"INSERT PhieuThu(maPT,maHD,ngayThu,soTienThu,hinhThucThanhToan,maNVThu,khoaYeuCau) VALUES('PT261120003','HD261120001','20261120 10:01',1,N'Tiền mặt','LT001','first')");
     reject(c,"UPDATE ChiTietHoaDon SET tienGiam=0,thanhTien=1100000 WHERE maHD='HD261120001'");
     reject(c,"UPDATE HoaDon SET tenKhachHang=N'Đổi tên' WHERE maHD='HD261120001'");
     reject(c,"UPDATE PhieuThu SET soTienThu=1 WHERE maPT='PT261120001'");
     s.execute("INSERT PhieuThu(maPT,maHD,ngayThu,soTienThu,hinhThucThanhToan,maNVThu,khoaYeuCau) VALUES('PT261120002','HD261120001','20261120 10:30',700000,N'Chuyển khoản','LT001','second')");
     check(number(c,"SELECT COUNT(*) FROM HoaDon WHERE trangThai=1 AND trangThaiThanhToan=N'DA_THANH_TOAN' AND tongDaThu=1000000 AND conNo=0")==1,"Final payment");
     s.execute("UPDATE KhachHang SET tenKH=N'Tên Mới' WHERE maKH='KH00001'");
     check(number(c,"SELECT COUNT(*) FROM HoaDon WHERE tenKhachHang=N'Khách Kiểm Thử'")==1,"Snapshot changed");
     reject(c,"UPDATE TaiKhoan SET tenTK='hieu.nhasi' WHERE maNV='NS001'");
     reject(c,"INSERT NhanVien VALUES('NV099',N'Nhân Viên Thử',1,'0900000099','test99@example.com',N'Nha sĩ')");
     int ct=number(c,"SELECT maCTPK FROM ChiTietPhieuKham WHERE maPK='PK261120001'");
     s.execute("INSERT LichHen(maLH,maKH,maNV,maDV,maPKNguon,maCTPKNguon,ngayGioHen) VALUES('LH261121001','KH00001','NS001','DV001','PK261120001',"+ct+",'20261121 09:00')");
     reject(c,"INSERT LichHen(maLH,maKH,maNV,maDV,maPKNguon,maCTPKNguon,ngayGioHen) VALUES('LH261121002','KH00001','QL001','DV001','PK261120001',"+ct+",'20261121 10:00')");
     command(c,"INSERT LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,thoiLuongDuKien) VALUES('LH261120002','KH00001','NS001','DV001','20261120 11:00','DA_DEN',30); INSERT PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,maNV_NhaSiPhuTrach) VALUES('PK261120002','HS00001','LH261120002','LT001','20261120 11:00','DANG_KHAM','NS001'); INSERT ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,donViTinh,soLuong,donGia,mucThueGTGT,loaiGTGT) VALUES('PK261120002','DV001','NS001',N'Test',N'Test',N'Lần',2,1000000,10,N'CHIU_THUE'),('PK261120002','DV001','NS001',N'Test',N'Test',N'Lần',1,1000000,10,N'CHIU_THUE'); INSERT Voucher(maVoucher,tenVoucher,loaiGiam,giaGiam,ngayBatDau,ngayKetThuc,trangThai) VALUES('VC002',N'Giảm 5%',1,5,'20261101','20261201',1); INSERT VoucherDichVu VALUES('VC002','DV001');");
     Properties testProps=new Properties();testProps.putAll(p);testProps.setProperty("db.name",name);
     tempConfig=Files.createTempFile(Path.of("target"),"finance-dao-",".properties");try(var out=Files.newOutputStream(tempConfig)){testProps.store(out,"Isolated finance test");}
     System.setProperty("bclinic.config",tempConfig.toAbsolutePath().toString());
     TaiKhoan doctor=new TaiKhoan();doctor.setMaTK("TK002");doctor.setVaiTro(VaiTro.NHASI);SessionManager.getInstance().login(doctor);
     PhieuKhamDAO pd=new PhieuKhamDAO();pd.hoanTatPhieuKham(pd.layTheoMa("PK261120002"),List.of());
     check(number(c,"SELECT COUNT(*) FROM LichHen WHERE maLH='LH261120002' AND trangThai='HOAN_TAT'")==1,"Exam and schedule state");
     TaiKhoan receptionist=new TaiKhoan();receptionist.setMaTK("TK003");receptionist.setVaiTro(VaiTro.LETAN);SessionManager.getInstance().login(receptionist);
     HoaDonDAO hdDao=new HoaDonDAO();HoaDon invoice=hdDao.layTheoPhieuKham("PK261120002");String id=invoice.getMaHD();
     check(invoice.getTongThanhToan().compareTo(new BigDecimal("3300000"))==0,"DAO invoice snapshot");
     PhieuThuDAO receipts=new PhieuThuDAO();PhieuThu first=receipts.thuTien(id,new BigDecimal("135000"),HoaDon.TIEN_MAT,"dao:first",null);
     PhieuThu again=receipts.thuTien(id,new BigDecimal("135000"),HoaDon.TIEN_MAT,"dao:first",null);
     check(first.getMaPT().equals(again.getMaPT()),"DAO idempotency");
     invoice=hdDao.layTheoMa(id);
     check(invoice.getTongThanhToan().compareTo(new BigDecimal("3135000"))==0,"Best voucher per service");
     check(invoice.getTongDaThu().compareTo(new BigDecimal("135000"))==0 && invoice.getConNo().compareTo(new BigDecimal("3000000"))==0,"DAO balance");
     check(invoice.getTrangThaiThanhToan()==TrangThaiThanhToan.THANH_TOAN_MOT_PHAN,"DAO partial status");
     check(hdDao.layDanhSach(java.time.LocalDate.of(2026,11,20),java.time.LocalDate.now(),false).size()==1,"DAO list mapping");
     receipts.thuTien(id,new BigDecimal("3000000"),HoaDon.CHUYEN_KHOAN,"dao:second","transfer:second");
     check(hdDao.layTheoMa(id).isTrangThai(),"DAO settled status");
     check(receipts.layTheoHoaDon(id).size()==2,"DAO receipts");
     SessionManager.getInstance().logout();
     s.execute("DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS");
    }
    var result=Flyway.configure().dataSource(base+name,user,password).locations("filesystem:src/main/resources/db/migration").load().migrate();
    check(result.migrationsExecuted==0,"Migration not repeatable");
    System.out.println("FINANCE_SCHEMA_OK migration_reset_partial_payments_snapshots_constraints_verified clone="+(args.length==1));
   }finally{
    if(tempConfig!=null){DatabaseConnection.getInstance().getDataSource().close();Files.deleteIfExists(tempConfig);}
    if(created){try(var s=master.createStatement()){s.execute("ALTER DATABASE ["+name+"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE ["+name+"]");}}}
  }
 }
}
