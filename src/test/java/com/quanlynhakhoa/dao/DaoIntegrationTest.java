package com.quanlynhakhoa.dao;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.quanlynhakhoa.config.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

/** Chạy bằng -Dbclinic.it=true; tạo/xóa duy nhất BClinic_Test_<random>, không ghi BClinic_Dev. */
@EnabledIfSystemProperty(named="bclinic.it",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DaoIntegrationTest {
    private SQLServerDataSource ds,master;
    private DaoContext ctx;
    private final AtomicReference<TaiKhoan> actor=new AtomicReference<>();
    private TaiKhoan quanLy,nhaSi,leTan;
    private String db;
    private final LocalDate ngay=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final AtomicInteger unique=new AtomicInteger(100);
    private int soNgay=30;

    @BeforeAll void taoDatabaseRieng() throws Exception {
        Properties p=new Properties();
        try(var in=Files.newInputStream(Path.of("src/main/resources/database.properties"))) { p.load(in); }
        master=source(p,"master"); db="BClinic_Test_"+UUID.randomUUID().toString().replace("-","").substring(0,8);
        assertTrue(db.matches("BClinic_Test_[a-f0-9]{8}"));
        try(var c=master.getConnection();var s=c.prepareStatement("CREATE DATABASE ["+db+"]")) { s.execute(); }
        ds=source(p,db);
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        try(var c=ds.getConnection();var s=c.prepareStatement(Files.readString(Path.of("scripts/seed_demo.sql")).replace("N'BClinic_Dev'","N'"+db+"'"))) { s.execute(); }
        ctx=new DaoContext(ds,actor::get,Clock.fixed(ngay.atTime(8,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),ZoneId.of("Asia/Ho_Chi_Minh")));
        var tk=new TaiKhoanDAO(ctx);
        quanLy=tk.dangNhap("hieu.quanly","Clinic@123"); nhaSi=tk.dangNhap("hieu.nhasi","Clinic@123"); leTan=tk.dangNhap("hieu.letan","Clinic@123");
        actor.set(quanLy);
    }
    private SQLServerDataSource source(Properties p,String name) {
        SQLServerDataSource result=new SQLServerDataSource();
        result.setServerName(p.getProperty("db.host","localhost")); result.setPortNumber(Integer.parseInt(p.getProperty("db.port","1433")));
        result.setDatabaseName(name); result.setEncrypt("true"); result.setTrustServerCertificate(true);
        result.setIntegratedSecurity(Boolean.parseBoolean(p.getProperty("db.integratedSecurity","false")));
        result.setUser(p.getProperty("db.username")); result.setPassword(p.getProperty("db.password")); return result;
    }
    @BeforeEach void resetActor() { actor.set(quanLy); }
    @AfterAll void xoaDungDatabaseDaTao() throws Exception {
        if(db!=null && db.matches("BClinic_Test_[a-f0-9]{8}")) {
            try(var c=master.getConnection();var s=c.prepareStatement("ALTER DATABASE ["+db+"] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE ["+db+"];")) { s.execute(); }
        }
    }
    private long count(String table) throws Exception {
        Set<String> safe=Set.of("NhanVien","TaiKhoan","KhachHang","HoSoBenhAn","LichHen","PhieuKham","HoaDon","ChiTietPhieuKham");
        assertTrue(safe.contains(table));
        try(var c=ds.getConnection();var s=c.prepareStatement("SELECT COUNT_BIG(*) FROM "+table);var r=s.executeQuery()) { r.next(); return r.getLong(1); }
    }
    private KhachHang khach() {
        int n=unique.incrementAndGet();
        var kh=new KhachHang(null,"Khách kiểm thử "+n,LocalDate.of(1995,1,1),true,"091000"+String.format("%04d",n),"Địa chỉ test","test"+n+"@example.com");
        new KhachHangDAO(ctx).them(kh,"Tiền sử kiểm thử"); return kh;
    }
    private LocalDate caMoi() {
        LocalDate date=ngay.plusDays(soNgay++);
        var ll=new LichLamViec(null,date,CaTruc.CA_NGAY,LocalTime.of(7,0),LocalTime.of(17,30),TrangThaiLichLamViec.DA_PHAN_CONG,null,nhaSi.getNhanVien());
        new LichLamViecDAO(ctx).them(ll); return date;
    }
    private LichHen lich(KhachHang kh,LocalDate date,LocalTime time) {
        return new LichHen(null,date.atTime(time),TrangThaiLichHen.DA_DAT,null,null,kh,nhaSi.getNhanVien(),new DichVuDAO(ctx).layTheoMa("DV001"),null);
    }

    @Test @Order(1) void mapping_tatCaBang_vaThongKeKhongNhanDoiHoaDon() {
        assertEquals(3,new NhanVienDAO(ctx).layTatCa().size());
        assertEquals(3,new TaiKhoanDAO(ctx).layTatCa().size());
        assertEquals(3,new LoaiDichVuDAO(ctx).layTatCa().size());
        assertEquals(10,new DichVuDAO(ctx).layTatCa().size());
        assertEquals(20,new KhachHangDAO(ctx).layTatCa().size());
        assertEquals(20,new HoSoBenhAnDAO(ctx).layTatCa().size());
        assertFalse(new LichLamViecDAO(ctx).layTatCa().isEmpty());
        assertEquals(21,new LichHenDAO(ctx).layTatCa().size());
        assertEquals(14,new PhieuKhamDAO(ctx).layTatCa().size());
        assertEquals(12,new HoaDonDAO(ctx).layTatCa().size());
        String phieu=new PhieuKhamDAO(ctx).layTatCa().stream().filter(p -> p.getTrangThai()==TrangThaiPhieuKham.HOAN_TAT).findFirst().orElseThrow().getMaPK();
        assertFalse(new ChiTietPhieuKhamDAO(ctx).layTheoPhieuKham(phieu).isEmpty());
        assertEquals(6,new VoucherDAO(ctx).layTatCa().size());
        assertFalse(new NhatKyHeThongDAO(ctx).layTatCa().isEmpty());
        var stats=new ThongKeDoanhThuDAO(ctx).thongKe(ngay.minusDays(120),ngay);
        assertEquals(10,stats.stream().mapToLong(ThongKeDoanhThuDAO.TheoNgay::soHoaDon).sum());
        BigDecimal actual=stats.stream().map(ThongKeDoanhThuDAO.TheoNgay::doanhThu).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal independent=ctx.doc(c -> DaoSupport.one(c,"SELECT SUM(t.tong-hd.tongKhuyenMai) FROM HoaDon hd CROSS APPLY(SELECT SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)) tong FROM ChiTietPhieuKham WHERE maPK=hd.maPK) t WHERE hd.trangThai=1",r -> r.getBigDecimal(1)));
        assertEquals(0,actual.compareTo(independent));
        assertFalse(new ThongKeDoanhThuDAO(ctx).dichVuSuDung(ngay.minusDays(120),ngay).isEmpty());
        assertEquals(21,new ThongKeLichHenDAO(ctx).thongKe(ngay.minusDays(120),ngay.plusDays(10),null).stream().mapToLong(ThongKeLichHenDAO.TheoNgay::tong).sum());
        assertEquals(12,new ThongKeBenhNhanDAO(ctx).demBenhNhanKhacNhau(ngay.minusDays(120),ngay));
        assertNotNull(new ThongKeDoanhThuDAO(ctx).tongQuanTrongNgay(ngay));
    }
    @Test @Order(2) void taoNhanVien_trungTenTaiKhoan_rollbackNhanVien() throws Exception {
        long before=count("NhanVien");
        var nv=new NhanVien(null,"Nhân viên rollback",true,"0988880001","rollback@example.com","Nha sĩ");
        var tk=new TaiKhoan(null,"hieu.quanly","Clinic@123",VaiTro.NHASI,true,nv);
        assertThrows(BusinessException.class,() -> new TaiKhoanDAO(ctx).taoNhanVienVaTaiKhoan(nv,tk));
        assertEquals(before,count("NhanVien")); assertNull(nv.getMaNV()); assertNull(tk.getMaTK());
    }
    @Test @Order(1) void tongQuan_truyVanTheoNgayVaDuLieuChuaThanhToanCu() {
        var view=new ThongKeDoanhThuDAO(ctx).layTongQuanQuanLy();
        assertEquals(ngay,view.capNhatLuc().toLocalDate());
        assertEquals(2,view.tongLichHen()); assertEquals(2,view.daDen());
        assertEquals(0,view.hoaDonDaTra()); assertEquals(2,view.hoaDonChuaTra());
        assertEquals(0,new BigDecimal("1200000").compareTo(view.conPhaiThu()));
        assertEquals(0,view.daThu().compareTo(BigDecimal.ZERO));
        assertEquals(0,view.doanhThuDuKien().compareTo(view.daThu().add(view.conPhaiThu())));
        assertEquals(7,view.bieuDo().size()); assertEquals(ngay,view.bieuDo().getLast().ngay());
        assertTrue(view.bieuDo().stream().allMatch(d -> d.soTien().signum()==0));
        assertEquals(1,view.nhaSi().size()); assertTrue(view.nhaSi().getFirst().trongCa());
        assertNull(view.nhaSi().getFirst().benhNhanDangKham());
        assertNotNull(view.caHienTai());
        actor.set(leTan); assertThrows(BusinessException.class,() -> new ThongKeDoanhThuDAO(ctx).layTongQuanQuanLy());
    }
    @Test @Order(9) void tongQuan_sauThanhToan_capNhatSoLieuKhongNhanDoiDongDichVu() {
        var view=new ThongKeDoanhThuDAO(ctx).layTongQuanQuanLy();
        assertTrue(view.hoaDonDaTra()>=1);
        assertTrue(view.daThu().compareTo(new BigDecimal("2100000"))>=0);
        assertEquals(0,view.daThu().compareTo(view.bieuDo().getLast().soTien()));
        assertEquals(0,view.thuBayNgay().compareTo(view.bieuDo().stream().map(ThongKeDoanhThuDAO.DoanhThuNgay::soTien).reduce(BigDecimal.ZERO,BigDecimal::add)));
        var outside=new DaoContext(ds,actor::get,Clock.fixed(ngay.atTime(23,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),ZoneId.of("Asia/Ho_Chi_Minh")));
        var night=new ThongKeDoanhThuDAO(outside).layTongQuanQuanLy();
        assertNull(night.caHienTai());
        assertTrue(night.nhaSi().stream().noneMatch(ThongKeDoanhThuDAO.NhaSiHienTai::trongCa));
        assertTrue(night.nhaSi().stream().anyMatch(ns -> ns.benhNhanDangKham()!=null));
    }
    @Test @Order(3) void khachHang_taoKemHoSo_vaKhoaNgaySinh() throws Exception {
        long before=count("HoSoBenhAn"); KhachHang kh=khach();
        assertEquals(before+1,count("HoSoBenhAn"));
        kh.setNgaySinh(LocalDate.of(1996,1,1));
        assertThrows(BusinessException.class,() -> new KhachHangDAO(ctx).capNhat(kh));
    }
    @Test @Order(4) void lichLamViec_caSangCaChieuVaChongTrung() {
        LocalDate date=ngay.plusDays(25);
        var dao=new LichLamViecDAO(ctx);
        var sang=new LichLamViec(null,date,CaTruc.CA_SANG,LocalTime.of(7,0),LocalTime.NOON,TrangThaiLichLamViec.DA_PHAN_CONG,null,nhaSi.getNhanVien());
        var chieu=new LichLamViec(null,date,CaTruc.CA_CHIEU,LocalTime.of(13,30),LocalTime.of(17,30),TrangThaiLichLamViec.DA_PHAN_CONG,null,nhaSi.getNhanVien());
        assertTrue(dao.them(sang)); assertTrue(dao.them(chieu));
        assertThrows(BusinessException.class,() -> dao.them(new LichLamViec(null,date,CaTruc.CA_NGAY,LocalTime.of(7,0),LocalTime.of(17,30),TrangThaiLichLamViec.DA_PHAN_CONG,null,nhaSi.getNhanVien())));
    }
    @Test @Order(5) void datLich_duoi15PhutBiChan_dung15PhutChapNhan_vaKhongChoNghi() {
        LocalDate date=caMoi(); KhachHang a=khach(),b=khach();
        var dao=new LichHenDAO(ctx); LichHen first=lich(a,date,LocalTime.of(9,0)); assertTrue(dao.them(first));
        assertThrows(BusinessException.class,() -> dao.them(lich(b,date,LocalTime.of(9,44))));
        assertTrue(dao.them(lich(b,date,LocalTime.of(9,45))));
        var ca=new LichLamViecDAO(ctx).layTheoNhanVien(nhaSi.getNhanVien().getMaNV(),date,date).getFirst();
        assertThrows(BusinessException.class,() -> new LichLamViecDAO(ctx).duyetNghiPhep(ca.getMaLLV(),"Nghỉ test"));
    }
    @Test @Order(6) void datLich_dongThoi_chiMotGiaoDichThanhCong() throws Exception {
        LocalDate date=caMoi(); KhachHang a=khach(),b=khach();
        LichHen la=lich(a,date,LocalTime.of(9,0)),lb=lich(b,date,LocalTime.of(9,0));
        var start=new CountDownLatch(1); var successes=new AtomicInteger();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var jobs=new ArrayList<Future<?>>();
            for(LichHen lh:List.of(la,lb)) { jobs.add(pool.submit(() -> {
                try { start.await(); new LichHenDAO(ctx).them(lh); successes.incrementAndGet(); }
                catch(BusinessException expected) { } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            })); }
            start.countDown(); for(var job:jobs) { job.get(30,TimeUnit.SECONDS); }
        }
        assertEquals(1,successes.get());
    }
    @Test @Order(7) void phanQuyen_leTanKhongDocChanDoan_nhaSiKhongSuaDanhMuc() {
        actor.set(leTan);
        assertThrows(BusinessException.class,() -> new HoSoBenhAnDAO(ctx).layTatCa());
        assertThrows(BusinessException.class,() -> new ChiTietPhieuKhamDAO(ctx).layTheoPhieuKham("khong-co"));
        var pk=new PhieuKhamDAO(ctx).layTatCa().getFirst();
        assertNull(pk.getLoiDan()); assertNull(pk.getHoSoBenhAn().getTienSuBenh());
        actor.set(nhaSi); assertThrows(BusinessException.class,() -> new VoucherDAO(ctx).layTatCa());
        assertThrows(BusinessException.class,() -> new DichVuDAO(ctx).ngungCungCap("DV010"));
    }
    @Test @Order(8) void khamHoanTat_taoHoaDon_thanhToanVaKhoaDuLieu() throws Exception {
        KhachHang kh=khach(); LichHen lh=lich(kh,ngay,LocalTime.of(10,0)); new LichHenDAO(ctx).them(lh);
        actor.set(leTan); PhieuKham pk=new PhieuKhamDAO(ctx).taoTuLichHen(lh.getMaLH());
        actor.set(nhaSi); new PhieuKhamDAO(ctx).batDauKham(pk.getMaPK());
        var ct=new ChiTietPhieuKham(); ct.setDichVu(new DichVuDAO(ctx).layTheoMa("DV009"));
        ct.setDonViTinh("Lần"); ct.setSoLuong(1); ct.setTrieuChung("Nhiễm màu"); ct.setChanDoan("Chỉ định thẩm mỹ");
        pk.setLoiDan("Hướng dẫn sau điều trị");
        new PhieuKhamDAO(ctx).hoanTatPhieuKham(pk,List.of(ct));
        actor.set(leTan);
        HoaDon hd=new HoaDonDAO(ctx).layTatCa().stream().filter(h -> h.getPhieuKham().getMaPK().equals(pk.getMaPK())).findFirst().orElseThrow();
        assertEquals(0,new HoaDonDAO(ctx).tinhTongTien(hd.getMaHD()).compareTo(new BigDecimal("2200000")));
        assertTrue(new HoaDonDAO(ctx).apDungVoucher(hd.getMaHD(),"VC001"));
        assertThrows(BusinessException.class,() -> new HoaDonDAO(ctx).apDungVoucher(hd.getMaHD(),"VC002"));
        assertThrows(BusinessException.class,() -> new HoaDonDAO(ctx).thanhToan(hd.getMaHD(),HoaDon.TIEN_MAT,BigDecimal.ONE));
        assertFalse(new HoaDonDAO(ctx).layTheoMa(hd.getMaHD()).isTrangThai());
        assertEquals(0,new HoaDonDAO(ctx).thanhToan(hd.getMaHD(),HoaDon.TIEN_MAT,new BigDecimal("2200000")).compareTo(new BigDecimal("100000")));
        assertThrows(BusinessException.class,() -> new HoaDonDAO(ctx).thanhToan(hd.getMaHD(),HoaDon.TIEN_MAT,new BigDecimal("2200000")));
        assertEquals(TrangThaiLichHen.HOAN_TAT,new LichHenDAO(ctx).layTheoMa(lh.getMaLH()).getTrangThai());
        assertFalse(new ChiTietPhieuKhamDAO(ctx).layDongThanhToan(pk.getMaPK()).isEmpty());
        actor.set(nhaSi); assertThrows(BusinessException.class,() -> new PhieuKhamDAO(ctx).hoanTatPhieuKham(pk,List.of()));
    }
    @Test @Order(9) void taiKhoanMoi_hashVaPhanQuyenTheoDatabase() {
        var nv=new NhanVien(null,"Nha sĩ kiểm thử",true,"0988880002","newdoc@example.com","Nha sĩ");
        var tk=new TaiKhoan(null,"test.doctor","Clinic@123",VaiTro.NHASI,true,nv);
        new TaiKhoanDAO(ctx).taoNhanVienVaTaiKhoan(nv,tk);
        TaiKhoan signed=new TaiKhoanDAO(ctx).dangNhap("test.doctor","Clinic@123");
        actor.set(signed); assertTrue(new PhieuKhamDAO(ctx).layTatCa().isEmpty());
        assertThrows(BusinessException.class,() -> new HoSoBenhAnDAO(ctx).layTheoMa("HS00001"));
        assertThrows(BusinessException.class,() -> new TaiKhoanDAO(ctx).layTatCa());
    }
    @Test @Order(10) void backupVaRestore_chiDatabaseTest_vaDangXuat() throws Exception {
        String folder;
        try(var c=master.getConnection();var s=c.prepareStatement("SELECT CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS NVARCHAR(4000))");var r=s.executeQuery()) { r.next(); folder=r.getString(1); }
        var loggedOut=new AtomicBoolean();
        var backup=new BackupDAO(ctx,master::getConnection,() -> () -> { },() -> loggedOut.set(true),
                new QuyDinhSaoLuu(folder,Path.of("target/test-backups/"+db+".csv"),120));
        assertThrows(BusinessException.class,() -> backup.validateDuongDan(folder+"/../bad.bak"));
        assertThrows(BusinessException.class,() -> backup.validateDuongDan(folder+"/bad';.bak"));
        String file=backup.backup(LoaiSaoLuu.FULL); assertTrue(backup.verify(file));
        assertEquals(db,backup.docThongTin(file).database());
        long before=count("KhachHang"); KhachHang kh=khach(); assertEquals(before+1,count("KhachHang"));
        assertThrows(BusinessException.class,() -> backup.restore(file,"Sai@123",true));
        assertThrows(BusinessException.class,() -> backup.restore(file,"Clinic@123",false));
        backup.restore(file,"Clinic@123",true);
        assertEquals(before,count("KhachHang")); assertTrue(loggedOut.get());
        assertTrue(backup.layLichSu().stream().anyMatch(line -> line.contains("KHOI_PHUC")));
    }
    @Test @Order(9) void phieuKham_loiGiuaDanhSach_rollbackChiTietVaHoaDon() throws Exception {
        KhachHang kh=khach(); LichHen lh=lich(kh,ngay,LocalTime.of(11,0)); new LichHenDAO(ctx).them(lh);
        actor.set(leTan); PhieuKham pk=new PhieuKhamDAO(ctx).taoTuLichHen(lh.getMaLH());
        actor.set(nhaSi); new PhieuKhamDAO(ctx).batDauKham(pk.getMaPK());
        long details=count("ChiTietPhieuKham"),bills=count("HoaDon");
        var valid=new ChiTietPhieuKham(); valid.setDichVu(new DichVuDAO(ctx).layTheoMa("DV003"));
        valid.setSoLuong(1); valid.setDonViTinh("Răng"); valid.setTrieuChung("Đau răng"); valid.setChanDoan("Sâu răng");
        assertThrows(BusinessException.class,() -> new PhieuKhamDAO(ctx).hoanTatPhieuKham(pk,List.of(valid,new ChiTietPhieuKham())));
        assertEquals(details,count("ChiTietPhieuKham")); assertEquals(bills,count("HoaDon"));
        assertEquals(0,valid.getMaCTPK());
        assertEquals(TrangThaiPhieuKham.DANG_KHAM,new PhieuKhamDAO(ctx).layTheoMa(pk.getMaPK()).getTrangThai());
    }
    @Test @Order(9) void huyNhieuLan_gioiHanKhung_vaGoiYKhongTrung() {
        LocalDate date=caMoi(); KhachHang kh=khach(); var dao=new LichHenDAO(ctx);
        for(int hour:List.of(8,9,10)) { LichHen lh=lich(kh,date,LocalTime.of(hour,0)); dao.them(lh); dao.huy(lh.getMaLH(),"Bận việc"); }
        assertEquals(3,dao.demSoLanHuyTrongThang(kh.getMaKH(),date));
        assertThrows(BusinessException.class,() -> dao.them(lich(kh,date,LocalTime.of(11,0))));
        assertTrue(dao.them(lich(kh,date,LocalTime.of(16,0))));
        List<LocalDateTime> times=dao.goiYKhungTrong(kh.getMaKH(),"NV002","DV001",date);
        assertFalse(times.contains(date.atTime(16,0)));
        assertTrue(times.stream().allMatch(t -> t.toLocalTime().isBefore(LocalTime.of(9,30)) || !t.toLocalTime().isBefore(LocalTime.of(16,0))));
    }
    @Test @Order(9) void voucherHetHan_vaNhaSiNghiPhep_biChan() {
        HoaDon unpaid=new HoaDonDAO(ctx).layTatCa().stream().filter(h -> !h.isTrangThai()).findFirst().orElseThrow();
        assertThrows(BusinessException.class,() -> new HoaDonDAO(ctx).apDungVoucher(unpaid.getMaHD(),"VC003"));
        assertThrows(BusinessException.class,() -> new HoaDonDAO(ctx).apDungVoucher(unpaid.getMaHD(),"VC004"));
        assertThrows(BusinessException.class,() -> new HoaDonDAO(ctx).apDungVoucher(unpaid.getMaHD(),"VC005"));
        KhachHang kh=khach();
        assertThrows(BusinessException.class,() -> new LichHenDAO(ctx).them(lich(kh,ngay.plusDays(3),LocalTime.of(9,0))));
    }
    @Test @Order(9) void taiKham_ngoaiKhoangNgayBiChan_vaKhongDenKhongTinhHuy() {
        LocalDate date=caMoi();
        PhieuKham src=new PhieuKhamDAO(ctx).layTatCa().stream().filter(p -> p.getTrangThai()==TrangThaiPhieuKham.HOAN_TAT && p.getLichHen().getDichVu().getMaDV().equals("DV005")).findFirst().orElseThrow();
        var lh=lich(src.getHoSoBenhAn().getKhachHang(),date,LocalTime.of(8,0));
        lh.setDichVu(new DichVuDAO(ctx).layTheoMa("DV005")); lh.setPhieuKhamNguon(src);
        assertThrows(BusinessException.class,() -> new LichHenDAO(ctx).them(lh));
        ctx.transaction(c -> { DaoSupport.update(c,"UPDATE LichHen SET trangThai='DA_DAT' WHERE trangThai='KHONG_DEN'"); return null; });
        assertEquals(1,new LichHenDAO(ctx).capNhatKhongDen());
        assertEquals(0,new LichHenDAO(ctx).demSoLanHuyTrongThang("KH00019",ngay));
    }
}
