package com.quanlynhakhoa.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Khởi chạy Flyway migration khi ứng dụng bắt đầu.
 *
 * <p>Flyway sẽ:
 * <ol>
 *   <li>Kết nối DB qua thông tin từ {@code database.properties}</li>
 *   <li>Tạo bảng {@code flyway_schema_history} nếu chưa có</li>
 *   <li>Quét {@code resources/db/migration/} tìm file SQL chưa chạy</li>
 *   <li>Thực thi theo thứ tự version (V1, V2, ...)</li>
 * </ol>
 *
 * <p>Quy tắc đặt tên file migration:
 * <pre>
 *   V{version}__{mô_tả}.sql
 *   Ví dụ: V1__init_schema.sql, V2__add_index.sql
 * </pre>
 */
public class FlywayMigration {

    private FlywayMigration() {}

    /**
     * Chạy migration. Gọi một lần duy nhất ở đầu {@code main()}.
     */
    public static void migrate() {
        DatabaseConnection db = DatabaseConnection.getInstance();

        Flyway flyway = Flyway.configure()
                .dataSource(db.getUrl(), db.getUsername(), db.getPassword())
                // Thư mục chứa các file SQL migration
                .locations("classpath:db/migration")
                // baselineVersion("0"): khi DB trống, Flyway baseline tại 0
                // → V1 trở đi sẽ được chạy bình thường
                .baselineOnMigrate(true)
                .baselineVersion("0")
                // Nếu checksum file thay đổi thì báo lỗi (bảo vệ tính toàn vẹn)
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();
        System.out.printf("[Flyway] Migration hoàn tất — %d file đã chạy, version hiện tại: %s%n",
                result.migrationsExecuted,
                result.targetSchemaVersion);
    }
}
