import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.util.PasswordHasher;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;

/** Source launcher Java 21, chi nap script tuong ung vao database dev da cau hinh. */
public class SeedScenarios {
    public static void main(String[] args) throws Exception {
        boolean preview=args.length==1 && "--preview".equals(args[0]);
        if(args.length>0 && !preview) { throw new IllegalArgumentException("Chỉ hỗ trợ --preview hoặc không tham số."); }
        var ds=DatabaseConnection.getInstance().getDataSource();
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0);
        try(var c=ds.getConnection()) {
            if(!"BClinic_Dev".equals(c.getCatalog())) { throw new IllegalStateException("Chỉ cho phép BClinic_Dev."); }
            String sql=Files.readString(Path.of("scripts/seed_scenarios.sql"),StandardCharsets.UTF_8);
            try(var ps=c.prepareStatement(sql)) {
                ps.setQueryTimeout(90);
                for(int i=1;i<=6;i++) { ps.setString(i,PasswordHasher.bam("Clinic@123")); }
                ps.setObject(7,now.toLocalDate(),Types.DATE); ps.setObject(8,now,Types.TIMESTAMP); ps.setBoolean(9,preview);
                boolean results=ps.execute();
                while(true) {
                    if(results) { try(var r=ps.getResultSet()) {
                        while(r.next()) { for(int i=1;i<=r.getMetaData().getColumnCount();i++) {
                            System.out.print(r.getMetaData().getColumnLabel(i)+"="+r.getString(i)+" ");
                        } System.out.println(); }
                    } } else if(ps.getUpdateCount()==-1) { break; }
                    results=ps.getMoreResults();
                }
                for(var warning=ps.getWarnings();warning!=null;warning=warning.getNextWarning()) {
                    System.out.println(warning.getMessage());
                }
            }
            if(!preview) {
                for(String file:new String[]{"scripts/verify_demo.sql","scripts/verify_scenarios.sql"}) {
                    try(var ps=c.prepareStatement(Files.readString(Path.of(file),StandardCharsets.UTF_8))) {
                        ps.setQueryTimeout(60);
                        boolean result=ps.execute();
                        while(true) {
                            if(result) { try(var r=ps.getResultSet()) { while(r.next()) {
                                for(int i=1;i<=r.getMetaData().getColumnCount();i++) { System.out.print(r.getString(i)+" | "); }
                                System.out.println();
                            } } } else if(ps.getUpdateCount()==-1) { break; }
                            result=ps.getMoreResults();
                        }
                    }
                }
            }
        } finally { ds.close(); }
    }
}
