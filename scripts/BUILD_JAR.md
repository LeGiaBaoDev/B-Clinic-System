# Đóng gói JAR chạy độc lập

Yêu cầu Java 21 và Maven để build; máy chạy chỉ cần Java 21 và SQL Server. Chạy `mvn clean package` tại thư mục repo. Kết quả `target/BClinic-1.0-SNAPSHOT.jar` chứa thư viện và Main-Class com.quanlynhakhoa.App; không dùng file original-BClinic-1.0-SNAPSHOT.jar.

Đặt JAR và database.properties cùng thư mục, chạy `java -jar BClinic-1.0-SNAPSHOT.jar`. Cấu hình ưu tiên file trong thư mục đang chạy, tiếp theo file cạnh JAR; có thể chỉ rõ `java -Dbclinic.config=C:/duong-dan/database.properties -jar BClinic-1.0-SNAPSHOT.jar`. Khi chạy IDE vẫn hỗ trợ tài nguyên classpath. Cấu hình DB/SMTP thật không được nhúng trong fat JAR.

Bản demo đã chuẩn bị tại dist/BClinic-demo/BClinic.jar với Chay-BClinic.cmd và cấu hình DB của máy hiện tại; dist/BClinic-demo-share.zip chỉ chứa JAR, launcher, hướng dẫn và cấu hình mẫu. Không gửi database.properties thật. SQL Server và dữ liệu demo không nằm trong JAR; máy báo cáo hiện tại tiếp tục dùng BClinic_Dev đã có dữ liệu. Các migration V1–V3 nằm trong JAR và chạy khi khởi động; không tự nạp các script demo ngoài migration.

Kiểm chứng 06/11/2026: mvn clean package đạt, 322 test, 0 failures/errors, 6 skipped; kiểm manifest, service providers, 3 migration và loại file cấu hình riêng; chạy java -jar/Javaw, Flyway thành công và cửa sổ B-Clinic phản hồi. Chưa kiểm lại toàn bộ nghiệp vụ bằng tay trên JAR. Không commit/push/tag.
