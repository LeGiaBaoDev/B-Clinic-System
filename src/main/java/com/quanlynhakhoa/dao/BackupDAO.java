package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.*;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.*;
import com.quanlynhakhoa.util.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/** UC039/041/042: master connection, verify, pre-restore và lịch sử độc lập database. */
public class BackupDAO extends DaoSupport {
    @FunctionalInterface public interface KetNoiMaster { Connection mo() throws SQLException; }
    @FunctionalInterface public interface BaoTri { AutoCloseable batDau() throws Exception; }
    private static final ReentrantLock LOCK = new ReentrantLock();
    private final KetNoiMaster master;
    private final BaoTri baoTri;
    private final Runnable dangXuat;
    private final QuyDinhSaoLuu quyDinh;

    public BackupDAO() {
        this(DaoContext.macDinh(),DatabaseConnection.getInstance()::getMasterConnection,
                DatabaseConnection.getInstance()::tamDungDeKhoiPhuc,
                () -> SessionManager.getInstance().logout(),cauHinh());
    }
    public BackupDAO(DaoContext context,KetNoiMaster master,BaoTri baoTri,Runnable dangXuat,QuyDinhSaoLuu quyDinh) {
        super(context); this.master=Objects.requireNonNull(master); this.baoTri=Objects.requireNonNull(baoTri);
        this.dangXuat=Objects.requireNonNull(dangXuat); this.quyDinh=Objects.requireNonNull(quyDinh);
    }
    private static QuyDinhSaoLuu cauHinh() {
        java.util.Properties p=new java.util.Properties();
        try(var in=BackupDAO.class.getClassLoader().getResourceAsStream("app.properties")) {
            if(in!=null) { p.load(in); }
        } catch(IOException e) { throw new IllegalStateException("Không đọc được cấu hình sao lưu.",e); }
        String dir=p.getProperty("backup.dir");
        if(dir==null || dir.isBlank()) {
            try(Connection c=DatabaseConnection.getInstance().getMasterConnection()) {
                dir=one(c,"SELECT CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS NVARCHAR(4000))",r -> r.getString(1));
            } catch(SQLException e) { throw new DataAccessException("Không đọc được thư mục backup mặc định của SQL Server.",e); }
        }
        return new QuyDinhSaoLuu(dir,Path.of(p.getProperty("backup.history","backups/backup-history.csv")),Integer.parseInt(p.getProperty("backup.timeoutSeconds","120")));
    }
    public record ThongTinBanSaoLuu(String database,LocalDateTime ngayTao,BigDecimal kichThuoc,int viTri,int loai) { }
    public record LichSu(LocalDateTime thoiGian,String thaoTac,String file,String ketQua,String maTK) { }

