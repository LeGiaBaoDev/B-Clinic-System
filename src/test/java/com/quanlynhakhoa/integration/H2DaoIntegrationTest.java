package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;

import com.quanlynhakhoa.util.*;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: database riêng và JVM riêng, không khởi tạo pool của DB ứng dụng trong JVM test. */
@EnabledIfSystemProperty(named = "bclinic.integration", matches = "true")
class H2DaoIntegrationTest {
    @Test
    void t601_lichLamViec() throws Exception {
        Properties props = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/database.properties"))) { props.load(in); }
        String name = "BClinic_Test_" + UUID.randomUUID().toString().replace("-", "");
        if (!name.matches("BClinic_Test_[a-f0-9]{32}")) { throw new IllegalStateException("Tên DB test không hợp lệ."); }
        String base = "jdbc:sqlserver://" + props.getProperty("db.host", "localhost") + ":"
                + props.getProperty("db.port", "1433") + ";encrypt=true;trustServerCertificate=true;"
                + (Boolean.parseBoolean(props.getProperty("db.integratedSecurity", "false"))
                        ? "integratedSecurity=true;" : "") + "databaseName=";
        Path directory = Files.createTempDirectory(Path.of("target"), "h2-dao-");
        Path config = directory.resolve("database.properties");
        Path output = directory.resolve("result.log");
        Process child = null;
        boolean created = false;
        try (Connection master = DriverManager.getConnection(base + "master",
                props.getProperty("db.username"), props.getProperty("db.password"))) {
            try {
                try (var s = master.createStatement()) { s.execute("CREATE DATABASE [" + name + "]"); created = true; }
                // Kiểm chứng nâng cấp DB V2 đang có dữ liệu, không chỉ tạo schema trống.
                Flyway.configure().dataSource(base + name, props.getProperty("db.username"),
                        props.getProperty("db.password")).locations("classpath:db/migration").target("2").load().migrate();
                try(Connection upgrade=DriverManager.getConnection(base+name,props.getProperty("db.username"),props.getProperty("db.password"))){
                    String before=Scenario.scalar(upgrade,"SELECT COUNT(*) FROM LichLamViec");
                    Flyway.configure().dataSource(base + name, props.getProperty("db.username"),
                            props.getProperty("db.password")).locations("classpath:db/migration").load().migrate();
                    assertEquals(before,Scenario.scalar(upgrade,"SELECT COUNT(*) FROM LichLamViec"));
                    assertEquals("0",Scenario.scalar(upgrade,"SELECT COUNT(*) FROM DonNghiPhep"));
                }
                props.setProperty("db.name", name);
                try (var out = Files.newOutputStream(config)) { props.store(out, "Isolated H2_DAO verification"); }
                String cp = directory.toAbsolutePath() + java.io.File.pathSeparator
                        + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                child = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-cp", cp,
                        Scenario.class.getName(), name).redirectErrorStream(true).redirectOutput(output.toFile()).start();
                assertTrue(child.waitFor(90, TimeUnit.SECONDS), "H2_DAO integration quá 90 giây.");
                String result = Files.readString(output);
                System.out.println(result);
                assertEquals(0, child.exitValue(), result);
                assertTrue(result.contains("H2_DAO_INTEGRATION_OK"), result);
            } finally {
                if (child != null && child.isAlive()) {
                    child.destroyForcibly();
                    if (!child.waitFor(10, TimeUnit.SECONDS)) { throw new IllegalStateException("JVM test chưa dừng."); }
                }
                if (created) {
                    try (var s = master.createStatement()) {
                        s.execute("ALTER DATABASE [" + name + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                        s.execute("DROP DATABASE [" + name + "]");
                    }
                    System.out.println("TEMP_TEST_DB_REMOVED");
                }
            }
        } finally {
            Files.deleteIfExists(config);
            // result.log không chứa mật khẩu; giữ lại để xem bằng chứng khi test lỗi.
        }
    }

    public static class Scenario {
        private static final LichLamViecDAO DAO = new LichLamViecDAO();
        private static final LocalDate DAY = LocalDate.now().plusYears(2);

