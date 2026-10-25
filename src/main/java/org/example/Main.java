package org.example;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.config.FlywayMigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {

        // 1. Chạy migration — tự tạo/cập nhật schema trước khi dùng DB
        FlywayMigration.migrate();

        // 2. Dùng DB bình thường
        DatabaseConnection db = DatabaseConnection.getInstance();

        String sql = "SELECT maLoaiDV, tenLoaiDV, mucThueGTGT FROM LoaiDichVu";
        try (Connection conn = db.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            System.out.println("\n--- Danh sách LoaiDichVu ---");
            while (rs.next()) {
                System.out.printf("  [%d] %s - Thuế: %.0f%%%n",
                        rs.getInt("maLoaiDV"),
                        rs.getString("tenLoaiDV"),
                        rs.getDouble("mucThueGTGT"));
            }

        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn: " + e.getMessage());
        }
    }
}
