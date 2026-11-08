package com.quanlynhakhoa.util;

import com.quanlynhakhoa.config.AppConfig;
import com.quanlynhakhoa.dao.BackupDAO;
import java.time.*;
import java.util.concurrent.*;
import java.util.logging.*;

/** Lịch chạy khi ứng dụng mở. Tác vụ được quản lý đã cấu hình cấp quyền, kiểm lại mỗi lần. */
public final class BackupScheduler {
    private static final BackupScheduler INSTANCE=new BackupScheduler();
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"BClinic-backup");t.setDaemon(true);return t;});
    private ScheduledFuture<?> task;
    private long generation;
    private volatile boolean jobDangChay;
    private final AppConfig config;
    private final java.util.function.Supplier<BackupDAO> daoFactory;
    private final Clock clock;
    private BackupScheduler(){this(new AppConfig(),BackupDAO::new,Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));}
    public BackupScheduler(AppConfig config,java.util.function.Supplier<BackupDAO> daoFactory,Clock clock){this.config=config;this.daoFactory=daoFactory;this.clock=clock;Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));}
    public static BackupScheduler getInstance(){return INSTANCE;}
    public static ZonedDateTime lanTiepTheo(ZonedDateTime now,AppConfig.LichTuDong lich){
        ZonedDateTime next=now.toLocalDate().atTime(lich.gio()).atZone(now.getZone());
        if(lich.hangTuan())next=next.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if(!next.isAfter(now))next=next.plusDays(lich.hangTuan()?7:1);return next;
    }
    public synchronized void khoiDongLai(){
        generation++;if(task!=null)task.cancel(false);
        try{var lich=config.lich();if(lich.bat())datLich(generation,lich);}
        catch(Exception e){Logger.getLogger(getClass().getName()).log(Level.WARNING,"Chưa bật được lịch sao lưu",e);}
    }
    private synchronized void datLich(long version,AppConfig.LichTuDong lich){
        if(version!=generation||executor.isShutdown())return;
        var now=ZonedDateTime.now(clock);long delay=Duration.between(now,lanTiepTheo(now,lich)).toMillis();
        task=executor.schedule(()->{jobDangChay=true;try{var dao=daoFactory.get();dao.backupTuDong(lich.maTK());dao.donAuto(lich.soBanGiu());}
            catch(Exception e){Logger.getLogger(getClass().getName()).log(Level.WARNING,"Sao lưu tự động thất bại",e);}
            finally{jobDangChay=false;datLich(version,lich);}},Math.max(1,delay),TimeUnit.MILLISECONDS);
    }
    public synchronized void dung(){generation++;if(task!=null)task.cancel(false);}
    public boolean dangChay(){return jobDangChay;}
    public static void main(String[] args)throws Exception{
        var lich=new AppConfig().lich();if(!lich.bat())throw new IllegalStateException("Chưa bật lịch tự động trong ứng dụng.");
        try{var dao=new BackupDAO();System.out.println(dao.backupTuDong(lich.maTK()));dao.donAuto(lich.soBanGiu());}
        finally{com.quanlynhakhoa.config.DatabaseConnection.getInstance().getDataSource().close();}
    }
}
