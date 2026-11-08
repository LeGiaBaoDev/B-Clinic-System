package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.AppConfig;
import com.quanlynhakhoa.entity.LichSuSaoLuu;
import com.quanlynhakhoa.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class BackupRulesTest {
    @TempDir Path temp;
    @Test void historyPathDoesNotDependOnIdeWorkingDirectory(){
        Path expected=com.quanlynhakhoa.config.DuongDanUngDung.lichSuSaoLuu();
        String previous=System.getProperty("user.dir");
        try{System.setProperty("user.dir",temp.toString());assertEquals(expected,com.quanlynhakhoa.config.DuongDanUngDung.lichSuSaoLuu());assertTrue(expected.isAbsolute());}
        finally{System.setProperty("user.dir",previous);}
    }
    @Test void relocatedHistoryUsesConfiguredFileAndSurvivesScheduleSave()throws Exception{
        Path settings=temp.resolve("relocated.properties"),history=temp.resolve("new-folder/backup-history.csv");
        var p=new java.util.Properties();p.setProperty("backup.history.file",history.toString());
        try(var out=Files.newOutputStream(settings)){p.store(out,"test");}
        var config=new AppConfig(settings);assertEquals(history,config.fileLichSu());
        config.luu("D:/Clinic/backups",new AppConfig.LichTuDong(false,LocalTime.of(22,0),false,14,"TK001"));
        assertEquals(history,new AppConfig(settings).fileLichSu());
    }
    @Test void pathRejectsTraversalSiblingsAndSqlTokens(){
        assertEquals("C:\\Backup\\safe.bak",BackupDAO.kiemTraDuongDan("C:\\Backup","C:\\Backup\\safe.bak"));
        for(String file:new String[]{"C:/Backup2/x.bak","C:/Backup/../x.bak","C:/Backup/a/x.bak","C:/Backup/x.sql","C:/Backup/a';DROP.bak","C:/Backup/x.bak\n","C:/Backup/a\n\nb.bak","C:/Backup/*.bak"})
            assertThrows(IllegalArgumentException.class,()->BackupDAO.kiemTraDuongDan("C:/Backup",file));
        assertThrows(IllegalArgumentException.class,()->BackupDAO.kiemTraDuongDan("/server/Backup","/server/backup/x.bak"));
    }
    @Test void historySurvivesReloadAndCsvSpecialCharacters()throws Exception{
        Path file=temp.resolve("history.csv");var store=new LichSuSaoLuuFile(file);
        store.ghi(new LichSuSaoLuu(LocalDateTime.of(2026,11,6,13,0),"FULL","C:/Bản sao, mới/a\"b.bak",100,"THANH_CONG","TK001"));
        var row=new LichSuSaoLuuFile(file).doc().getFirst();assertEquals("C:/Bản sao, mới/a\"b.bak",row.getDuongDan());assertEquals(100,row.getKichThuoc());
        Files.writeString(file,"broken");assertThrows(java.io.IOException.class,store::doc);
    }
    @Test void settingsRoundTripAndNextWeeklyRun()throws Exception{
        var config=new AppConfig(temp.resolve("app.properties"));assertFalse(config.lich().bat());
        var plan=new AppConfig.LichTuDong(true,LocalTime.of(22,0),true,14,"TK001");config.luu("C:/Backup",plan);
        assertEquals(plan,new AppConfig(temp.resolve("app.properties")).lich());
        var now=ZonedDateTime.of(2026,11,6,23,0,0,0,ZoneId.of("Asia/Ho_Chi_Minh"));
        assertEquals(LocalDate.of(2026,11,9),BackupScheduler.lanTiepTheo(now,plan).toLocalDate());
        assertThrows(IllegalArgumentException.class,()->new AppConfig.LichTuDong(true,LocalTime.NOON,false,0,"TK001"));
    }
}
