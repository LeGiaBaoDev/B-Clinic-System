import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.MaSinh;
import java.time.*;
import java.sql.*;

/** Dữ liệu thống kê hieu.nhasi, giữ nguyên nghỉ phép 05/11/2026. */
public class SeedDentistStatistics extends SeedFourDays {
    static final String TAG="DENTIST_STATS_20261105";
    static void addVisit(int i,LocalDate day,String time,String service,int payment)throws Exception {
        String nv=one("SELECT maNV FROM TaiKhoan WHERE tenTK='hieu.nhasi' AND vaiTro='NHASI' AND trangThai=1");
        String cashier=one("SELECT maNV FROM TaiKhoan WHERE tenTK='hieu.letan' AND trangThai=1");
        String lh=id(MaSinh.Bang.LICH_HEN,day),pk=id(MaSinh.Bang.PHIEU_KHAM,day);
        LocalDateTime at=day.atTime(LocalTime.parse(time));
        if(one("SELECT maLLV FROM LichLamViec WHERE maNV=? AND ngayTruc=? AND trangThai='DA_PHAN_CONG' AND gioBatDau<=CAST(? AS time) AND gioKetThuc>=CAST(DATEADD(minute,(SELECT thoiLuong FROM DichVu WHERE maDV=?),?) AS time)",nv,day,at,service,at)==null)throw new IllegalStateException("Không có ca hợp lệ "+day);
        exec("INSERT LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,ghiChu) VALUES(?,?,?,?,?,'HOAN_TAT',?)",lh,patients[i],nv,service,at,TAG);
        exec("INSERT PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,loiDan) VALUES(?,?,?,?,?,'CHO_KHAM',?)",pk,records[i],lh,cashier,at,TAG+" - Dữ liệu mẫu; vệ sinh răng miệng và theo dõi sau điều trị.");
        exec("INSERT ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT) SELECT ?,d.maDV,?,N'Cần kiểm tra và chăm sóc răng',N'Chỉ định sau thăm khám',N'Đã hoàn thành, khách ổn định',N'Lần',1,d.donGia,l.mucThueGTGT FROM DichVu d JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV WHERE d.maDV=? AND d.trangThai=1",pk,nv,service);
        exec("UPDATE PhieuKham SET trangThai='HOAN_TAT' WHERE maPK=?",pk);
        exec("INSERT HoaDon(maHD,maPK,ngayLap,trangThai,maNV_ThuNgan,hinhThucThanhToan,ngayThanhToan,tongKhuyenMai) VALUES(?,?,?,?,?,?,?,0)","HD"+pk.substring(2),pk,at.plusMinutes(5),payment>0,payment==0?null:cashier,payment==0?null:payment==1?"Tiền mặt":"Chuyển khoản",payment==0?null:at.plusMinutes(70));
        System.out.println(day+" "+time+" "+pk+" "+service);
    }
    public static void main(String[] args)throws Exception {
        boolean apply=args.length==1&&args[0].equals("--apply");
        if(args.length>0&&!apply)throw new IllegalArgumentException("Chỉ --apply hoặc preview");
        var ds=DatabaseConnection.getInstance().getDataSource();
        try(var con=ds.getConnection()) {
            c=con;if(!"BClinic_Dev".equals(c.getCatalog()))throw new IllegalStateException("Chỉ BClinic_Dev");
            exec("SET NOCOUNT ON; SET XACT_ABORT ON; SET QUOTED_IDENTIFIER ON; SET ANSI_NULLS ON; SET ANSI_WARNINGS ON; SET ANSI_PADDING ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; SET NUMERIC_ROUNDABORT OFF;");
            c.setAutoCommit(false);
            try {
                one("SELECT COUNT(*) FROM NhatKyHeThong WITH(UPDLOCK,HOLDLOCK)");
                MaSinh.khoa(c,"BClinic:dentist-statistics-seed");
                if(one("SELECT maNhatKy FROM NhatKyHeThong WHERE hanhDong=?",TAG)!=null){c.rollback();System.out.println("ALREADY_SEEDED");return;}
                String[] names={"Nguyễn Hải Đăng","Trần Ngọc Quỳnh","Lê Thanh Bình"};
                for(int i=0;i<3;i++) {
                    patients[i]=id(MaSinh.Bang.KHACH_HANG,null);records[i]=id(MaSinh.Bang.HO_SO,null);
                    exec("INSERT KhachHang VALUES(?,?,?,?,?,?,?)",patients[i],names[i],i!=1,LocalDate.of(1990+i,5,12+i),"093762000"+(i+1),patients[i].toLowerCase()+"@example.com",(42+i*4)+" Đường Nguyễn Oanh, TP. Hồ Chí Minh");
                    exec("INSERT HoSoBenhAn VALUES(?,?,?)",records[i],patients[i],TAG+" - Chưa ghi nhận bệnh nền hoặc dị ứng");
                }
                addVisit(0,LocalDate.of(2026,11,2),"07:00","DV001",1);
                addVisit(1,LocalDate.of(2026,11,2),"09:00","DV002",2);
                addVisit(0,LocalDate.of(2026,11,3),"11:00","DV002",0);
                addVisit(1,LocalDate.of(2026,11,4),"09:00","DV001",1);
                addVisit(2,LocalDate.of(2026,11,4),"11:00","DV009",0);
                exec("INSERT NhatKyHeThong VALUES(?,?,?,?,?)",id(MaSinh.Bang.NHAT_KY,null),one("SELECT TOP 1 maTK FROM TaiKhoan WHERE vaiTro='QUANLY' AND trangThai=1 ORDER BY maTK"),LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")),TAG,"Thêm 3 khách, 5 lượt hoàn tất 02–04/11 cho hieu.nhasi; giữ nghỉ phép 05/11.");
                try(var p=c.prepareStatement(java.nio.file.Files.readString(java.nio.file.Path.of("scripts/verify_dentist_statistics.sql")))){p.execute();}
                if(apply){c.commit();System.out.println("DENTIST_STATS_COMMITTED");}else{c.rollback();System.out.println("DENTIST_STATS_PREVIEW_ROLLED_BACK");}
            }catch(Exception e){c.rollback();throw e;}
        }finally{ds.close();}
    }
}
