package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.config.AppConfig;
import com.quanlynhakhoa.dao.BackupDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.LoaiSaoLuu;
import com.quanlynhakhoa.util.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Test tạo DB riêng; không restore/drop database ứng dụng. */
@EnabledIfSystemProperty(named="bclinic.backup.integration",matches="true")
class BackupRestoreIntegrationTest {
    @TempDir Path temp;
    @Test void backupRestoreSafetyAndRecovery()throws Exception{
        var p=new Properties();try(var in=Files.newInputStream(Path.of("src/main/resources/database.properties"))){p.load(in);}
        String db="BClinic_BackupTest_"+UUID.randomUUID().toString().replace("-","");
        assertTrue(db.matches("BClinic_BackupTest_[a-f0-9]{32}"));
        String base="jdbc:sqlserver://"+p.getProperty("db.host","localhost")+":"+p.getProperty("db.port","1433")+";encrypt=true;trustServerCertificate=true;databaseName=";
        var paths=new ArrayList<String>();var pauseCount=new AtomicInteger();
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var hold=new java.util.concurrent.atomic.AtomicBoolean();
        try(var admin=DriverManager.getConnection(base+"master",p.getProperty("db.username"),p.getProperty("db.password"))){
            try(var s=admin.createStatement()){s.executeUpdate("CREATE DATABASE ["+db+"]");}
            try{
                try(var c=DriverManager.getConnection(base+db,p.getProperty("db.username"),p.getProperty("db.password"));var s=c.createStatement()){
                    s.executeUpdate("CREATE TABLE TaiKhoan(maTK varchar(15) PRIMARY KEY,matKhau varchar(255),vaiTro varchar(10),trangThai bit); CREATE TABLE NhatKyHeThong(maNhatKy varchar(15) PRIMARY KEY,maTK varchar(15),thoiDiem datetime2,hanhDong nvarchar(100),noiDungThayDoi nvarchar(max)); CREATE TABLE Probe(id int PRIMARY KEY)");
                    try(var insert=c.prepareStatement("INSERT TaiKhoan VALUES('TK001',?,'QUANLY',1),('TK002',?,'LETAN',1)")){insert.setString(1,PasswordHasher.bam("BackupTest123!"));insert.setString(2,PasswordHasher.bam("BackupTest123!"));insert.executeUpdate();}
                }
                String dir;try(var s=admin.createStatement();var r=s.executeQuery("SELECT CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS nvarchar(4000))")){r.next();dir=r.getString(1);}
                var config=new AppConfig(temp.resolve("app.properties"));config.luu(dir,new AppConfig.LichTuDong(false,LocalTime.of(22,0),false,1,"TK001"));
                var history=new LichSuSaoLuuFile(temp.resolve("history.csv"));
                var dao=new BackupDAO(db,()->DriverManager.getConnection(base+db,p.getProperty("db.username"),p.getProperty("db.password")),()->DriverManager.getConnection(base+"master",p.getProperty("db.username"),p.getProperty("db.password")),()->{pauseCount.incrementAndGet();if(hold.get()){entered.countDown();if(!release.await(20,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Test barrier timeout");}return ()->{};},config,history);
                var tk=new TaiKhoan();tk.setMaTK("TK001");SessionManager.getInstance().login(tk);
                String first=dao.backup(LoaiSaoLuu.FULL);paths.add(first);assertEquals(db,dao.docThongTin(first).database());
                config.luu(dir+"/missing_"+UUID.randomUUID(),new AppConfig.LichTuDong(false,LocalTime.of(22,0),false,1,"TK001"));
                assertThrows(Exception.class,()->dao.backup(LoaiSaoLuu.FULL));
                config.luu(dir,new AppConfig.LichTuDong(false,LocalTime.of(22,0),false,1,"TK001"));
                for(int i=0;i<3;i++)assertFalse(dao.xacThuc("wrong".toCharArray()));
                assertThrows(Exception.class,()->dao.restore(first,"wrong".toCharArray()));assertEquals(0,pauseCount.get());
                tk.setMaTK("TK002");assertThrows(Exception.class,()->dao.backup(LoaiSaoLuu.FULL));tk.setMaTK("TK001");
                assertThrows(Exception.class,()->dao.docThongTin(dir+"/invalid.txt"));
                String invalid=dir+"/missing_"+UUID.randomUUID()+".bak";assertThrows(Exception.class,()->dao.restore(invalid,"BackupTest123!".toCharArray()));assertEquals(0,pauseCount.get());
                try(var c=DriverManager.getConnection(base+db,p.getProperty("db.username"),p.getProperty("db.password"));var s=c.createStatement()){s.executeUpdate("INSERT Probe VALUES(1)");}
                BackupDAO.KetQuaKhoiPhuc restored;
                hold.set(true);
                try(var executor=java.util.concurrent.Executors.newSingleThreadExecutor()){
                    var running=executor.submit(()->dao.restore(first,"BackupTest123!".toCharArray()));
                    try{assertTrue(entered.await(20,java.util.concurrent.TimeUnit.SECONDS));assertThrows(IllegalStateException.class,()->dao.backup(LoaiSaoLuu.FULL));}
                    finally{release.countDown();}
                    restored=running.get(60,java.util.concurrent.TimeUnit.SECONDS);
                }
                hold.set(false);paths.add(restored.banAnToan());assertEquals(1,pauseCount.get());
                try(var c=DriverManager.getConnection(base+db,p.getProperty("db.username"),p.getProperty("db.password"));var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM Probe")){r.next();assertEquals(0,r.getInt(1));}
                var recovered=dao.restore(restored.banAnToan(),"BackupTest123!".toCharArray());paths.add(recovered.banAnToan());
                try(var c=DriverManager.getConnection(base+db,p.getProperty("db.username"),p.getProperty("db.password"));var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM Probe")){r.next();assertEquals(1,r.getInt(1));}
                try(var s=admin.createStatement();var r=s.executeQuery("SELECT user_access_desc FROM sys.databases WHERE name='"+db+"'")){r.next();assertEquals("MULTI_USER",r.getString(1));}
                paths.add(dao.backupTuDong("TK001"));paths.add(dao.backupTuDong("TK001"));dao.donAuto(1);
                assertEquals(db,dao.docThongTin(first).database());assertEquals(db,dao.docThongTin(restored.banAnToan()).database());
                assertThrows(Exception.class,()->dao.docThongTin(paths.get(paths.size()-2)));assertEquals(db,dao.docThongTin(paths.getLast()).database());
                assertTrue(history.doc().stream().anyMatch(row->row.getLoai().equals("RESTORE")&&row.getKetQua().equals("THANH_CONG")));
                // T7: chạy bằng timer thật ở phút kế tiếp, không ghi cấu hình ứng dụng chính.
                var zone=java.time.ZoneId.of("Asia/Ho_Chi_Minh");
                var next=java.time.LocalTime.now(zone).plusMinutes(1).withSecond(0).withNano(0);
                config.luu(dir,new AppConfig.LichTuDong(true,next,false,1,"TK001"));
                long autoBefore=history.doc().stream().filter(row->row.getLoai().equals("AUTO")).count();
                var scheduler=new BackupScheduler(config,()->dao,java.time.Clock.system(zone));
                try{
                    scheduler.khoiDongLai();long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(75);
                    while(System.nanoTime()<deadline&&history.doc().stream().filter(row->row.getLoai().equals("AUTO")).count()==autoBefore)Thread.sleep(300);
                    assertEquals(autoBefore+1,history.doc().stream().filter(row->row.getLoai().equals("AUTO")).count());
                    // Đợi job dọn bản cũ nhả khóa trước bước cleanup.
                    while(scheduler.dangChay()&&System.nanoTime()<deadline)Thread.sleep(100);
                    for(var row:history.doc())if(row.getLoai().equals("AUTO")&&!paths.contains(row.getDuongDan()))paths.add(row.getDuongDan());
                }finally{scheduler.dung();}
            }finally{
                SessionManager.getInstance().logout();
                try(var s=admin.createStatement()){s.executeUpdate("ALTER DATABASE ["+db+"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE ["+db+"]");}
                // Xóa đúng các file do lượt test này tạo; không quét hoặc xóa thư mục backup.
                for(String file:paths){Path path=Path.of(file);if(path.getFileName().toString().startsWith("BClinic_")&&path.toString().endsWith(".bak")){
                    try(var delete=admin.prepareStatement("EXEC master.sys.xp_delete_files ?")){delete.setString(1,file);delete.execute();}
                }}
            }
        }
    }
}
