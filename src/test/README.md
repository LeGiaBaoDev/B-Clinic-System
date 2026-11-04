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

## H2 — UC013: phân công lịch làm việc

Quản lý mở **Lịch làm việc**, chọn nhân viên đang hoạt động, ngày và ca rồi nhấn **Lưu phân công**. Giờ theo ca: sáng 08:00–12:00, chiều 13:30–17:30, cả ngày 08:00–17:30. Bảng có chế độ tuần/tháng, chuyển kỳ, Hôm nay và Làm mới/F5. Sau lưu thành công, bảng chuyển về kỳ của ngày vừa phân công và tải lại dữ liệu.

```powershell
mvn '-Dtest=PnlLichLamViecTest' test
mvn '-Dbclinic.integration=true' '-Dtest=H2DaoIntegrationTest' test
```

Bài tích hợp H2 bổ sung thao tác trên panel thật: chọn nha sĩ/ngày/ca chiều, lưu qua DAO và đọc lại SQL Server, xác nhận dòng lịch được hiển thị; lưu trùng lần hai và kiểm tra không sinh thêm ca. Chạy trên database UUID riêng như T601.

Checklist kiểm thử chéo UC013 (chưa nghiệm thu thủ công):

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| H2-M01 | Quản lý mở Lịch làm việc, chuyển tuần/tháng và Làm mới/F5 | Hiển thị đúng khoảng ngày và ca đã có; không đứng giao diện |
| H2-M02 | Lọc vị trí, chọn nhân viên và tạo ca mới | Chỉ nhân viên có tài khoản hoạt động được chọn; giờ đúng ca; báo thành công, bảng có ca mới |
| H2-M03 | Lưu lại cùng nhân viên/ngày/ca | Báo trùng ca, giữ lựa chọn để sửa; không tạo ca thứ hai |
| H2-M04 | Chọn ngày quá khứ hoặc nhập 31/02 | Báo lỗi ngày, không gửi lưu |
| H2-M05 | Nhấn Lưu nhiều lần khi đang xử lý | Chỉ một yêu cầu; không sửa nhân viên/ngày/ca trong lúc lưu |
| H2-M06 | Đăng nhập Nha sĩ/Lễ tân | Không có chức năng phân công trong menu; DAO vẫn kiểm tra quyền |
| H2-M07 | Thu nhỏ cửa sổ về 1366×768 hoặc kích thước tối thiểu | Các nút dùng được, form cuộn dọc, bảng cuộn ngang khi cần |

UC012 xem lịch cá nhân, UC014 đổi ca và UC015 nghỉ phép chưa thuộc phần UI đã triển khai ở lượt này.

### Cập nhật giao diện lịch theo ảnh tham chiếu

- Mặc định mở bảng tuần: cột thứ Hai–Chủ nhật, hàng ca sáng/chiều; ngày hiện tại được tô nền nhẹ. Mỗi thẻ ghi tên, chức vụ từ vai trò tài khoản và trạng thái ca. Không có thông tin ghế.
- Bộ lọc nhân viên, chức vụ, trạng thái áp dụng lên lịch. Ca cả ngày xuất hiện ở cả hai buổi; ca nghỉ phép giữ nhãn riêng.
- Nút + Phân ca mở/đóng form bên cạnh, giữ luồng lưu nền hiện có. Chế độ Tháng giữ danh sách ca trong tháng.
- Đã qua ca/Đang trực/Đã phân ca là nhãn hiển thị dựa trên giờ ca; không ghi thêm trạng thái vào DB. Chủ nhật không có ca hiển thị Chưa phân ca, không tự giả định nghỉ cố định.

Ngày phân công dùng calendar (nút lịch): chỉ chọn từ hôm nay trở đi, ô ngày không nhập tay. Ngày trống hoặc quá khứ vẫn bị kiểm tra lại khi Lưu. Khi mở lại form, giới hạn ngày được cập nhật theo hôm nay.

### Chặn ca đã có lịch/nghỉ phép ngay trên form

Khi chọn nhân viên, ngày hoặc ca, form đối chiếu lịch đã tải bằng mã nhân viên. Nếu cùng loại ca hoặc chồng giờ với ca đã phân công/nghỉ phép, hiện lý do và khóa Lưu. Đổi sang ca còn trống hoặc nhân viên khác sẽ cho lưu lại. Nghỉ ca sáng không tự chặn ca chiều; nghỉ cả ngày chặn cả hai buổi. Thẻ nghỉ phép có nền và nhãn riêng.

