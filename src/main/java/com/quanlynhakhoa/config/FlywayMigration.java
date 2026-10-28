package com.quanlynhakhoa.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

public class FlywayMigration {
    private FlywayMigration() {}

    public static void migrate() {
        DatabaseConnection db = DatabaseConnection.getInstance();

        Flyway flyway = Flyway.configure()
                .dataSource(db.getUrl(), db.getUsername(), db.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();
        System.out.printf("[Flyway] Migration hoàn tất — %d file đã chạy, version hiện tại: %s%n",
                result.migrationsExecuted,
                result.targetSchemaVersion);
    }
}
