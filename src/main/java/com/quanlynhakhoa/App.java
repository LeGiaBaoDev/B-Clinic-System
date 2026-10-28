package com.quanlynhakhoa;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.config.FlywayMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class App {
    public static void main(String[] args) {
        FlywayMigration.migrate();

        String sql = "SELECT maLoaiDV, tenLoaiDV, mucThueGTGT FROM LoaiDichVu";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\n--- Danh sách LoaiDichVu ---");
            while (rs.next()) {
                System.out.printf("  [%s] %s - Thuế: %.0f%%%n",
                        rs.getString("maLoaiDV"),
                        rs.getString("tenLoaiDV"),
                        rs.getDouble("mucThueGTGT"));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn: " + e.getMessage());
        }
    }
}
