package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.LoaiSaoLuu;
import com.quanlynhakhoa.util.*;
import com.quanlynhakhoa.dao.support.MaSinh;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/** SQL Server backup/restore qua master, không chạy trong transaction. */
public final class BackupDAO {
    @FunctionalInterface public interface KetNoi { Connection mo() throws SQLException; }
    @FunctionalInterface public interface BaoTri { AutoCloseable batDau() throws Exception; }
    public record ThongTinBanSaoLuu(String database,LocalDateTime ngay,long kichThuoc,int loai,int viTri) { }
    public record KetQuaKhoiPhuc(String banAnToan,String canhBao) { }
    private static final ReentrantLock LOCK=new ReentrantLock();
    private static final java.util.regex.Pattern INVALID_PATH=java.util.regex.Pattern.compile("[';*?<>|\\[\\]\\r\\n]");
    private final String db;
    private final KetNoi data,master;
    private final BaoTri baoTri;
    private final AppConfig config;
    private final LichSuSaoLuuFile history;
    public BackupDAO(){this(DatabaseConnection.getInstance().getDatabaseName(),DatabaseConnection.getInstance()::getConnection,
            DatabaseConnection.getInstance()::getMasterConnection,DatabaseConnection.getInstance()::tamDungDeKhoiPhuc,
            new AppConfig(),new LichSuSaoLuuFile(DuongDanUngDung.lichSuSaoLuu()));}
    public BackupDAO(String db,KetNoi data,KetNoi master,BaoTri baoTri,AppConfig config,LichSuSaoLuuFile history){
        if(db==null||!db.matches("[A-Za-z][A-Za-z0-9_]{0,100}"))throw new IllegalArgumentException("Tên database không hợp lệ.");
        this.db=db;this.data=data;this.master=master;this.baoTri=baoTri;this.config=config;this.history=history;
    }
    public static boolean dangChay(){return LOCK.isLocked();}
    private static LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
    private String actor(){var tk=SessionManager.getInstance().getCurrentUser();if(tk==null)throw new IllegalStateException("Vui lòng đăng nhập quản lý.");return tk.getMaTK();}
    private void quyen(String tk,char[] password)throws Exception{
        try(var c=data.mo();var p=c.prepareStatement("SELECT matKhau FROM TaiKhoan WHERE maTK=? AND vaiTro='QUANLY' AND trangThai=1")){
            p.setString(1,tk);try(var r=p.executeQuery()){if(!r.next())throw new IllegalStateException("Chỉ quản lý hoạt động được sao lưu/khôi phục.");
                if(password!=null&&!PasswordHasher.xacMinh(new String(password),r.getString(1)))throw new IllegalArgumentException("Mật khẩu quản lý không đúng.");}
        }
    }
    public boolean xacThuc(char[] password)throws Exception{if(password==null)return false;try{quyen(actor(),password);return true;}catch(IllegalArgumentException e){return false;}finally{Arrays.fill(password,'\0');}}
    public String thuMuc()throws Exception{
        String dir=config.thuMuc();if(!dir.isBlank())return dir;
        try(var c=master.mo();var s=c.createStatement();var r=s.executeQuery("SELECT CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS nvarchar(4000))")){
            if(!r.next()||r.getString(1)==null)throw new IllegalStateException("Hãy cấu hình thư mục sao lưu phía SQL Server.");return r.getString(1);
        }
    }
    public static String kiemTraDuongDan(String dir,String file){
        if(dir==null||file==null||dir.isBlank()||INVALID_PATH.matcher(dir).find()||INVALID_PATH.matcher(file).find()||dir.contains("..")||file.contains("..")||dir.indexOf('\0')>=0||file.indexOf('\0')>=0)throw new IllegalArgumentException("Đường dẫn sao lưu không hợp lệ.");
        String root=dir.replace('\\','/').replaceAll("/+$","");String path=file.replace('\\','/');
        boolean windows=root.matches("(?i)^[a-z]:/.*")||root.startsWith("//");
        String compare=windows?path.toLowerCase(Locale.ROOT):path;String base=windows?root.toLowerCase(Locale.ROOT):root;
        if(!compare.startsWith(base+"/")||path.substring(root.length()+1).contains("/")||!path.toLowerCase(Locale.ROOT).endsWith(".bak"))throw new IllegalArgumentException("Chỉ chọn file .bak trực tiếp trong thư mục sao lưu đã cấu hình.");
        return file;
    }
    private String fileMoi(LoaiSaoLuu loai)throws Exception{
        String dir=thuMuc();return kiemTraDuongDan(dir,dir.replaceAll("[\\\\/]+$","")+"/BClinic_"+loai+"_"+now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))+"_"+UUID.randomUUID().toString().substring(0,8)+".bak");
    }
    private void command(Connection c,String sql,String file)throws SQLException{
        try(var p=c.prepareStatement(sql)){p.setQueryTimeout(180);if(file!=null)p.setString(1,file);boolean result=p.execute();while(true){if(result){try(var r=p.getResultSet()){while(r.next()){ /* tiêu thụ kết quả SQL */ }}}else if(p.getUpdateCount()==-1)break;result=p.getMoreResults();}}
    }
    private void khoaMayChu(Connection c)throws SQLException{
        try(var p=c.prepareStatement("DECLARE @r int; EXEC @r=sys.sp_getapplock @Resource=?,@LockMode='Exclusive',@LockOwner='Session',@LockTimeout=0; SELECT @r")){
            p.setString(1,"BClinic:backup:"+db);try(var r=p.executeQuery()){if(!r.next()||r.getInt(1)<0)throw new SQLException("Một phiên khác đang sao lưu/khôi phục.");}
        }
    }
    private ThongTinBanSaoLuu header(Connection c,String file)throws Exception{
        kiemTraDuongDan(thuMuc(),file);
        try(var p=c.prepareStatement("RESTORE HEADERONLY FROM DISK=?")){p.setString(1,file);p.setQueryTimeout(180);try(var r=p.executeQuery()){
            if(!r.next())throw new IllegalArgumentException("File sao lưu không hợp lệ.");
            var info=new ThongTinBanSaoLuu(r.getString("DatabaseName"),r.getTimestamp("BackupFinishDate").toLocalDateTime(),r.getLong("CompressedBackupSize"),r.getInt("BackupType"),r.getInt("Position"));
            if(!db.equals(info.database())||info.loai()!=1||info.viTri()!=1||r.next())throw new IllegalArgumentException("Cần một bản Full backup của đúng database "+db+".");return info;
        }}
    }
    public ThongTinBanSaoLuu docThongTin(String file)throws Exception{
        quyen(actor(),null);try(var c=master.mo()){var result=header(c,file);command(c,"RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM",file);return result;}
    }
    public List<LichSuSaoLuu> lichSu()throws Exception{quyen(actor(),null);return history.doc();}
    public void luuCauHinh(String dir,AppConfig.LichTuDong lich)throws Exception{quyen(actor(),null);if(!actor().equals(lich.maTK()))throw new IllegalArgumentException("Sai người cấu hình.");config.luu(dir,lich);}
    private void ghi(String loai,String file,long bytes,String ketQua,String tk)throws Exception{history.ghi(new LichSuSaoLuu(now(),loai,file,bytes,ketQua,tk));}
    private String audit(String tk,String action,String detail){
        try(var c=data.mo()){
            c.setAutoCommit(false);
            try{try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM NhatKyHeThong WITH(UPDLOCK,HOLDLOCK)")){r.next();}
                String actor=tk;try(var p=c.prepareStatement("SELECT maTK FROM TaiKhoan WHERE maTK=?")){p.setString(1,tk);try(var r=p.executeQuery()){if(!r.next()){try(var s=c.createStatement();var row=s.executeQuery("SELECT TOP 1 maTK FROM TaiKhoan WHERE vaiTro='QUANLY' ORDER BY maTK")){if(!row.next())throw new SQLException("Không có tài khoản ghi nhật ký");actor=row.getString(1);}}}}
                String code=MaSinh.maMoi(c,MaSinh.Bang.NHAT_KY,null);
                try(var p=c.prepareStatement("INSERT NhatKyHeThong VALUES(?,?,?,?,?)")){p.setString(1,code);p.setString(2,actor);p.setTimestamp(3,Timestamp.valueOf(now()));p.setString(4,action);p.setString(5,"Người thực hiện "+tk+": "+detail);p.executeUpdate();}c.commit();return "";
            }catch(Exception e){c.rollback();throw e;}
        }catch(Exception e){java.util.logging.Logger.getLogger(BackupDAO.class.getName()).log(java.util.logging.Level.WARNING,"Không ghi được nhật ký DB",e);return "Nhật ký DB chưa ghi được; xem lịch sử ngoài DB.";}
    }
    private String backup(Connection c,LoaiSaoLuu loai,String tk)throws Exception{
        String file=fileMoi(loai);
        try{String sql="DECLARE @path nvarchar(4000)=?; IF CONVERT(int,SERVERPROPERTY('EngineEdition'))=4 "
                    +"BACKUP DATABASE ["+db+"] TO DISK=@path WITH COPY_ONLY,CHECKSUM,INIT,STATS=10; ELSE "
                    +"BACKUP DATABASE ["+db+"] TO DISK=@path WITH COPY_ONLY,CHECKSUM,INIT,COMPRESSION,STATS=10;";
            command(c,sql,file);
            command(c,"RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM",file);var h=header(c,file);ghi(loai.name(),file,h.kichThuoc(),"THANH_CONG",tk);audit(tk,"SAO_LUU",loai+" thành công: "+file);return file;
        }catch(Exception e){try{ghi(loai.name(),file,0,"THAT_BAI",tk);audit(tk,"SAO_LUU","Thất bại: "+file);}catch(Exception log){e.addSuppressed(log);}throw new IllegalStateException("Sao lưu không hoàn tất. Kiểm tra quyền SQL Server và thư mục máy chủ. "+e.getMessage(),e);}
    }
    public String backup(LoaiSaoLuu loai)throws Exception{if(loai!=LoaiSaoLuu.FULL)throw new IllegalArgumentException("Chỉ sao lưu thủ công FULL.");return runBackup(loai,actor());}
    public String backupTuDong(String tk)throws Exception{return runBackup(LoaiSaoLuu.AUTO,tk);}
    private String runBackup(LoaiSaoLuu loai,String tk)throws Exception{
        if(!LOCK.tryLock())throw new IllegalStateException("Đang có thao tác sao lưu/khôi phục.");
        try{quyen(tk,null);try(var c=master.mo()){c.setAutoCommit(true);khoaMayChu(c);return backup(c,loai,tk);}}finally{LOCK.unlock();}
    }
    public KetQuaKhoiPhuc restore(String file,char[] password)throws Exception{
        if(password==null||password.length==0)throw new IllegalArgumentException("Cần nhập lại mật khẩu quản lý.");
        String tk=actor();
        if(!LOCK.tryLock()){Arrays.fill(password,'\0');throw new IllegalStateException("Đang có thao tác sao lưu/khôi phục.");}
        String pre="";boolean restored=false;
        try{
            quyen(tk,password);
            try(var c=master.mo()){
                c.setAutoCommit(true);khoaMayChu(c);header(c,file);command(c,"RESTORE VERIFYONLY FROM DISK=? WITH CHECKSUM",file);
                // Tạo bản an toàn thành công rồi mới đóng pool/thay DB.
                pre=backup(c,LoaiSaoLuu.PRE_RESTORE,tk);
                try(var pause=baoTri.batDau()){
                    Exception failure=null;
                    try{command(c,"ALTER DATABASE ["+db+"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE",null);
                        command(c,"RESTORE DATABASE ["+db+"] FROM DISK=? WITH REPLACE,CHECKSUM,RECOVERY,STATS=10",file);restored=true;
                    }catch(Exception e){failure=e;throw e;}
                    finally{try{command(c,"ALTER DATABASE ["+db+"] SET MULTI_USER WITH ROLLBACK IMMEDIATE",null);}catch(Exception e){if(failure!=null)failure.addSuppressed(e);else throw e;}}
                }
            }
            String warning=audit(tk,"KHOI_PHUC","Thành công từ "+file+"; bản an toàn "+pre);
            try{ghi("RESTORE",file,0,"THANH_CONG",tk);}catch(Exception e){warning+=" Không ghi được file lịch sử sau khôi phục.";}
            return new KetQuaKhoiPhuc(pre,warning);
        }catch(Exception e){
            try{ghi("RESTORE",file,0,restored?"DA_RESTORE_CAN_KIEM_TRA":"THAT_BAI",tk);audit(tk,"KHOI_PHUC","Lỗi; bản an toàn: "+pre);}catch(Exception log){e.addSuppressed(log);}
            if(restored)return new KetQuaKhoiPhuc(pre,"DB đã khôi phục nhưng cần kiểm tra kết nối/MULTI_USER: "+e.getMessage());
            throw new IllegalStateException("Khôi phục không thành công. Bản dữ liệu an toàn: "+(pre.isEmpty()?"chưa tạo (DB chưa bị thay)":pre)+". "+e.getMessage(),e);
        }finally{Arrays.fill(password,'\0');LOCK.unlock();}
    }
    /** Xóa đúng file AUTO đã ghi nhận bằng quyền SQL Server, không quét thư mục. */
    public void donAuto(int keep)throws Exception{
        if(keep<1)throw new IllegalArgumentException("Số bản giữ lại phải dương.");
        String tk=config.lich().maTK();quyen(tk.isBlank()?actor():tk,null);
        if(!LOCK.tryLock())return;
        try(var server=master.mo()){server.setAutoCommit(true);khoaMayChu(server);String dir=thuMuc();
            var files=history.doc().stream().filter(r->r.getLoai().equals("AUTO")&&r.getKetQua().equals("THANH_CONG"))
                    .map(LichSuSaoLuu::getDuongDan).distinct().filter(f->{try{kiemTraDuongDan(dir,f);return true;}catch(Exception e){return false;}}).toList();
            for(int i=keep;i<files.size();i++){
                String file=files.get(i);String name=file.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);
                if(!name.matches("BClinic_AUTO_\\d{8}_\\d{6}_\\d{3}_[a-f0-9]{8}\\.bak"))continue;
                command(server,"EXEC master.sys.xp_delete_files ?",file);
            }
        }finally{LOCK.unlock();}
    }
}
