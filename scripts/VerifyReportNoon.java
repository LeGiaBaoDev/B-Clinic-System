import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.util.SessionManager;
import java.time.*;
public class VerifyReportNoon {
    static void actor(String name)throws Exception {
        try(var c=DatabaseConnection.getInstance().getConnection();var p=c.prepareStatement("SELECT maTK FROM TaiKhoan WHERE tenTK=? AND trangThai=1")){
            p.setString(1,name);try(var r=p.executeQuery()){if(!r.next())throw new IllegalStateException(name);var t=new TaiKhoan();t.setMaTK(r.getString(1));SessionManager.getInstance().login(t);}
        }
    }
    public static void main(String[] args)throws Exception {
        var day=LocalDate.of(2026,11,6);
        try{
            actor("hieu.nhasi");var dao=new ThongKeBenhNhanDAO();var s=dao.tongHop(day,day);
            if(s.soLuotKham()!=3||s.soBenhNhan()!=3||s.soBenhNhanMoi()!=3||s.doanhThu()!=2580000)throw new IllegalStateException("Sai thống kê nha sĩ: "+s);
            if(dao.danhSachBenhNhan(day,day).size()!=3||dao.doanhThuTheoNgay(day,day).size()!=1)throw new IllegalStateException("Thiếu biểu đồ/danh sách");
            System.out.println("DENTIST "+s);
            actor("hieu.quanly");var clock=Clock.fixed(day.atTime(13,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),ZoneId.of("Asia/Ho_Chi_Minh"));
            var q=new TongQuanQuanLyDAO(clock).layTongQuanQuanLy(day);
            System.out.println("MANAGER lich="+q.tongLichHen()+" daThu="+q.daThu()+" conPhaiThu="+q.conPhaiThu()+" hoanTat="+q.hoanTat());
            if(q.daThu().signum()<=0||q.conPhaiThu().signum()<=0||q.hoanTat()<3)throw new IllegalStateException("Thiếu dashboard");
            actor("hieu.letan");var bills=new ThongKeDoanhThuDAO().doanhThuTrongNgay(day).hoaDon();
            if(bills.isEmpty())throw new IllegalStateException("Thiếu hóa đơn lễ tân");
            System.out.println("RECEPTION invoices="+bills.size());
            System.out.println("DAO_REPORT_NOON_VERIFIED");
        }finally{SessionManager.getInstance().logout();DatabaseConnection.getInstance().getDataSource().close();}
    }
}