    /** Chỉ chấp nhận file trực tiếp trong backup.dir, không cho traversal hoặc SQL fragment. */
    public String validateDuongDan(String file) {
        can(file!=null && !file.isBlank(),"Thiếu file sao lưu.");
        String normalized=file.replace('\\','/');
        String dir=quyDinh.thuMucMayChu().replace('\\','/').replaceAll("/+$", "");
        can(!normalized.contains("..") && !normalized.contains("'") && !normalized.contains(";")
                && !normalized.contains("\n") && !normalized.contains("\r")
                && normalized.startsWith(dir+"/") && normalized.substring(dir.length()+1).matches("[A-Za-z0-9_-]+\\.bak"),
                "File phải nằm trực tiếp trong thư mục backup và có đuôi .bak hợp lệ.");
        return normalized;
    }
    private String database() {
        return context.doc(c -> { quyen(c,VaiTro.QUANLY); String db=c.getCatalog();
            can(db!=null && db.matches("[A-Za-z][A-Za-z0-9_]{0,127}") && !List.of("master","model","msdb","tempdb").contains(db.toLowerCase(java.util.Locale.ROOT)),"Tên database ứng dụng không hợp lệ."); return db; });
    }
    private Connection master() throws SQLException {
        Connection c=master.mo();
        if(!"master".equalsIgnoreCase(c.getCatalog())) { c.close(); throw new SQLException("Backup/restore bắt buộc kết nối master."); }
        c.setAutoCommit(true); return c;
    }
    private void execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setQueryTimeout(quyDinh.timeoutGiay()); bind(ps,args);
            boolean result=ps.execute();
            while(result || ps.getUpdateCount()!=-1) {
                if(result) { try(ResultSet r=ps.getResultSet()) { while(r.next()) { /* đọc hết kết quả quản trị */ } } }
                result=ps.getMoreResults();
            }
        }
    }
    private void khoaMaster(Connection c,String db) throws SQLException {
        Integer code=one(c,"DECLARE @r int; EXEC @r=sys.sp_getapplock @Resource=?,@LockMode='Exclusive',@LockOwner='Session',@LockTimeout=0; SELECT @r;",
                r -> r.getInt(1),"BClinic:backup:"+db);
        can(code!=null && code>=0,"Database đang được sao lưu/khôi phục bởi tiến trình khác.");
    }
    public ThongTinBanSaoLuu docThongTin(String file) {
        database(); String path=validateDuongDan(file);
        try(Connection c=master()) { return header(c,path); }
        catch(SQLException e) { throw new DataAccessException("Không đọc được bản sao lưu.",e); }
    }
    private ThongTinBanSaoLuu header(Connection c,String path) throws SQLException {
        ThongTinBanSaoLuu info=one(c,"RESTORE HEADERONLY FROM DISK=?",r -> new ThongTinBanSaoLuu(
                r.getString("DatabaseName"),r.getObject("BackupFinishDate",LocalDateTime.class),r.getBigDecimal("BackupSize"),r.getInt("Position"),r.getInt("BackupType")),path);
        can(info!=null && info.loai()==1 && info.viTri()==1,"File không phải bản full backup được hỗ trợ."); return info;
    }
    public boolean verify(String file) {
        String db=database(),path=validateDuongDan(file);
        try(Connection c=master()) {
            can(db.equals(header(c,path).database()),"Bản sao lưu thuộc database khác.");
            execute(c,"RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM",path); return true;
        } catch(SQLException e) { throw new DataAccessException("Bản sao lưu không hợp lệ hoặc không đọc được.",e); }
    }
    public String backup(LoaiSaoLuu loai) {
        Objects.requireNonNull(loai); String db=database();
        can(LOCK.tryLock(),"Đang có thao tác sao lưu/khôi phục.");
        try(Connection c=master()) {
            khoaMaster(c,db);
            String file=backup(c,db,loai);
            lichSu("SAO_LUU_"+loai,file,"THANH_CONG"); return file;
        } catch(SQLException e) {
            ghiLoi("SAO_LUU_"+loai,"",e); throw new DataAccessException("Không thể sao lưu database; kiểm tra quyền và đường dẫn phía SQL Server.",e);
        } finally { LOCK.unlock(); }
    }
    private String backup(Connection c,String db,LoaiSaoLuu loai) throws SQLException {
        String dir=quyDinh.thuMucMayChu().replace('\\','/').replaceAll("/+$", "");
        String path=null;
        for(int n=0;n<120;n++) {
            String candidate=dir+"/"+db+"_"+loai+"_"+context.bayGio().plusSeconds(n).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))+".bak";
            Integer found=one(c,"EXEC master.dbo.xp_fileexist ?",r -> r.getInt(1),candidate);
            if(found==null || found==0) { path=validateDuongDan(candidate); break; }
        }
        can(path!=null,"Không cấp được tên file backup mới.");
        Integer engine=one(c,"SELECT CAST(SERVERPROPERTY('EngineEdition') AS INT)",r -> r.getInt(1));
        String compression=engine!=null && engine==4 ? "" : ", COMPRESSION";
        execute(c,"BACKUP DATABASE ["+db+"] TO DISK=? WITH COPY_ONLY,CHECKSUM,INIT"+compression+",STATS=10",path);
        execute(c,"RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM",path); return path;
    }
    /** Không gọi nếu chưa hiển thị thông tin file và nhận xác nhận lần hai từ UI. */
    public void restore(String file,String matKhauQuanLy,boolean xacNhanLanHai) {
        String db=database(),path=validateDuongDan(file);
        can(xacNhanLanHai,"Cần xác nhận lần hai trước khi khôi phục.");
        context.doc(c -> { TaiKhoan actor=quyen(c,VaiTro.QUANLY);
            String stored=one(c,"SELECT matKhau FROM TaiKhoan WHERE maTK=?",r -> r.getString(1),actor.getMaTK());
            can(PasswordHasher.xacMinh(matKhauQuanLy,stored),"Mật khẩu quản lý không chính xác."); return null; });
        can(LOCK.tryLock(),"Đang có thao tác sao lưu/khôi phục.");
        boolean restored=false;
        try(Connection c=master()) {
            khoaMaster(c,db);
            can(db.equals(header(c,path).database()),"Bản sao lưu thuộc database khác.");
            execute(c,"RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM",path);
            String before=backup(c,db,LoaiSaoLuu.PRE_RESTORE);
            lichSu("SAO_LUU_PRE_RESTORE",before,"THANH_CONG");
            try(AutoCloseable ignored=baoTri.batDau()) {
                SQLException failure=null;
                try {
                    execute(c,"ALTER DATABASE ["+db+"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                    execute(c,"RESTORE DATABASE ["+db+"] FROM DISK=? WITH REPLACE,CHECKSUM,RECOVERY,STATS=10",path);
                    restored=true;
                } catch(SQLException e) { failure=e; throw e; }
                finally {
                    try { execute(c,"ALTER DATABASE ["+db+"] SET MULTI_USER"); }
                    catch(SQLException multi) { if(failure!=null) { failure.addSuppressed(multi); } else { throw multi; } }
                }
            }
            lichSu("KHOI_PHUC",path,"THANH_CONG");
        } catch(Exception e) {
            ghiLoi("KHOI_PHUC",path,e);
            if(e instanceof BusinessException business) { throw business; }
            throw new DataAccessException("Khôi phục thất bại. Kiểm tra bản PRE_RESTORE và trạng thái database trước khi tiếp tục.",e);
        } finally {
            try { if(restored) { dangXuat.run(); } } finally { LOCK.unlock(); }
        }
    }
    private void lichSu(String action,String file,String result) {
        TaiKhoan actor=context.nguoiDung(); String ma=actor==null?"":actor.getMaTK();
        Path history=quyDinh.fileLichSu().toAbsolutePath().normalize();
        try {
            Files.createDirectories(history.getParent());
            String row=csv(context.bayGio().toString())+","+csv(action)+","+csv(file)+","+csv(result)+","+csv(ma)+System.lineSeparator();
            Files.writeString(history,row,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch(IOException e) { throw new DataAccessException("Thao tác đã chạy nhưng không ghi được file lịch sử sao lưu.",e); }
        // Sau restore tai khoan co the khong con; van giu lich su ngoai DB.
        context.transaction(c -> {
            if(actor!=null && exists(c,"SELECT 1 FROM TaiKhoan WHERE maTK=? AND tenTK=?",ma,actor.getTenTK())) {
                nhatKy(c,action,result+" / "+file);
            }
            return null;
        });
    }
    private void ghiLoi(String action,String file,Exception original) {
        try { lichSu(action,file,"THAT_BAI"); } catch(RuntimeException log) { original.addSuppressed(log); }
    }
    private static String csv(String value) { return "\""+value.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\""; }
    /** Trả dòng CSV đã escape để UI/export dùng nguyên bản, gồm cả các lần trước restore. */
    public List<String> layLichSu() {
        database();
        try { return Files.exists(quyDinh.fileLichSu()) ? Files.readAllLines(quyDinh.fileLichSu(),StandardCharsets.UTF_8) : List.of(); }
        catch(IOException e) { throw new DataAccessException("Không đọc được lịch sử sao lưu.",e); }
    }
}
