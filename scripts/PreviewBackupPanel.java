import com.formdev.flatlaf.FlatLightLaf;
import com.quanlynhakhoa.dao.BackupDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.LoaiSaoLuu;
import com.quanlynhakhoa.util.SessionManager;
import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.ui.panel.PnlSaoLuu;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Kiểm tra UI bằng render Swing, có thể tạo FULL backup thật khi truyền --backup. */
public class PreviewBackupPanel {
    static PnlSaoLuu panel;
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container inner)layout(inner);}
    static JTabbedPane tabs(Container c){for(Component child:c.getComponents()){if(child instanceof JTabbedPane t)return t;if(child instanceof Container inner){var t=tabs(inner);if(t!=null)return t;}}return null;}
    static boolean busy(Container c){for(Component child:c.getComponents()){if(child instanceof JProgressBar p&&p.isIndeterminate())return true;if(child instanceof Container inner&&busy(inner))return true;}return false;}
    public static void main(String[] args)throws Exception{
        FlatLightLaf.setup();var tk=new TaiKhoan();tk.setMaTK("TK001");SessionManager.getInstance().login(tk);
        try{
            if(args.length==1&&args[0].equals("--backup")){long start=System.nanoTime();System.out.println("FULL_BACKUP="+new BackupDAO().backup(LoaiSaoLuu.FULL));System.out.println("MILLISECONDS="+(System.nanoTime()-start)/1_000_000);}
            SwingUtilities.invokeAndWait(()->{panel=new PnlSaoLuu(()->{});panel.setSize(1180,730);layout(panel);});
            for(int wait=0;wait<100;wait++){Thread.sleep(200);var running=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->running.set(busy(panel)));if(!running.get())break;if(wait==99)throw new IllegalStateException("UI worker did not finish");}
            for(int tab=0;tab<4;tab++){final int selected=tab;SwingUtilities.invokeAndWait(()->{try{tabs(panel).setSelectedIndex(selected);layout(panel);var img=new BufferedImage(1180,730,BufferedImage.TYPE_INT_RGB);var g=img.createGraphics();panel.printAll(g);g.dispose();ImageIO.write(img,"png",Path.of("target/backup-ui-"+selected+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});}
        }finally{SessionManager.getInstance().logout();DatabaseConnection.getInstance().getDataSource().close();}
        System.exit(0);
    }
}