DAO vẫn kiểm tra trong transaction để chặn dữ liệu mới phát sinh ở máy khác sau lúc tải. Thông báo nghỉ phép được ưu tiên thay cho thông báo trùng ca chung. Bài H2DaoIntegrationTest kiểm tra chặn cả giao diện và DAO trên DB UUID riêng.

### Luồng chọn ca trước ngày

Form tạo ca: nhân viên → chọn ca làm việc → chọn ngày còn trống trên calendar. Ban đầu chưa chọn ca nên calendar chưa bật. Các ngày có ca trùng hoặc nghỉ phép của đúng nhân viên/ca được vô hiệu hóa trực tiếp trong popup; đổi nhân viên hoặc ca sẽ cập nhật lại. Ngày quá khứ vẫn bị khóa. Kiểm tra ở nút Lưu và transaction DAO tiếp tục bảo vệ trường hợp dữ liệu thay đổi sau lúc tải.

Lịch tuần chỉ hiển thị tên và chức vụ trên thẻ: nghỉ phép nền vàng, đang trực nền xanh, ca còn lại trung tính. Đã bỏ dòng trạng thái dưới ô và thanh chú thích. Có thể xem chi tiết trạng thái khi rê chuột hoặc dùng bộ lọc.

Màu đang trực ở lịch tuần tính theo từng buổi, kể cả ca cả ngày: sáng kết thúc lúc 12:00, chiều bắt đầu 13:30 và kết thúc tối đa 17:30. Màu tự cập nhật mỗi phút khi màn hình đang hiển thị. Sau buổi sáng, thẻ sáng trở về trung tính; nghỉ phép luôn nhấn vàng.

## UC012 — Lịch của tôi (Quản lý, Nha sĩ, Lễ tân)

Đăng nhập một trong ba vai trò rồi chọn **Lịch của tôi** trên sidebar. Mặc định là tuần hiện tại; dùng Tuần/Tháng, trước/sau, Hôm nay, bộ lọc trạng thái và Làm mới/F5. Màn hình chỉ đọc, không có nút phân công hoặc chọn nhân viên khác. Quản lý vẫn có mục Lịch làm việc riêng cho toàn phòng khám; Lịch trực của Nha sĩ vẫn phục vụ lịch hẹn, tách với lịch ca cá nhân.

`LichLamViecDAO.layLichCaNhan()` lấy mã nhân viên từ Actor xác thực trong DB, không nhận mã nhân viên từ UI. API vẫn giới hạn lịch bản thân đối với Quản lý. Test H2 tích hợp kiểm tra cả ba vai trò, giả mã nhân viên session, tài khoản bị khóa/chưa đăng nhập và panel chỉ đọc trên database UUID riêng.

```powershell
mvn '-Dtest=PnlLichCaNhanTest' test
mvn '-Dbclinic.integration=true' '-Dtest=H2DaoIntegrationTest' test
```

Checklist kiểm thử chéo (chưa nghiệm thu thủ công):

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| H2-M08 | Quản lý chọn Lịch của tôi, rồi Lịch làm việc | Cá nhân chỉ có ca của mình; màn quản lý vẫn xem/phân công toàn bộ |
| H2-M09 | Nha sĩ chọn Lịch của tôi rồi quay lại Lịch trực | Cá nhân chỉ có ca của mình; Lịch trực giữ lịch hẹn/khám hiện có |
| H2-M10 | Lễ tân chọn Lịch của tôi rồi Bệnh nhân/Tiếp nhận | Cá nhân chỉ có ca của mình; các chức năng cũ vẫn dùng được |
| H2-M11 | Đổi tuần/tháng, Hôm nay, lọc Nghỉ phép, F5 | Đúng kỳ, đúng ca/nghỉ phép cá nhân; không sửa được dữ liệu |
| H2-M12 | Tài khoản chưa có ca trong kỳ | Hiển thị chưa có ca, không nhầm lỗi tải dữ liệu |

Calendar trên thanh điều hướng dùng để xem lịch: chọn ngày sẽ mở tuần/tháng chứa ngày đó, cho phép xem quá khứ. Nút trước/sau và Hôm nay đồng bộ với ngày đang chọn; calendar phân công vẫn khóa ngày không hợp lệ theo ca.
