package com.quanlynhakhoa.config;

import java.nio.file.*;
import java.io.*;
import java.time.LocalTime;
import java.util.Properties;

/** Cấu hình sao lưu ngoài JAR; không chứa mật khẩu. */
public final class AppConfig {
    public record LichTuDong(boolean bat, LocalTime gio, boolean hangTuan, int soBanGiu, String maTK) {
        public LichTuDong {
            if (gio == null || soBanGiu < 1 || soBanGiu > 365 || (bat && (maTK == null || !maTK.matches("TK\\d{3}"))))
                throw new IllegalArgumentException("Lịch sao lưu không hợp lệ (giữ 1–365 bản, cần người quản lý).");
        }
    }
    private final Path file;
    public AppConfig() { this(Path.of(System.getProperty("bclinic.app.config", DuongDanUngDung.thuMuc().resolve("app.properties").toString()))); }
    public AppConfig(Path file) { this.file=file; }
    private Properties doc() {
        Properties p=new Properties();
        if(Files.exists(file))try(var in=Files.newInputStream(file)){p.load(in);}catch(IOException e){throw new IllegalStateException("Không đọc được app.properties",e);}
        return p;
    }
    public synchronized String thuMuc() { return doc().getProperty("backup.dir", ""); }
    public synchronized Path fileLichSu() {
        String value=doc().getProperty("backup.history.file", "backups/backup-history.csv");
        Path path=Path.of(value);
        return (path.isAbsolute()?path:DuongDanUngDung.thuMuc().resolve(path)).toAbsolutePath().normalize();
    }
    public synchronized LichTuDong lich() {
        var p=doc();
        return new LichTuDong(Boolean.parseBoolean(p.getProperty("backup.auto.enabled","false")), LocalTime.parse(p.getProperty("backup.auto.time","22:00")),
                p.getProperty("backup.auto.period","DAILY").equals("WEEKLY"),Integer.parseInt(p.getProperty("backup.keep","14")),p.getProperty("backup.auto.actor",""));
    }
    public synchronized void luu(String dir,LichTuDong lich) {
        new QuyDinhSaoLuu(dir,fileLichSu(),180);
        Properties p=doc();p.setProperty("backup.dir",dir);p.setProperty("backup.auto.enabled",Boolean.toString(lich.bat()));p.setProperty("backup.auto.time",lich.gio().toString());
        p.setProperty("backup.auto.period",lich.hangTuan()?"WEEKLY":"DAILY");p.setProperty("backup.keep",Integer.toString(lich.soBanGiu()));p.setProperty("backup.auto.actor",lich.maTK());
        try { Path target=file.toAbsolutePath();Files.createDirectories(target.getParent());Path temp=Files.createTempFile(target.getParent(),"app-",".tmp");
            try{try(var out=Files.newOutputStream(temp)){p.store(out,"BClinic backup settings");}Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            finally{Files.deleteIfExists(temp);}
        }catch(IOException e){throw new IllegalStateException("Không lưu được cấu hình sao lưu",e);}
    }
}
