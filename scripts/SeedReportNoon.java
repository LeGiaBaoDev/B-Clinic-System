import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.MaSinh;
import java.time.*;
import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Ảnh chụp dữ liệu demo tại 13:00 06/11/2026, không dùng như giao dịch thật. */
public class SeedReportNoon extends SeedFourDays {
    static final String TAG="REPORT_NOON_20261106";
    static final LocalDate DAY=LocalDate.of(2026,11,6);
    static String dentist,cashier;
    static String book(int i,String time,String state,String dv)throws Exception {
        String lh=id(MaSinh.Bang.LICH_HEN,DAY);
        exec("INSERT LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,ghiChu,lyDoHuy) VALUES(?,?,?,?,?,?,?,?)",lh,patients[i],dentist,dv,DAY.atTime(LocalTime.parse(time)),state,TAG+" - Mô phỏng đến 13:00",state.equals("DA_HUY")?"Khách bận việc gia đình, đã báo hủy từ buổi sáng":null);
        return lh;
    }
    static void medical(String lh,String time,String state,int pay,boolean extra,boolean voucher)throws Exception {
        String hs=one("SELECT hs.maHSBA FROM LichHen h JOIN HoSoBenhAn hs ON hs.maKH=h.maKH WHERE h.maLH=?",lh);
        String ns=one("SELECT maNV FROM LichHen WHERE maLH=?",lh);
        String dv=one("SELECT maDV FROM LichHen WHERE maLH=?",lh);
        String pk=id(MaSinh.Bang.PHIEU_KHAM,DAY);LocalDateTime at=DAY.atTime(LocalTime.parse(time));
        exec("INSERT PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,loiDan) VALUES(?,?,?,?,?,'CHO_KHAM',?)",pk,hs,lh,cashier,at,TAG+" - Vệ sinh răng miệng, theo dõi sau điều trị; dữ liệu mẫu báo cáo.");
        if(!state.equals("CHO_KHAM")) {
            for(String service:extra?new String[]{dv,"DV007"}:new String[]{dv})
                exec("INSERT ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT) SELECT ?,d.maDV,?,N'Cần kiểm tra, điều trị và chăm sóc răng miệng',N'Chỉ định sau khám lâm sàng',?,?,1,d.donGia,l.mucThueGTGT FROM DichVu d JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV WHERE d.maDV=? AND d.trangThai=1",pk,ns,state.equals("HOAN_TAT")?"Hoàn thành chỉ định, khách ổn định":null,service.equals("DV007")?"Cái":"Lần",service);
            exec("UPDATE PhieuKham SET trangThai=? WHERE maPK=?",state,pk);
            // Hóa đơn chưa trả cũng có khi đã lưu chỉ định, đúng UC023.
            LocalDateTime invoiceAt=at.plusMinutes(state.equals("HOAN_TAT")?Integer.parseInt(one("SELECT thoiLuong FROM DichVu WHERE maDV=?",dv)):5);
            exec("INSERT HoaDon(maHD,maPK,maVoucher,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,ngayThanhToan,tongKhuyenMai) VALUES(?,?,?,?,?,?,?,?,?)","HD"+pk.substring(2),pk,voucher?"VC002":null,pay==0?null:cashier,invoiceAt,pay>0,pay==0?null:pay==1?"Tiền mặt":"Chuyển khoản",pay==0?null:invoiceAt.plusMinutes(5),voucher?50000:0);
        }
        exec("UPDATE LichHen SET trangThai=? WHERE maLH=?",state.equals("HOAN_TAT")?"HOAN_TAT":"DA_DEN",lh);
        System.out.println("VISIT "+ns+" "+lh+" "+pk+" "+state+" pay="+pay);
    }
    public static void main(String[]args)throws Exception {
        boolean apply=args.length==1&&args[0].equals("--apply");
        if(args.length>0&&!apply)throw new IllegalArgumentException("--apply hoặc preview");
        var ds=DatabaseConnection.getInstance().getDataSource();
        try(var con=ds.getConnection()) {
            c=con;if(!c.getCatalog().equals("BClinic_Dev"))throw new IllegalStateException("Chỉ BClinic_Dev");
            exec("SET NOCOUNT ON; SET XACT_ABORT ON; SET QUOTED_IDENTIFIER ON; SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; SET NUMERIC_ROUNDABORT OFF;");c.setAutoCommit(false);
            try {
                one("SELECT COUNT(*) FROM NhatKyHeThong WITH(UPDLOCK,HOLDLOCK)");MaSinh.khoa(c,"BClinic:report-noon");
                if(one("SELECT maNhatKy FROM NhatKyHeThong WHERE hanhDong=?",TAG)!=null){c.rollback();System.out.println("ALREADY_SEEDED");return;}
                dentist=one("SELECT maNV FROM TaiKhoan WHERE tenTK='hieu.nhasi' AND trangThai=1 AND vaiTro='NHASI'");
                cashier=one("SELECT maNV FROM TaiKhoan WHERE tenTK='hieu.letan' AND trangThai=1 AND vaiTro='LETAN'");
                String ql=one("SELECT maNV FROM TaiKhoan WHERE tenTK='hieu.quanly' AND trangThai=1");
                if(dentist==null||cashier==null||ql==null)throw new IllegalStateException("Thiếu tài khoản demo");
                for(String nv:new String[]{dentist,cashier,ql})shift(nv,DAY,"DA_PHAN_CONG");
                for(int d=1;d<=3;d++)for(String nv:new String[]{dentist,cashier,ql})shift(nv,DAY.plusDays(d),"DA_PHAN_CONG");
                String leaveShift=one("SELECT maLLV FROM LichLamViec WHERE maNV=? AND ngayTruc='20261108'",dentist);
                exec("INSERT DonNghiPhep(maDon,maLLV,maNV,maTKGui,ngayTruc,caTruc,gioBatDau,gioKetThuc,lyDo,ngayGui,trangThai) VALUES(?,?,?,?,?,'CA_NGAY','07:00','17:30',?,?,'CHO_DUYET')",id(MaSinh.Bang.DON_NGHI_PHEP,null),leaveShift,dentist,one("SELECT maTK FROM TaiKhoan WHERE tenTK='hieu.nhasi'"),DAY.plusDays(2),"Việc gia đình ngày 08/11; đơn mẫu để trình diễn duyệt nghỉ phép",DAY.atTime(9,0));
                String[] names={"Nguyễn Minh Quân","Trần Hoài An","Lê Ngọc Hà","Phạm Tuấn Tú","Võ Thảo Nhi","Đặng Gia Hưng","Bùi Thanh Trúc","Đỗ Hải Nam","Hồ Ngọc Lan"};
                for(int i=0;i<names.length;i++) {
                    patients[i]=id(MaSinh.Bang.KHACH_HANG,null);records[i]=id(MaSinh.Bang.HO_SO,null);
                    exec("INSERT KhachHang VALUES(?,?,?,?,?,?,?)",patients[i],names[i],i%2==0,LocalDate.of(1988+i,2+i,10+i),"093763000"+(i+1),patients[i].toLowerCase()+"@example.com",(60+i*2)+" Đường Lê Văn Sỹ, TP. Hồ Chí Minh");
                    exec("INSERT HoSoBenhAn VALUES(?,?,?)",records[i],patients[i],TAG+" - "+(i==3?"Tiền sử tăng huyết áp; kiểm tra trước điều trị":"Chưa ghi nhận bệnh nền hoặc dị ứng"));
                }
                medical(book(0,"07:00","DA_DAT","DV001"),"07:00","HOAN_TAT",1,false,false);
                medical(book(1,"08:30","DA_DAT","DV003"),"08:30","HOAN_TAT",2,false,true);
                medical(book(2,"10:00","DA_DAT","DV009"),"10:00","HOAN_TAT",0,true,false);
                medical(book(3,"12:00","DA_DAT","DV006"),"12:00","DANG_KHAM",0,false,false);
                medical(book(4,"13:30","DA_DAT","DV001"),"12:50","CHO_KHAM",0,false,false);
                book(5,"14:30","DA_DAT","DV005");
                book(6,"15:45","DA_HUY","DV001");
                book(7,"09:30","KHONG_DEN","DV001");
                book(8,"16:00","DA_DAT","DV002");
                // Các lịch mẫu buổi sáng đã có: hoàn tất hợp lệ hoặc không đến tại mốc báo cáo.
                var morning=new ArrayList<String>();
                try(var p=c.prepareStatement("SELECT maLH FROM LichHen WHERE ngayGioHen>='20261106' AND ngayGioHen<'20261106 11:30' AND trangThai='DA_DAT' AND maNV<>? ORDER BY ngayGioHen")){p.setString(1,dentist);try(var r=p.executeQuery()){while(r.next())morning.add(r.getString(1));}}
                int k=0;for(String lh:morning) {
                    if(one("SELECT maPK FROM PhieuKham WHERE maLH=?",lh)!=null)throw new IllegalStateException("Lịch đã có phiếu "+lh);
                    if(one("SELECT maLLV FROM LichLamViec v JOIN LichHen h ON h.maNV=v.maNV AND CAST(h.ngayGioHen AS date)=v.ngayTruc WHERE h.maLH=? AND v.trangThai='DA_PHAN_CONG'",lh)==null)throw new IllegalStateException("Lịch sáng không có ca "+lh);
                    if(k++%4==3){exec("UPDATE LichHen SET trangThai='KHONG_DEN' WHERE maLH=?",lh);continue;}
                    String time=one("SELECT CONVERT(char(5),ngayGioHen,108) FROM LichHen WHERE maLH=?",lh);
                    medical(lh,time,"HOAN_TAT",k%3,false,false);
                }
                exec("INSERT NhatKyHeThong VALUES(?,?,?,?,?)",id(MaSinh.Bang.NHAT_KY,null),one("SELECT maTK FROM TaiKhoan WHERE tenTK='hieu.quanly'"),LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")),TAG,"Dữ liệu MÔ PHỎNG tại 13:00 ngày 06/11/2026 theo yêu cầu báo cáo; không phải giao dịch bệnh nhân thật.");
                try(var p=c.prepareStatement(Files.readString(Path.of("scripts/verify_report_noon.sql")))){boolean result=p.execute();while(true){if(result){try(var r=p.getResultSet()){while(r.next()){for(int i=1;i<=r.getMetaData().getColumnCount();i++)System.out.print(r.getMetaData().getColumnLabel(i)+"="+r.getString(i)+" ");System.out.println();}}}else if(p.getUpdateCount()==-1)break;result=p.getMoreResults();}}
                if(apply){c.commit();System.out.println("REPORT_NOON_COMMITTED");}else{c.rollback();System.out.println("REPORT_NOON_PREVIEW_ROLLED_BACK");}
            }catch(Exception e){c.rollback();throw e;}
        }finally{ds.close();}
    }
}
