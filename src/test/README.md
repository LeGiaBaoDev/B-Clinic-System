# Kiểm thử H1 — Tài khoản và nhân viên

Chạy test thông thường, không cần SQL Server:

```powershell
mvn test
```

Chạy toàn bộ test, bao gồm tích hợp H1 với SQL Server và cửa sổ Swing:

```powershell
mvn '-Dbclinic.integration=true' test
```

Chỉ chạy phần tích hợp H1:

```powershell
mvn '-Dbclinic.integration=true' '-Dtest=H1IntegrationTest' test
```

Nếu các thư viện đã có trong cache Maven, thêm `-o` để chạy offline.

## Điều kiện và cách cô lập

- JDK 21, SQL Server đang chạy; thông tin kết nối lấy từ file riêng `src/main/resources/database.properties`.
- Tài khoản SQL cần quyền tạo/xóa database test. Cần desktop hoạt động để mở cửa sổ Swing kiểm tra đăng xuất.
- Test tạo `BClinic_Test_<uuid>`, migrate vào database đó và chạy DAO trong JVM riêng.
- File cấu hình test được đặt đầu classpath trước khi JVM khởi động. Test xác nhận `DB_NAME()` khớp chính xác tên vừa tạo trước mọi thao tác DAO.
- Test đóng JVM, xóa đúng database vừa tạo và xóa file cấu hình test trong `finally`. Không chọn database ứng dụng làm đích kiểm thử.
- Bằng chứng chạy nằm trong `target/h1-integration-*/result.log`. Chỉ bật tích hợp bằng cờ trên; không tự bỏ qua lỗi kết nối/quyền/desktop khi đã bật.

## Phạm vi tự động

| Nhóm | Đã kiểm tra |
|---|---|
| UC001 | Thiếu/sai thông tin, tài khoản không tồn tại; đăng nhập cả ba vai trò; nâng hash seed; gọi handler đăng xuất của FrmMain với xác nhận, xóa session và ghi nhật ký; FrmDangNhap thật yêu cầu đổi mật khẩu mặc định trước cấp session/mở main, hủy không vào hệ thống, đổi xong đăng nhập lại không bị hỏi nữa |
| Phân quyền | Chỉ Quản lý xem danh sách tài khoản/nhật ký; Nha sĩ/Lễ tân không tạo nhân viên hoặc xem tài khoản khác; sửa vai trò trong session không vượt quyền DB |
| UC002 | Tạo đồng thời 4 người cùng tên, hai lượt; đúng chuỗi tên từ không số tới số 7; mã/tên không trùng, mật khẩu mặc định được hash, không nhân viên dư; trùng SĐT rollback |
| UC003 theo STAGES | Cập nhật/khóa/mở khóa; giữ tên và mật khẩu; trùng email rollback quyền/trạng thái; chặn tự khóa/hạ quyền, tài khoản không tồn tại, nha sĩ còn lịch chưa xử lý |
| UC004 theo STAGES | Test panel: tìm kiếm, chi tiết sau sắp xếp, dữ liệu không có tài khoản, quyền, lỗi tải và bật/tắt nút sửa theo dòng chọn |
| Form | Chuẩn hóa họ tên, tối thiểu hai từ, định dạng liên hệ và hướng dẫn đỏ, tên sinh khi rời ô, chuyên môn/role, lưu nền, xác nhận khóa và giữ dữ liệu khi DAO lỗi |

Test logout UI gọi handler thật và tự trả lời hộp xác nhận; không phải test thao tác chuột vật lý. Quy tắc đánh số UC theo STAGES hiện hành; báo cáo Word phiên bản khác có thể đánh số khác.

## Kiểm thử chéo trước khi chốt H1

Các hàng dưới dành cho một thành viên khác thao tác trên ứng dụng và ghi kết quả; chưa được coi là đạt chỉ vì test tự động xanh.

