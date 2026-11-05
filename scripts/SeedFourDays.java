import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.util.PasswordHasher;
import com.quanlynhakhoa.util.QuyTacTaiKhoan;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.math.*;
import java.util.*;

/** Dữ liệu demo: lịch 06–09/11, khám/thanh toán 01–04/11/2026. */
public class SeedFourDays {
    static final String MARK="FOUR_DAYS_REALTIME_20261106";
    static Connection c;
    static String a,b,leave,reception,manager,leaveTk;
    static String[] patients=new String[16], records=new String[16],services=new String[3],vouchers=new String[5],sources=new String[4];
    static final Map<String,Integer> added=new TreeMap<>();
    static void exec(String sql,Object... args)throws Exception {
        try(var p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.setQueryTimeout(60);p.executeUpdate();}
    }
    static String one(String sql,Object...args)throws Exception {
        try(var p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);try(var r=p.executeQuery()){return r.next()?r.getString(1):null;}}
    }
    static String id(MaSinh.Bang table,LocalDate day)throws Exception {return MaSinh.maMoi(c,table,day);}
    static void count(String t){added.merge(t,1,Integer::sum);}
    static String staff(String name,String phone,VaiTro role)throws Exception {
        String nv=id(MaSinh.Bang.NHAN_VIEN,null),tk=id(MaSinh.Bang.TAI_KHOAN,null);
        var used=new ArrayList<String>();try(var p=c.prepareStatement("SELECT tenTK FROM TaiKhoan");var r=p.executeQuery()){while(r.next())used.add(r.getString(1));}
        String username=QuyTacTaiKhoan.tenDangNhap(name,role,used);
        exec("INSERT NhanVien VALUES(?,?,?,?,?,?)",nv,name,!name.equals("Phạm Ngọc Trâm")&&!name.equals("Lê Ngọc Ánh"),phone,username+"@example.com",role==VaiTro.NHASI?"Nha sĩ":"Lễ tân");
        exec("INSERT TaiKhoan VALUES(?,?,?,?,?,1)",tk,nv,username,PasswordHasher.bam(QuyTacTaiKhoan.MAT_KHAU_MAC_DINH),role.name());
        if(name.equals("Phạm Ngọc Trâm"))leaveTk=tk;
        System.out.println("ACCOUNT "+username+" "+nv+" "+tk);count("NhanVien");count("TaiKhoan");return nv;
    }
    static String shift(String nv,LocalDate day,String status)throws Exception {
        String existing=one("SELECT maLLV FROM LichLamViec WHERE maNV=? AND ngayTruc=?",nv,day);
        if(existing!=null)return existing;
        String code=id(MaSinh.Bang.LICH_LAM_VIEC,null);
        exec("INSERT LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai,ghiChuNghi) VALUES(?,?,?,'CA_NGAY','07:00','17:30',?,?)",code,nv,day,status,status.equals("NGHI_PHEP")?"Nghỉ việc gia đình đã được quản lý duyệt":null);
        count("LichLamViec");return code;
    }
    static String appointment(int patient,String dentist,String service,LocalDate day,String time,String status,String reason,String source)throws Exception {
        String code=id(MaSinh.Bang.LICH_HEN,day);
        exec("INSERT LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,ghiChu,lyDoHuy,maPKNguon) VALUES(?,?,?,?,?,?,?,?,?)",code,patients[patient],dentist,service,day.atTime(LocalTime.parse(time)),status,MARK+" - "+(source==null?"lịch khám":"tái khám cùng nha sĩ"),reason,source);
        count("LichHen");return code;
    }
    static String visit(int patient,String dentist,String lh,LocalDate day,String time,String state,int kind,int pay,int voucher)throws Exception {
        String pk=id(MaSinh.Bang.PHIEU_KHAM,day);
        LocalDateTime at=day.atTime(LocalTime.parse(time));
        exec("INSERT PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,loiDan) VALUES(?,?,?,?,?,'CHO_KHAM',?)",pk,records[patient],lh,reception,at,MARK+" - "+(kind==4?"Điều trị tủy buổi 1; tái khám cùng nha sĩ sau 7–14 ngày.":"Vệ sinh răng miệng, tái khám khi có bất thường."));count("PhieuKham");
        if(!state.equals("CHO_KHAM")) {
            int[] types=kind==1?new int[]{0,1,2}:new int[]{kind==4?3:kind==2?2:0};
            for(int type:types){String dv=type==3?"DV005":services[type];int quantity=type==1?2:1;
                exec("INSERT ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT) SELECT ?,maDV,?,?,?, ?,N'Lần',?,donGia,l.mucThueGTGT FROM DichVu d JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV WHERE maDV=?",pk,dentist,type==2?"Răng nhiễm màu":"Ê buốt, cần kiểm tra răng miệng",type==2?"Chỉ định thẩm mỹ răng":"Chỉ định điều trị sau thăm khám",state.equals("HOAN_TAT")?"Đã hoàn thành chỉ định, khách ổn định":null,quantity,dv);count("ChiTietPhieuKham");}
            exec("UPDATE PhieuKham SET trangThai=? WHERE maPK=?",state,pk);
            String hd="HD"+pk.substring(2);BigDecimal total=new BigDecimal(one("SELECT SUM(ROUND(thanhTien*(1+mucThueGTGT/100),0)) FROM ChiTietPhieuKham WHERE maPK=?",pk));
            BigDecimal discount=BigDecimal.ZERO;
            if(voucher==0)discount=new BigDecimal("20000").min(total);
            if(voucher==1)discount=total.multiply(new BigDecimal("0.10")).setScale(0,RoundingMode.HALF_UP).min(new BigDecimal("100000"));
            exec("INSERT HoaDon(maHD,maPK,maVoucher,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,ngayThanhToan,tongKhuyenMai) VALUES(?,?,?,?,?,?,?,?,?)",hd,pk,voucher<0?null:vouchers[voucher],pay==0?null:reception,at.plusMinutes(5),pay>0,pay==0?null:pay==1?"Tiền mặt":"Chuyển khoản",pay==0?null:at.plusMinutes(65),discount);count("HoaDon");
        }
        return pk;
    }
    static void log(String action,String text,LocalDateTime time)throws Exception {
        exec("INSERT NhatKyHeThong VALUES(?,?,?,?,?)",id(MaSinh.Bang.NHAT_KY,null),manager,time,action,text);count("NhatKyHeThong");
    }
    static void seed()throws Exception {
        manager=one("SELECT TOP 1 maTK FROM TaiKhoan WHERE vaiTro='QUANLY' AND trangThai=1 ORDER BY maTK");
        a=staff("Đỗ Thanh Sơn","0937600101",VaiTro.NHASI);
        b=staff("Nguyễn Minh Hòa","0937600102",VaiTro.NHASI);
        leave=staff("Phạm Ngọc Trâm","0937600103",VaiTro.NHASI);
        reception=staff("Lê Ngọc Ánh","0937600104",VaiTro.LETAN);
        String[] names={"Nguyễn Hoàng Bách","Trần Thị Thanh Vân","Lê Quang Huy","Phạm Ngọc Hân","Võ Minh Tâm","Đặng Thu Thảo","Bùi Đức Long","Đỗ Bảo Ngọc","Hồ Gia Phúc","Dương Thùy Chi","Mai Tuấn Kiệt","Lý Khánh Vy","Nguyễn Anh Khoa","Trần Ngọc Diệp","Lê Minh Triết","Phạm Hải Yến"};
        for(int i=0;i<names.length;i++) {
            patients[i]=id(MaSinh.Bang.KHACH_HANG,null);records[i]=id(MaSinh.Bang.HO_SO,null);
            exec("INSERT KhachHang VALUES(?,?,?,?,?,?,?)",patients[i],names[i],i%2,LocalDate.of(1985+i,1+i%12,5+i),"093761"+String.format("%04d",i+1),"khach."+patients[i].toLowerCase()+"@example.com",(20+i*3)+" Đường Phan Văn Trị, TP. Hồ Chí Minh");count("KhachHang");
            exec("INSERT HoSoBenhAn VALUES(?,?,?)",records[i],patients[i],MARK+" - "+(i%4==0?"Dị ứng penicillin; cần kiểm tra trước kê đơn":i%4==1?"Tiền sử tăng huyết áp, theo dõi trước điều trị":"Chưa ghi nhận bệnh nền hoặc dị ứng"));count("HoSoBenhAn");
        }
        String[] cats={"Điều trị bệnh lý","Vật tư/thiết bị bán kèm","Thẩm mỹ"};
        String[] titles={"Khám và tư vấn nha chu","Bộ bàn chải kẽ răng","Đánh bóng răng thẩm mỹ"};
        int[] prices={180000,90000,1000000},durations={30,15,60};
        for(int i=0;i<3;i++) {
            String category=one("SELECT maLoaiDV FROM LoaiDichVu WHERE tenLoaiDV=?",cats[i]);
            if(category==null)throw new IllegalStateException("Thiếu nhóm dịch vụ chuẩn: "+cats[i]);
            services[i]=id(MaSinh.Bang.DICH_VU,null);
            exec("INSERT DichVu VALUES(?,?,?,?,?,?,1)",services[i],category,titles[i],prices[i],durations[i],null);count("DichVu");
        }
        for(int i=0;i<5;i++) {
            vouchers[i]=id(MaSinh.Bang.VOUCHER,null);
            exec("INSERT Voucher VALUES(?,?,?,?,?,?,?,?)",vouchers[i],new String[]{"Ưu đãi chăm sóc răng tháng 11","Giảm 10% tối đa 100.000 đồng","Ưu đãi tháng 10 đã hết hạn","Ưu đãi bắt đầu ngày 08/11","Ưu đãi đã ngừng áp dụng"}[i],i==1?1:0,i==1?10:20000,i==1?100000:null,LocalDate.parse(i==2?"2026-10-01":i==3?"2026-11-08":"2026-11-01"),LocalDate.parse(i==2?"2026-10-31":"2026-11-30"),i!=4);count("Voucher");
        }
        // Bốn phiếu nguồn quá khứ để lịch tái khám 06–09/11 đủ 7 ngày.
        for(int d=0;d<4;d++) {
            LocalDate day=LocalDate.of(2026,10,30).plusDays(d);shift(b,day,"DA_PHAN_CONG");shift(reception,day,"DA_PHAN_CONG");
            String lh=appointment(12+d,b,"DV005",day,"07:00","HOAN_TAT",null,null);
            sources[d]=visit(12+d,b,lh,day,"07:00","HOAN_TAT",4,0,-1);
        }
        for(int d=0;d<4;d++)for(boolean future:new boolean[]{false,true}) {
            LocalDate day=LocalDate.of(2026,11,future?6:1).plusDays(d);
            shift(a,day,"DA_PHAN_CONG");shift(b,day,"DA_PHAN_CONG");shift(reception,day,"DA_PHAN_CONG");
            String leaveShift=shift(leave,day,future&&d==1?"NGHI_PHEP":"DA_PHAN_CONG");
            if(future&&d<3) {
                String state=new String[]{"CHO_DUYET","DA_DUYET","TU_CHOI"}[d];
                LocalDateTime sent=LocalDateTime.of(2026,11,5,9,0);
                exec("INSERT DonNghiPhep(maDon,maLLV,maNV,maTKGui,ngayTruc,caTruc,gioBatDau,gioKetThuc,lyDo,ngayGui,trangThai,maTKXuLy,ngayXuLy,lyDoTuChoi) VALUES(?,?,?,?,?,'CA_NGAY','07:00','17:30',?,?,?,?,?,?)",id(MaSinh.Bang.DON_NGHI_PHEP,null),leaveShift,leave,leaveTk,day,"Việc gia đình cần giải quyết",sent,state,d==0?null:manager,d==0?null:sent.plusHours(1),d==2?"Chưa đủ nhân sự trực, đề nghị chọn ngày khác":null);count("DonNghiPhep");
            }
            String[] times={"07:00","08:30","10:00","13:30","15:00","16:00","09:00","11:00","14:00"};
            for(int s=0;s<9;s++) {
                String dentist=s<6?a:b;
                String state=future?(s==6?"DA_HUY":"DA_DAT"):(s<3?"HOAN_TAT":s<5?"DA_DEN":s==6?"DA_HUY":"KHONG_DEN");
                String service=s==1||s==3?services[2]:services[0];
                String lh=appointment(s,dentist,service,day,times[s],state,s==6?"Khách bận việc, chủ động hủy lịch":null,null);
                if(!future&&s<5)visit(s,dentist,lh,day,times[s],s<3?"HOAN_TAT":s==3?"DANG_KHAM":"CHO_KHAM",s==1?1:s==3?2:0,s==0?1:s==1?2:0,s==0?(d%2==0?-1:0):s==1?1:-1);
            }
            if(future) {
                appointment(12+d,b,"DV005",day,"07:00","DA_DAT",null,sources[d]);
                appointment(9,b,services[0],day,"14:00","DA_HUY","Khách xin dời sang 16:30 cùng ngày",null);
                appointment(9,b,services[0],day,"16:30","DA_DAT",null,null);
            } else {
                // Tiếp nhận trực tiếp, không có lịch hẹn.
                visit(10,b,null,day,"16:00","HOAN_TAT",0,1,0);
                log("MO_PHONG_HOAT_DONG",MARK+" - lịch/khám/thanh toán ngày "+day,day.atTime(17,20));
            }
        }
        log(MARK,"Bổ sung lịch 06–09/11/2026 và tình huống quá khứ 01–04/11; phiếu nguồn 30/10–02/11. Dữ liệu giả lập, không phải bệnh nhân thật.",LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
    }
    public static void main(String[] args)throws Exception {
        boolean apply=args.length==1&&args[0].equals("--apply");
        if(args.length>0&&!apply)throw new IllegalArgumentException("Chỉ hỗ trợ --apply hoặc preview mặc định");
        var ds=DatabaseConnection.getInstance().getDataSource();
        try(var connection=ds.getConnection()) {
            c=connection;if(!c.getCatalog().equals("BClinic_Dev"))throw new IllegalStateException("Chỉ BClinic_Dev");
            exec("SET NOCOUNT ON; SET XACT_ABORT ON; SET QUOTED_IDENTIFIER ON; SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; SET NUMERIC_ROUNDABORT OFF;");
            c.setAutoCommit(false);
            try {
                one("SELECT COUNT(*) FROM NhatKyHeThong WITH(UPDLOCK,HOLDLOCK)");
                String lock=one("DECLARE @r int; EXEC @r=sys.sp_getapplock @Resource=N'BClinic:four-days-seed',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000; SELECT @r;");
                if(lock==null||Integer.parseInt(lock)<0)throw new IllegalStateException("Seed lock: "+lock);
                if(one("SELECT maNhatKy FROM NhatKyHeThong WHERE hanhDong=?",MARK)!=null){System.out.println("ALREADY_SEEDED");c.rollback();return;}
                seed();
                String verify=Files.readString(Path.of("scripts/verify_four_days.sql"));
                try(var p=c.prepareStatement(verify)){p.setQueryTimeout(60);p.execute();}
                added.forEach((t,n)->System.out.println(t+" +"+n));
                if(apply){c.commit();System.out.println("FOUR_DAYS_COMMITTED");}else{c.rollback();System.out.println("FOUR_DAYS_PREVIEW_ROLLED_BACK");}
            }catch(Exception e){c.rollback();throw e;}
        }finally{ds.close();}
    }
}
