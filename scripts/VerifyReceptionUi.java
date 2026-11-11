import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.ui.panel.*;
import com.quanlynhakhoa.util.SessionManager;
import javax.swing.*;
import java.lang.reflect.*;

public class VerifyReceptionUi {
 static PnlLeTan panel;
 static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args){int code=1;try{
  TaiKhoan actor=new TaiKhoan();
  try(var c=DatabaseConnection.getInstance().getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT TOP 1 t.maTK,n.* FROM TaiKhoan t JOIN NhanVien n ON n.maNV=t.maNV WHERE t.vaiTro='LETAN' AND t.trangThai=1 ORDER BY t.maTK")){
   check(r.next(),"Missing receptionist");actor.setMaTK(r.getString("maTK"));actor.setVaiTro(VaiTro.LETAN);actor.setTenTK("test.letan");
   actor.setNhanVien(NhanVien.tuDuLieu(r.getString("maNV"),r.getString("hoTenNV"),r.getBoolean("gioiTinh"),r.getString("sdt"),r.getString("email"),r.getString("chuyenMon")));
  }
  SessionManager.getInstance().login(actor);
  SwingUtilities.invokeAndWait(()->panel=new PnlLeTan(actor,()->{}));
  SwingUtilities.invokeAndWait(()->{try{check(((JPanel)field(panel,"pnlCards")).getComponentCount()==1,"Unexpected eager pages");check(field(panel,"pnlHoaDon")==null,"Invoice initialized eagerly");}catch(Exception e){throw new RuntimeException(e);}});
  Object dashboard=field(panel,"pnlTongQuan");long start=System.nanoTime();boolean ready=false;
  for(int i=0;i<400;i++){
   boolean[] state={false};SwingUtilities.invokeAndWait(()->{try{state[0]=!((JLabel)field(dashboard,"lblLichHen")).getText().equals("—");}catch(Exception e){throw new RuntimeException(e);}});
   if(state[0]){ready=true;break;}Thread.sleep(50);
  }
  check(ready,"Dashboard timeout");
  SwingUtilities.invokeAndWait(()->{try{
   check(((JLabel)field(dashboard,"lblLoi")).getText().isBlank(),"Dashboard error");
   JTable table=null; Object model=field(dashboard,"modelHoaDon");var m=(javax.swing.table.DefaultTableModel)model;
   for(int i=0;i<m.getRowCount();i++)check(!"—".equals(m.getValueAt(i,1)),"Missing customer name");
   Method open=PnlLeTan.class.getDeclaredMethod("moTrang",String.class);open.setAccessible(true);open.invoke(panel,"Bệnh nhân");
   check(((JPanel)field(panel,"pnlCards")).getComponentCount()==2,"Lazy page creation failed");
   check(field(panel,"pnlHoaDon")==null,"Other pages loaded");
  }catch(Exception e){throw new RuntimeException(e);}});
  System.out.println("RECEPTION_UI_OK dashboard_ms="+(System.nanoTime()-start)/1000000+" initial_cards=1 customer_names_verified=true");code=0;
 }catch(Throwable e){e.printStackTrace();}finally{try{if(panel!=null)((Timer)field(panel,"dongHo")).stop();}catch(Exception ignored){}SessionManager.getInstance().logout();}System.exit(code);}
}