| Mã | Thao tác | Kết quả mong đợi | Kết quả kiểm thử chéo |
|---|---|---|---|
| H1-M01 | Đăng nhập từng vai trò, đăng xuất; hủy rồi xác nhận hộp đăng xuất | Đúng màn hình/quyền; hủy giữ phiên, xác nhận xóa phiên và có nhật ký | Chờ |
| H1-M02 | Nhập sai mật khẩu; thử tài khoản khóa | Báo lỗi, không vào màn chính | Chờ |
| H1-M03 | Tạo nhân viên hợp lệ với mỗi chuyên môn | Role tự đồng bộ; tên và mật khẩu tự cấp; danh sách tải lại theo mã mới | Chờ |
| H1-M04 | Nhập tên một từ/SĐT/email sai, rời ô bằng Tab hoặc bấm nền | Hướng dẫn đỏ đúng ô; sửa đúng và rời ô thì ẩn lỗi | Chờ |
| H1-M05 | Gõ họ tên thường, chưa rời ô rồi rời ô | Chưa rời ô không đổi username; khi rời ô tên viết hoa đầu từ và username cập nhật | Chờ |
| H1-M06 | Tạo thêm người cùng tên cuối và cùng role | Tên được thêm số nhỏ nhất còn trống trước dấu chấm | Chờ |
| H1-M07 | Tìm kiếm, sắp xếp bảng, chọn dòng rồi nhấn đúp | Đúng người trong chi tiết và form cập nhật; bấm vùng bảng trống không mở form | Chờ |
| H1-M08 | Sửa thông tin rồi Hủy/đóng; mở lại | Không lưu dữ liệu đã hủy | Chờ |
| H1-M09 | Cập nhật liên hệ/chuyên môn và Lưu | Tải lại danh sách; giữ tên đăng nhập/mật khẩu, role khớp chuyên môn | Chờ |
| H1-M10 | Chọn khóa; hủy xác nhận rồi xác nhận lại; mở khóa | Hủy không lưu, xác nhận khóa không đăng nhập được, mở khóa đăng nhập lại được | Chờ |
| H1-M11 | Tự khóa/hạ quyền; khóa nha sĩ còn lịch chưa xử lý | Báo đúng lỗi và không thay đổi thông tin | Chờ |
| H1-M12 | Sửa email/SĐT trùng; thử khi mất kết nối DB | Báo lỗi, không lưu dở dang, form cho thử lại | Chờ |
| H1-M13 | Đăng nhập bằng Clinic@123; hủy/đóng form đổi lần đầu, rồi đăng nhập lại và đổi thành công | Hủy quay lại đăng nhập; chưa đổi không vào màn chính; mật khẩu mới khác mặc định, ít nhất 6 ký tự và xác nhận khớp; đăng nhập bằng mật khẩu mới không hiện form lần nữa | Chờ |

Sau kiểm thử chéo, thành viên tự commit/push và làm PR theo quy trình nhóm. Bộ test không tự commit, push hoặc xác nhận review.

## H2 — T601: DAO lịch làm việc

Chạy riêng (SQL Server, không cần mở giao diện Swing):

```powershell
mvn '-Dbclinic.integration=true' '-Dtest=H2DaoIntegrationTest' test
```

`H2DaoIntegrationTest` tạo database UUID riêng, chạy Flyway, cấu hình JVM con và xác nhận `DB_NAME()` trước khi gọi DAO; xóa database/config trong finally. Log tại `target/h2-dao-*/result.log`.

Kịch bản: sinh mã LLV#####, đọc lại dữ liệu; trùng loại ca dù không chồng giờ; ca cả ngày chồng ca sáng/chiều; cho phép khác nhân viên hoặc sáng/chiều tách biệt; cập nhật không tự trùng, cập nhật gây trùng rollback, mã không tồn tại; ngày quá khứ, giờ ngoài ca, khoảng ngày ngược; phân quyền ba vai trò, chưa đăng nhập, tài khoản nhân viên bị khóa; ép lỗi ghi nhật ký để kiểm tra rollback cả dòng lịch và mã đối tượng; hai yêu cầu đồng thời chỉ một yêu cầu tạo cùng ca thành công.

Đây là kiểm chứng kỹ thuật T601. UI phân công/xem/đổi ca/nghỉ phép và kiểm thử chéo H2 còn thuộc T602/T603.
