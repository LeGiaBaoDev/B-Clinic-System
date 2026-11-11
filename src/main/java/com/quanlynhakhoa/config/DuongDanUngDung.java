package com.quanlynhakhoa.config;

import java.nio.file.*;

/** Thư mục dữ liệu dựa trên vị trí chương trình, không phụ thuộc thư mục mở IDE. */
public final class DuongDanUngDung {
    private DuongDanUngDung() { }
    public static Path thuMuc() {
        String override=System.getProperty("bclinic.home");
        if(override!=null&&!override.isBlank())return Path.of(override).toAbsolutePath().normalize();
        try {
            Path location=Path.of(DuongDanUngDung.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path start=Files.isDirectory(location)?location:location.getParent();
            // target/classes, IDE output và JAR trong dist đều dùng chung thư mục repo.
            for(Path p=start;p!=null;p=p.getParent())if(Files.isRegularFile(p.resolve("pom.xml")))return p;
            return start;
        } catch(Exception e) { throw new IllegalStateException("Không xác định được thư mục dữ liệu ứng dụng.",e); }
    }
    public static Path lichSuSaoLuu(){return new AppConfig().fileLichSu();}
}