        public static void main(String[] args) throws Exception {
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                assertEquals(args[0], scalar(c, "SELECT DB_NAME()"));
                System.out.println("ISOLATED_DATABASE_CONFIRMED");
                try {
                    login(VaiTro.QUANLY);
                    LichLamViec a = shift(DAY, "NV002", 7, 9);
                    assertTrue(DAO.them(a));
                    assertTrue(a.getMaLLV().matches("LLV\\d{5}"));
                    assertEquals(LocalTime.of(7, 0), DAO.layTheoMa(a.getMaLLV()).getGioBatDau());
                    LichLamViec duplicate = shift(DAY, "NV002", 9, 12);
                    BusinessException ex = assertThrows(BusinessException.class, () -> DAO.them(duplicate));
                    assertEquals("Nhân viên đã được phân công ca này trong ngày.", ex.getMessage());
                    assertNull(duplicate.getMaLLV());
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY, "NV002", 8, 10)));
                    LichLamViec other = shift(DAY, "NV003", 7, 9);
                    assertTrue(DAO.them(other));
                    assertNotEquals(a.getMaLLV(), other.getMaLLV());
                    LichLamViec afternoon = shift(DAY, "NV002", 14, 17);
                    afternoon.setCaTruc(CaTruc.CA_CHIEU);
                    assertTrue(DAO.them(afternoon));
                    LichLamViec full = shift(DAY, "NV002", 7, 17);
                    full.setCaTruc(CaTruc.CA_NGAY);
                    assertThrows(BusinessException.class, () -> DAO.them(full));
                    assertThrows(BusinessException.class, () -> DAO.them(shift(LocalDate.now().minusDays(1), "NV002", 7, 9)));
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY.plusDays(1), "NV002", 6, 9)));
                    assertTrue(DAO.capNhat(a)); // không tự xem ca hiện tại là trùng
                    LichLamViec changed = new LichLamViec(other);
                    changed.setNhanVien(a.getNhanVien());
                    assertThrows(BusinessException.class, () -> DAO.capNhat(changed));
                    assertEquals("NV003", DAO.layTheoMa(other.getMaLLV()).getNhanVien().getMaNV());
                    LichLamViec missing = new LichLamViec(a);
                    missing.setMaLLV("LLV99999");
                    assertThrows(BusinessException.class, () -> DAO.capNhat(missing));
                    assertThrows(BusinessException.class, () -> DAO.layTheoNhanVien("NV002", DAY, DAY.minusDays(1)));
                    login(VaiTro.NHASI);
                    assertEquals(2, DAO.layTheoNhanVien("NV002", DAY, DAY).size());
                    assertTrue(DAO.layTatCa().stream().allMatch(x -> x.getNhanVien().getMaNV().equals("NV002")));
                    assertThrows(BusinessException.class, () -> DAO.layTheoMa(other.getMaLLV()));
                    assertThrows(BusinessException.class, () -> DAO.layTheoNhanVien("NV003", DAY, DAY));
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY.plusDays(1), "NV002", 7, 9)));
                    login(VaiTro.LETAN);
                    assertThrows(BusinessException.class, () -> DAO.capNhat(other));
                    SessionManager.getInstance().logout();
                    assertThrows(BusinessException.class, DAO::layTatCa);
                    login(VaiTro.QUANLY);
                    try (var st = c.createStatement()) {
                        st.executeUpdate("UPDATE TaiKhoan SET trangThai=0 WHERE maNV='NV003'");
                    }
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY.plusDays(1), "NV003", 7, 9)));
                    try (var st = c.createStatement()) {
                        st.executeUpdate("UPDATE TaiKhoan SET trangThai=1 WHERE maNV='NV003'");
                        st.execute("CREATE TRIGGER T601_FailAudit ON NhatKyHeThong AFTER INSERT AS BEGIN THROW 51000, 'T601 rollback', 1; END");
                    }
                    LichLamViec rollback = shift(DAY.plusDays(2), "NV002", 7, 9);
                    assertThrows(DataAccessException.class, () -> DAO.them(rollback));
                    assertNull(rollback.getMaLLV(), "Rollback không được để lại mã trên đối tượng");
                    assertTrue(DAO.layTheoNhanVien("NV002", DAY.plusDays(2), DAY.plusDays(2)).isEmpty());
                    try (var st = c.createStatement()) { st.execute("DROP TRIGGER T601_FailAudit"); }
                    ExecutorService pool = Executors.newFixedThreadPool(2);
                    CountDownLatch gate = new CountDownLatch(1);
                    try {
                        List<Future<Boolean>> results = new ArrayList<>();
                        for (int i=0; i<2; i++) results.add(pool.submit(() -> {
                            gate.await();
                            try { return DAO.them(shift(DAY.plusDays(3), "NV002", 7, 9)); }
                            catch (BusinessException expected) { return false; }
                        }));
                        gate.countDown();
                        int success = 0;
                        for (Future<Boolean> result : results) if (result.get(20, TimeUnit.SECONDS)) success++;
                        assertEquals(1, success);
                        assertEquals(1, DAO.layTheoNhanVien("NV002", DAY.plusDays(3), DAY.plusDays(3)).size());
                    } finally { pool.shutdownNow(); }
                    var leave=shift(DAY.plusDays(5),"NV002",7,17);
                    leave.setCaTruc(CaTruc.CA_NGAY); legacyLeave(c,leave);
                    var blocked=shift(DAY.plusDays(5),"NV002",8,12);
                    assertTrue(assertThrows(BusinessException.class,()->DAO.them(blocked)).getMessage().contains("nghỉ phép"));
                    assertNull(blocked.getMaLLV());
                    var blockedAfternoon=shift(DAY.plusDays(5),"NV002",14,17); blockedAfternoon.setCaTruc(CaTruc.CA_CHIEU);
                    assertTrue(assertThrows(BusinessException.class,()->DAO.them(blockedAfternoon)).getMessage().contains("nghỉ phép"));
                    assertEquals(1,DAO.layTheoNhanVien("NV002",DAY.plusDays(5),DAY.plusDays(5)).size());
                    phanCongQuaGiaoDien();
                    lichCaNhanBaVaiTro(c);
                    doiCaVaHoanDoi(c);
                    nghiPhepVaTuChoi(c);
                    System.out.println("H2_DAO_INTEGRATION_OK");
                } finally {
                    SessionManager.getInstance().logout();
                    DatabaseConnection.getInstance().getDataSource().close();
                }
            }
        }

        // Mô phỏng ca nghỉ đã có trước migration V3; không tạo đơn giả cho dữ liệu lịch sử.
        private static void legacyLeave(Connection c,LichLamViec ca) throws Exception {
            assertTrue(DAO.them(ca));
            try(var ps=c.prepareStatement("UPDATE LichLamViec SET trangThai='NGHI_PHEP',ghiChuNghi=N'Nghỉ phép cũ' WHERE maLLV=?")){ps.setString(1,ca.getMaLLV());ps.executeUpdate();}
            ca.nghiPhep("Nghỉ phép cũ");
        }
        private static void nghiPhepVaTuChoi(Connection c) throws Exception {
            var don=new DonNghiPhepDAO();login(VaiTro.QUANLY);
            var a=shift(DAY.plusDays(40),"NV002",8,12);assertTrue(DAO.them(a));
            var b=shift(DAY.plusDays(40),"NV002",14,17);b.setCaTruc(CaTruc.CA_CHIEU);assertTrue(DAO.them(b));
            var other=shift(DAY.plusDays(42),"NV003",8,12);assertTrue(DAO.them(other));
            var mine=shift(DAY.plusDays(42),"NV001",8,12);assertTrue(DAO.them(mine));
            assertThrows(BusinessException.class,()->don.guiDon(a.getMaLLV(),"Không phải ca của tôi"));
            login(VaiTro.NHASI);
            for(String bad:new String[]{null,"","   ","x".repeat(256)})assertThrows(BusinessException.class,()->don.guiDon(a.getMaLLV(),bad));
            assertThrows(BusinessException.class,()->don.guiDon(other.getMaLLV(),"Giả người gửi"));
            String id=don.guiDon(a.getMaLLV(),"  Việc gia đình  ");assertTrue(id.matches("DNP\\d{6}"));
            assertEquals("CHO_DUYET",don.layDonCuaToi().getFirst().trangThai());assertEquals("Việc gia đình",don.layDonCuaToi().getFirst().lyDo());
            assertEquals(TrangThaiLichLamViec.DA_PHAN_CONG,DAO.layTheoMa(a.getMaLLV()).getTrangThai());
            assertThrows(BusinessException.class,()->don.guiDon(a.getMaLLV(),"Đơn trùng"));
            assertThrows(SQLException.class,()->{try(var ps=c.prepareStatement("INSERT INTO DonNghiPhep(maDon,maLLV,maNV,maTKGui,ngayTruc,caTruc,gioBatDau,gioKetThuc,lyDo) SELECT 'DNP999999',maLLV,maNV,maTKGui,ngayTruc,caTruc,gioBatDau,gioKetThuc,lyDo FROM DonNghiPhep WHERE maDon=?")){ps.setString(1,id);ps.executeUpdate();}});
            assertThrows(BusinessException.class,don::layDonQuanLy);assertThrows(BusinessException.class,()->don.duyet(id));assertThrows(BusinessException.class,()->don.tuChoi(id,"Không đồng ý"));assertThrows(BusinessException.class,()->don.kiemTra(id));
            login(VaiTro.LETAN);assertTrue(don.layDonCuaToi().isEmpty());assertThrows(BusinessException.class,()->don.duyet(id));
            login(VaiTro.QUANLY);assertTrue(don.layDonCuaToi().isEmpty());assertEquals(1,don.layDonQuanLy().size());
            assertTrue(don.kiemTra(id).isEmpty());assertTrue(don.duyet(id));
            assertEquals(TrangThaiLichLamViec.NGHI_PHEP,DAO.layTheoMa(a.getMaLLV()).getTrangThai());
            var approved=don.layDonQuanLy().getFirst();assertEquals("DA_DUYET",approved.trangThai());assertNotNull(approved.ngayXuLy());assertNotNull(approved.nguoiXuLy());
            assertThrows(BusinessException.class,()->don.duyet(id));assertThrows(BusinessException.class,()->don.tuChoi(id,"Xử lý lần hai"));
            assertThrows(BusinessException.class,()->DAO.duyetNghiPhep(b,"Bỏ qua đơn"));
            var illegal=new LichLamViec(b);illegal.nghiPhep("Bỏ qua đơn");assertThrows(BusinessException.class,()->DAO.capNhat(illegal));
            var fresh=shift(DAY.plusDays(55),"NV002",8,12);fresh.nghiPhep("Bỏ qua đơn");assertThrows(BusinessException.class,()->DAO.them(fresh));
            login(VaiTro.NHASI);assertThrows(BusinessException.class,()->don.guiDon(a.getMaLLV(),"Nghỉ lại"));
            String rejected=don.guiDon(b.getMaLLV(),"Xin nghỉ chiều");
            login(VaiTro.QUANLY);assertThrows(BusinessException.class,()->don.tuChoi(rejected,"   "));assertTrue(don.tuChoi(rejected,"Thiếu người trực"));
            assertEquals(TrangThaiLichLamViec.DA_PHAN_CONG,DAO.layTheoMa(b.getMaLLV()).getTrangThai());
            login(VaiTro.NHASI);var result=don.layDonCuaToi().stream().filter(d->d.maDon().equals(rejected)).findFirst().orElseThrow();assertEquals("Thiếu người trực",result.lyDoTuChoi());assertEquals("TU_CHOI",result.trangThai());
            String again=don.guiDon(b.getMaLLV(),"Xin lại sau từ chối");
            login(VaiTro.QUANLY);
            var kh=new KhachHangDAO().layTheoMa("KH00001");var ns=new NhanVienDAO().layTheoMa("NV002");var dv=new DichVuDAO().layTheoMa("DV001");
            var blocked=new LichHen(null,DAY.plusDays(40).atTime(9,0),com.quanlynhakhoa.entity.enums.TrangThaiLichHen.DA_DAT,null,null,kh,ns,dv,null);
            login(VaiTro.LETAN);assertTrue(assertThrows(BusinessException.class,()->new LichHenDAO().them(blocked)).getMessage().contains("nghỉ phép"));
            // Đơn chờ không tự chặn lịch hẹn; duyệt phải kiểm tra lại sau preview.
            login(VaiTro.QUANLY);assertTrue(don.kiemTra(again).isEmpty());
            var ap=new LichHen(null,DAY.plusDays(40).atTime(15,0),com.quanlynhakhoa.entity.enums.TrangThaiLichHen.DA_DAT,null,null,kh,ns,dv,null);assertTrue(new LichHenDAO().them(ap));
            assertEquals(1,don.kiemTra(again).size());assertThrows(BusinessException.class,()->don.duyet(again));
            try(var ps=c.prepareStatement("UPDATE LichHen SET trangThai='DA_DEN' WHERE maLH=?")){ps.setString(1,ap.getMaLH());ps.executeUpdate();}
            assertEquals("DA_DEN",don.kiemTra(again).getFirst().trangThai());assertThrows(BusinessException.class,()->don.duyet(again));
            try(var ps=c.prepareStatement("UPDATE LichHen SET trangThai='DA_HUY',lyDoHuy=N'Đã dời trong test' WHERE maLH=?")){ps.setString(1,ap.getMaLH());ps.executeUpdate();}
            assertTrue(don.duyet(again));
            login(VaiTro.LETAN);String rollback=don.guiDon(other.getMaLLV(),"Việc riêng; LLV00001 NV002: KHÔNG ĐƯỢC THẤY");
            login(VaiTro.QUANLY);
            try(var st=c.createStatement()){st.execute("CREATE TRIGGER UC015_FailAudit ON NhatKyHeThong AFTER INSERT AS BEGIN THROW 51000,'UC015 rollback',1; END");}
            try{assertThrows(DataAccessException.class,()->don.duyet(rollback));}finally{try(var st=c.createStatement()){st.execute("DROP TRIGGER UC015_FailAudit");}}
            assertEquals("CHO_DUYET",don.layDonQuanLy().stream().filter(d->d.maDon().equals(rollback)).findFirst().orElseThrow().trangThai());assertEquals(TrangThaiLichLamViec.DA_PHAN_CONG,DAO.layTheoMa(other.getMaLLV()).getTrangThai());
            try(var st=c.createStatement()){st.executeUpdate("UPDATE TaiKhoan SET trangThai=0 WHERE maNV='NV003'");}
            try{assertThrows(BusinessException.class,()->don.duyet(rollback));}finally{try(var st=c.createStatement()){st.executeUpdate("UPDATE TaiKhoan SET trangThai=1 WHERE maNV='NV003'");}}
            ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
            try{List<Future<Boolean>> res=new ArrayList<>();for(int i=0;i<2;i++)res.add(pool.submit(()->{gate.await();try{return don.duyet(rollback);}catch(BusinessException ex){return false;}}));gate.countDown();int n=0;for(var f:res)if(f.get(20,TimeUnit.SECONDS))n++;assertEquals(1,n);}finally{pool.shutdownNow();}
            login(VaiTro.NHASI);assertFalse(DAO.layThongBaoDoiCaGanNhat().noiDung().contains("KHÔNG ĐƯỢC THẤY"));assertTrue(don.layDonCuaToi().stream().allMatch(d->d.ca().getNhanVien().getMaNV().equals("NV002")));
            login(VaiTro.LETAN);assertTrue(DAO.layThongBaoDoiCaGanNhat().noiDung().contains("Đã duyệt nghỉ phép"));
            // Bản chụp ca trong đơn không được âm thầm đổi khi ca đã bị dời.
            login(VaiTro.QUANLY);String ownId=don.guiDon(mine.getMaLLV(),"Quản lý cũng là nhân viên");assertEquals(1,don.layDonCuaToi().size());
            assertTrue(DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(mine,null,DAY.plusDays(43),CaTruc.CA_SANG)));
            assertThrows(BusinessException.class,()->don.duyet(ownId));assertTrue(don.tuChoi(ownId,"Ca gốc đã thay đổi, vui lòng gửi lại"));
            // Gửi đơn đồng thời chỉ tạo một đơn chờ.
            pool=Executors.newFixedThreadPool(2);CountDownLatch sendGate=new CountDownLatch(1);
            try{List<Future<String>> res=new ArrayList<>();for(int i=0;i<2;i++)res.add(pool.submit(()->{sendGate.await();try{return don.guiDon(mine.getMaLLV(),"Gửi đồng thời");}catch(BusinessException ex){return null;}}));sendGate.countDown();int n=0;String pending=null;for(var f:res){var value=f.get(20,TimeUnit.SECONDS);if(value!=null){n++;pending=value;}}assertEquals(1,n);
                try(var ps=c.prepareStatement("UPDATE LichLamViec SET ngayTruc=? WHERE maLLV=?")){ps.setObject(1,LocalDate.now().minusDays(1));ps.setString(2,mine.getMaLLV());ps.executeUpdate();}
                final String pendingId=pending;assertThrows(BusinessException.class,()->don.guiDon(mine.getMaLLV(),"Ca quá khứ"));assertThrows(BusinessException.class,()->don.duyet(pendingId));assertTrue(don.tuChoi(pendingId,"Ca đã kết thúc"));
            }finally{pool.shutdownNow();}
            SessionManager.getInstance().logout();assertThrows(BusinessException.class,don::layDonCuaToi);login(VaiTro.QUANLY);
            System.out.println("UC015_EMPLOYEE_REQUEST_INBOX_APPROVE_REJECT_SNAPSHOT_ROLLBACK_CONCURRENT_PRIVACY_OK");
        }

        private static void doiCaVaHoanDoi(Connection c) throws Exception {
            var nv=new NhanVien(null,"Lê Kiểm Thử",true,"0980000004","uc014@example.com","Nha sĩ");
            var tk=new TaiKhoan(null,"ignored","ignored",VaiTro.NHASI,true,nv);
            assertTrue(new TaiKhoanDAO().taoNhanVienVaTaiKhoan(nv,tk));
            var a=shift(DAY.plusDays(20),"NV002",8,12); assertTrue(DAO.them(a));
            var b=shift(DAY.plusDays(21),nv.getMaNV(),14,17);b.setCaTruc(CaTruc.CA_CHIEU);assertTrue(DAO.them(b));
            var move=new LichLamViecDAO.YeuCauDoiCa(a,null,DAY.plusDays(22),CaTruc.CA_CHIEU);
            DAO.kiemTraDoiCa(move);assertEquals(DAY.plusDays(20),DAO.layTheoMa(a.getMaLLV()).getNgayTruc());
            assertTrue(DAO.doiCa(move));assertEquals(DAY.plusDays(20),a.getNgayTruc());
            assertEquals(DAY.plusDays(22),DAO.layTheoMa(a.getMaLLV()).getNgayTruc());
            assertThrows(BusinessException.class,()->DAO.doiCa(move)); // snapshot cũ
            var fresh=DAO.layTheoMa(a.getMaLLV());
            for(var role:List.of(VaiTro.NHASI,VaiTro.LETAN)) {
                login(role);assertThrows(BusinessException.class,()->DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(fresh,null,DAY.plusDays(24),CaTruc.CA_SANG)));
            }
            login(VaiTro.QUANLY);
            assertTrue(DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(fresh,b,null,null)));
            var aa=DAO.layTheoMa(a.getMaLLV());var bb=DAO.layTheoMa(b.getMaLLV());
            assertEquals(DAY.plusDays(21),aa.getNgayTruc());assertEquals(DAY.plusDays(22),bb.getNgayTruc());
            assertEquals("NV002",aa.getNhanVien().getMaNV());assertEquals(nv.getMaNV(),bb.getNhanVien().getMaNV());
            login(VaiTro.NHASI);var note=DAO.layThongBaoDoiCaGanNhat();assertNotNull(note);assertEquals("TK002",note.maTK());assertFalse(note.noiDung().contains(";"));
            SessionManager.getInstance().login(new TaiKhoanDAO().dangNhap(tk.getTenTK(),"Clinic@123"));
            var secondNote=DAO.layThongBaoDoiCaGanNhat();assertNotNull(secondNote);assertEquals(tk.getMaTK(),secondNote.maTK());
            login(VaiTro.QUANLY);
            String auditCount=scalar(c,"SELECT COUNT(*) FROM NhatKyHeThong WHERE hanhDong IN('DOI_CA','HOAN_DOI_CA')");
            try(var s=c.createStatement()) {s.execute("CREATE TRIGGER UC014_FailSecond ON LichLamViec AFTER UPDATE AS BEGIN IF EXISTS(SELECT 1 FROM inserted WHERE maLLV='"+bb.getMaLLV()+"') THROW 51001,'UC014 rollback',1; END");}
            try {assertThrows(DataAccessException.class,()->DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(aa,bb,null,null)));}
            finally {try(var s=c.createStatement()){s.execute("DROP TRIGGER UC014_FailSecond");}}
            assertEquals(aa.getNgayTruc(),DAO.layTheoMa(aa.getMaLLV()).getNgayTruc());assertEquals(bb.getNgayTruc(),DAO.layTheoMa(bb.getMaLLV()).getNgayTruc());
            assertEquals(auditCount,scalar(c,"SELECT COUNT(*) FROM NhatKyHeThong WHERE hanhDong IN('DOI_CA','HOAN_DOI_CA')"));
            var leave=shift(DAY.plusDays(23),"NV002",8,17);leave.setCaTruc(CaTruc.CA_NGAY);legacyLeave(c,leave);
            assertThrows(BusinessException.class,()->DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(aa,null,DAY.plusDays(23),CaTruc.CA_SANG)));
            assertThrows(BusinessException.class,()->DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(leave,null,DAY.plusDays(24),CaTruc.CA_SANG)));
            var other=shift(DAY.plusDays(26),"NV003",8,12);assertTrue(DAO.them(other));
            assertThrows(BusinessException.class,()->DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(aa,other,null,null)));
            assertThrows(BusinessException.class,()->DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(aa,null,LocalDate.now().minusDays(1),CaTruc.CA_SANG)));
            try(var s=c.createStatement()) {
                s.executeUpdate("IF NOT EXISTS(SELECT 1 FROM KhachHang WHERE maKH='KH00001') INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES('KH00001',N'Khách Test',1,'0909999999')");
                s.executeUpdate("IF NOT EXISTS(SELECT 1 FROM LoaiDichVu WHERE maLoaiDV='LDV01') INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES('LDV01',N'Điều trị bệnh lý',0)");
                s.executeUpdate("IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV001') INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong) VALUES('DV001','LDV01',N'Khám',100000,30)");
            }
            try(var s=c.prepareStatement("INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai) VALUES(?,'KH00001','NV002','DV001',?,'DA_DAT')")) {
                s.setString(1,"LH"+DAY.plusDays(21).format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))+"777");s.setObject(2,DAY.plusDays(21).atTime(15,0));s.executeUpdate();
            }
            assertTrue(assertThrows(BusinessException.class,()->DAO.kiemTraDoiCa(new LichLamViecDAO.YeuCauDoiCa(aa,null,DAY.plusDays(25),CaTruc.CA_CHIEU))).getMessage().contains("lịch hẹn"));
            assertThrows(BusinessException.class,()->DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(aa,bb,null,null)));
            assertEquals(aa.getNgayTruc(),DAO.layTheoMa(aa.getMaLLV()).getNgayTruc());
            ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
            try {
                List<Future<Boolean>> results=new ArrayList<>();
                for(int i=0;i<2;i++){final int d=i;results.add(pool.submit(()->{gate.await();try{return DAO.doiCa(new LichLamViecDAO.YeuCauDoiCa(bb,null,DAY.plusDays(28+d),CaTruc.CA_SANG));}catch(BusinessException ex){return false;}}));}
                gate.countDown();int successes=0;for(var f:results)if(f.get(20,TimeUnit.SECONDS))successes++;assertEquals(1,successes);
            } finally {pool.shutdownNow();}
            System.out.println("UC014_MOVE_SWAP_PERMISSION_APPOINTMENT_ROLLBACK_CONCURRENT_OK");
        }

        private static void lichCaNhanBaVaiTro(Connection c) throws Exception {
            assertTrue(DAO.them(shift(DAY.plusDays(10),"NV001",7,9)));
            for(VaiTro role:VaiTro.values()) {
                login(role);
                String staff=SessionManager.getInstance().getCurrentUser().getNhanVien().getMaNV();
                var own=DAO.layLichCaNhan();
                assertFalse(own.isEmpty()); assertTrue(own.stream().allMatch(x -> staff.equals(x.getNhanVien().getMaNV())));
                // Giả mạo mã nhân viên trong session cũng không đổi chủ dữ liệu mà DAO trả về.
                SessionManager.getInstance().getCurrentUser().getNhanVien().setMaNV(staff.equals("NV001")?"NV002":"NV001");
                assertTrue(DAO.layLichCaNhan().stream().allMatch(x -> staff.equals(x.getNhanVien().getMaNV())));
                login(role);
                var ref=new java.util.concurrent.atomic.AtomicReference<com.quanlynhakhoa.ui.panel.PnlLichCaNhan>();
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    var panel=new com.quanlynhakhoa.ui.panel.PnlLichCaNhan(); ref.set(panel); panel.lamMoiDuLieu();
                });
                var panel=ref.get();
                choGiaoDien(() -> ((javax.swing.JButton)tim(panel,"lamMoiLich")).isEnabled());
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    assertNull(tim(panel,"moPhanCa")); assertNull(tim(panel,"luuPhanCong"));
                    assertNull(tim(panel,"locNhanVienLich"));
                    var table=(javax.swing.JTable)tim(panel,"bangLichLamViec");
                    LocalDate first=LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    long expected=own.stream().filter(x -> !x.getNgayTruc().isBefore(first)&&!x.getNgayTruc().isAfter(first.plusDays(6))).count();
                    assertEquals(expected,table.getRowCount());
                    assertFalse(((javax.swing.JLabel)tim(panel,"ketQuaLich")).getText().contains("Không thể"));
                    for(int i=0;i<table.getRowCount();i++) assertTrue(table.getValueAt(i,1).toString().contains(staff));
                });
                System.out.println("UC012_PERSONAL_ONLY_"+role+"_OK");
            }
            login(VaiTro.LETAN);
            try(var s=c.prepareStatement("UPDATE TaiKhoan SET trangThai=0 WHERE maNV='NV003'")) {s.executeUpdate();}
            try { assertThrows(BusinessException.class,DAO::layLichCaNhan); }
            finally {try(var s=c.prepareStatement("UPDATE TaiKhoan SET trangThai=1 WHERE maNV='NV003'")){s.executeUpdate();}}
            SessionManager.getInstance().logout(); assertThrows(BusinessException.class,DAO::layLichCaNhan);
            login(VaiTro.QUANLY);
        }

        private static void phanCongQuaGiaoDien() throws Exception {
            var ref = new java.util.concurrent.atomic.AtomicReference<com.quanlynhakhoa.ui.panel.PnlLichLamViec>();
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                var panel = new com.quanlynhakhoa.ui.panel.PnlLichLamViec();
                ref.set(panel); panel.lamMoiDuLieu();
            });
            var panel = ref.get();
            choGiaoDien(() -> ((javax.swing.JComboBox<?>)tim(panel,"nhanVienPhanCong")).isEnabled());
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ((javax.swing.JComboBox<?>)tim(panel,"viTri")).setSelectedItem("Nha sĩ");
                ((javax.swing.JComboBox<?>)tim(panel,"caPhanCong")).setSelectedIndex(1);
                ((com.toedter.calendar.JDateChooser)tim(panel,"ngayPhanCong")).setDate(java.util.Date.from(
                        DAY.plusDays(4).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                ((javax.swing.JButton)tim(panel,"luuPhanCong")).doClick();
            });
            choGiaoDien(() -> ((javax.swing.JComboBox<?>)tim(panel,"nhanVienPhanCong")).isEnabled()
                    && ((javax.swing.JLabel)tim(panel,"thongBaoPhanCong")).getText().contains("thành công"));
            var rows = DAO.layTheoNhanVien("NV002",DAY.plusDays(4),DAY.plusDays(4));
            assertEquals(1,rows.size()); assertEquals(LocalTime.of(13,30),rows.getFirst().getGioBatDau());
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                var table=(javax.swing.JTable)tim(panel,"bangLichLamViec");
                String day=DAY.plusDays(4).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                assertTrue(java.util.stream.IntStream.range(0,table.getRowCount())
                        .anyMatch(i -> day.equals(table.getValueAt(i,0))&&table.getValueAt(i,4).toString().contains("13:30")));
                assertFalse(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
            });
            var duplicate=shift(DAY.plusDays(4),"NV002",14,17); duplicate.setCaTruc(CaTruc.CA_CHIEU);
            assertThrows(BusinessException.class,()->DAO.them(duplicate));
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ((com.toedter.calendar.JDateChooser)tim(panel,"ngayPhanCong")).setDate(java.util.Date.from(
                        DAY.plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                assertFalse(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
                assertTrue(((javax.swing.JLabel)tim(panel,"thongBaoPhanCong")).getText().contains("nghỉ phép"));
                ((javax.swing.JComboBox<?>)tim(panel,"caPhanCong")).setSelectedIndex(0);
                assertFalse(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
                ((com.toedter.calendar.JDateChooser)tim(panel,"ngayPhanCong")).setDate(java.util.Date.from(
                        DAY.plusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                assertTrue(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
            });
            assertEquals(1,DAO.layTheoNhanVien("NV002",DAY.plusDays(4),DAY.plusDays(4)).size());
            System.out.println("H2_UC013_UI_SAVE_AND_DUPLICATE_OK");
            System.out.println("H2_LEAVE_BLOCKED_UI_AND_DAO_OK");
        }

        private static java.awt.Component tim(java.awt.Container root,String name) {
            for(var child:root.getComponents()) {
                if(name.equals(child.getName())) return child;
                if(child instanceof java.awt.Container c) { var found=tim(c,name); if(found!=null)return found; }
            }
            return null;
        }

        private static void choGiaoDien(java.util.function.BooleanSupplier condition) throws Exception {
            for(int i=0;i<200;i++) {
                var ok=new java.util.concurrent.atomic.AtomicBoolean();
                javax.swing.SwingUtilities.invokeAndWait(() -> ok.set(condition.getAsBoolean()));
                if(ok.get())return;
                Thread.sleep(50);
            }
            fail("Giao diện phân công chưa hoàn tất xử lý.");
        }

        private static void login(VaiTro role) {
            SessionManager.getInstance().login(new TaiKhoanDAO().dangNhap("hieu." + role.name().toLowerCase(Locale.ROOT), "Clinic@123"));
        }

        private static LichLamViec shift(LocalDate day, String staff, int start, int end) {
            NhanVien nv = new NhanVien(); nv.setMaNV(staff);
            return new LichLamViec(null, day, CaTruc.CA_SANG,
                    LocalTime.of(start, 0), LocalTime.of(end, 0),
                    TrangThaiLichLamViec.DA_PHAN_CONG, null, nv);
        }

        private static String scalar(Connection c, String sql) throws SQLException {
            try (var s = c.prepareStatement(sql); var r = s.executeQuery()) { r.next(); return r.getString(1); }
        }
    }
}
