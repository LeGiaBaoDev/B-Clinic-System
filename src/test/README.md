## H3 — T501: nền tảng voucher (05/11/2026)

Kiểm thử entity không cần SQL Server:

```powershell
mvn -o '-Dtest=VoucherTest' test
```

Kiểm thử DAO trên SQL Server riêng:

```powershell
mvn -o '-Dbclinic.integration=true' '-Dtest=VoucherTest,H3VoucherIntegrationTest' test
```

- Cần quyền tạo/xóa database test. Test tạo `BClinic_Test_<uuid>`, migrate V1–V3, chạy DAO trong JVM con có cấu hình riêng và xác nhận `DB_NAME()` trước khi thao tác. Database tạm và file cấu hình được xóa trong `finally`; không migrate hoặc nạp dữ liệu vào DB ứng dụng.
- Log: `target/h3-voucher-*/result.log`. Khi bật cờ integration, lỗi kết nối/quyền/test là lỗi thật; không tự skip. `mvn test` mặc định bỏ qua test tích hợp này.
- Entity: tên/mã, giá trị giảm và giới hạn, chặn NaN/vô cực, đổi loại giảm, thứ tự ngày, hiệu lực gồm hai đầu, vô hiệu, tính giảm không vượt tổng.
- DAO: sinh `VC###`, thêm/đọc/cập nhật/vô hiệu, `giaTriGiamToiDa` NULL ↔ 0, lọc thời hạn/trạng thái, mã không tồn tại; Quản lý/Lễ tân/Nha sĩ, chưa đăng nhập, giả vai trò session và tài khoản khóa; giữ tham chiếu/số giảm trên hóa đơn; rollback thêm/sửa/vô hiệu khi nhật ký lỗi, không gán mã trước commit và thử lại; bốn yêu cầu thêm đồng thời không trùng mã.
- API giữ nguyên: Quản lý dùng `layTatCa`, `them`, `capNhat`, `xoa`; Quản lý/Lễ tân dùng `layTheoMa`, `layConHieuLuc`. `xoa` là vô hiệu, không xóa vật lý. Voucher đã gắn vào bất kỳ hóa đơn nào không được cập nhật nội dung, kể cả hóa đơn chưa thanh toán. Các thao tác ghi dùng chung khóa `BClinic:thanh-toan` với HoaDonDAO.
- T501 là nền tảng DAO; chưa có UI UC036–038, chưa nghiệm thu toàn H3 hoặc kiểm thử chéo. Kiểm thử này chưa thay thế kiểm thử thanh toán UC025 toàn luồng tại T503.

## Hồ sơ chốt kỹ thuật H2 (04/11/2026)

- [Báo cáo và phiếu kiểm thử chéo](../../docs/H2_TEST_REPORT.md)
- [Hợp đồng API/bàn giao Long L2](../../docs/H2_HANDOFF.md)
- [Nội dung PR chuẩn bị sẵn](../../docs/H2_PR.md)

165 test đạt trong tập hồi quy H2 (chỉ chạy test, không đóng gói JAR/RAR); H1IntegrationTest timeout cũ được loại rõ ràng. Các mục bên dưới lưu chi tiết theo từng lượt triển khai; luồng hiện hành xem hồ sơ bàn giao. Chưa đánh dấu kiểm thử chéo hoặc PR merge đã hoàn tất.

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

Ghi chú lịch sử của lượt UC013 ban đầu: UC012/014/015 khi đó chưa triển khai UI. Hiện cả UC012–015 đã có; xem hồ sơ chốt H2 ở đầu tài liệu.

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

## UC014 — Đổi ca làm việc (Quản lý)

Trong **Lịch làm việc**, nhấn **Đổi ca**:

1. Chọn ca cần đổi (chỉ ca đã phân công, chưa bắt đầu, nhân viên còn hoạt động).
2. Chọn **Dời ngày / ca làm** rồi chọn ca mới và ngày còn trống, hoặc **Hoán đổi hai nhân viên** rồi chọn ca của nhân viên cùng chức vụ.
3. Nhấn **Kiểm tra và đổi ca**. Nếu hợp lệ, xem tóm tắt và xác nhận. Lưu kiểm tra lại để chặn dữ liệu đã thay đổi sau bước kiểm tra.

Không đổi ca nghỉ phép, ca đã bắt đầu, ca trùng/nghỉ phép ở thời gian mới hoặc ca làm mất khoảng trực phục vụ lịch hẹn DA_DAT/DA_DEN. Hoán đổi giữ mã ca và nhân viên của từng bản ghi, đổi khung ngày/giờ của hai người trong cùng transaction; lỗi ở bất kỳ bước nào rollback cả hai ca và nhật ký. Không tự chuyển bệnh nhân sang nha sĩ khác.

Sau đổi, người liên quan thấy thông báo cập nhật riêng khi mở/Làm mới **Lịch của tôi**. Đây là thông báo trong ứng dụng lấy từ thay đổi đã commit, không có thông báo đẩy ngoài ứng dụng.

```powershell
mvn '-Dtest=DlgDoiCaTest' test
mvn '-Dbclinic.integration=true' '-Dtest=H2DaoIntegrationTest' test
```

Checklist kiểm thử chéo (chưa nghiệm thu thủ công):

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| H2-M13 | Dời một ca chưa bắt đầu sang ngày/ca trống | Kiểm tra → xác nhận → thành công; mã ca/nhân viên giữ nguyên |
| H2-M14 | Hủy hộp xác nhận | Không đổi dữ liệu, form vẫn dùng được |
| H2-M15 | Hoán đổi hai người cùng chức vụ | Hai khung ngày/giờ đổi cho nhau, không đổi một bên |
| H2-M16 | Chọn ca vướng lịch hẹn, nghỉ phép hoặc ca đã có | Báo lý do, không ghi thay đổi |
| H2-M17 | Hai cửa sổ cùng đổi một ca | Yêu cầu dùng dữ liệu cũ bị từ chối; tải lại trước khi đổi tiếp |
| H2-M18 | Nha sĩ/Lễ tân mở Lịch của tôi | Không có nút Đổi ca; nhận thông báo riêng nếu ca đã đổi |

Form Đổi ca có bộ lọc riêng cho nguồn và đích: chức vụ, ca, ngày, tên/mã NV (không phân biệt dấu/hoa thường). Nhấn Lọc hoặc Enter ở ô tìm; Xóa lọc để trở lại. Ca đích luôn giữ giới hạn cùng chức vụ và khác nhân viên nguồn; không có kết quả sẽ khóa nút đổi ca.

## UC015 — Nhân viên xin nghỉ, Quản lý tiếp nhận và xử lý

Luồng hiện hành theo yêu cầu điều chỉnh ngày 04/11/2026; thay thế luồng Quản lý tự nhập yêu cầu nghỉ trước đây.

1. Nhân viên (Nha sĩ/Lễ tân, cả Quản lý có ca cá nhân) vào **Lịch của tôi → Xin nghỉ phép / Đơn của tôi**. Chọn ca đã được phân công, nhập lý do 1–255 ký tự rồi gửi. Hệ thống lưu đơn **Chờ duyệt**, chưa đổi ca và chưa chặn đặt lịch hẹn.
2. Quản lý vào **Lịch làm việc → Đơn nghỉ phép**. Mặc định xem Chờ duyệt; có thể lọc theo trạng thái, ngày nghỉ, tên/mã nhân viên hoặc mã đơn. Chọn đơn để xem lý do nhân viên đã gửi (chỉ đọc).
3. **Duyệt**: chọn đơn để tự động kiểm tra lịch ảnh hưởng. Nếu còn lịch hẹn Đã đặt/Đã đến, phối hợp Lễ tân dời/hủy rồi kiểm tra lại. Sau xác nhận, đơn và ca cập nhật cùng transaction; chỉ lúc này ca chuyển Nghỉ phép, hiển thị vàng. Hệ thống kiểm tra lại khi lưu để chặn lịch hẹn mới phát sinh.
4. **Từ chối**: nhập lý do từ chối 1–255 ký tự, xác nhận. Đơn lưu người/thời điểm/lý do xử lý; ca giữ nguyên. Đơn có ca đã đổi hoặc kết thúc vẫn có thể từ chối để đóng yêu cầu cũ.
5. Nhân viên mở lại hoặc làm mới hộp đơn để xem kết quả, người xử lý, thời điểm và lý do từ chối. Có thể gửi lại sau khi bị từ chối nếu ca vẫn hợp lệ. Mỗi ca chỉ có một đơn chờ.

Đơn lưu bản chụp ngày/ca lúc gửi. Nếu ca bị dời/hoán đổi sau đó, Quản lý không được duyệt nhầm ca mới; cần từ chối đơn cũ và nhân viên gửi lại. Nhân viên không gửi hộ hoặc xem đơn người khác; Quản lý cũng chỉ thấy đơn của mình ở hộp cá nhân. Quyết định trên hộp quản lý được kiểm tra quyền trong DB, không dựa vào nút UI.

Flyway V3 thêm bảng DonNghiPhep và ràng buộc; không sửa V1/V2, không tạo đơn giả cho ca nghỉ cũ. Khởi động lại ứng dụng để migration chạy trước khi thử chức năng. Test dùng DB UUID riêng, không migrate DB ứng dụng.

```powershell
mvn '-Dtest=DlgNghiPhepTest' test
mvn '-Dbclinic.integration=true' '-Dtest=H2DaoIntegrationTest' test
```

Checklist kiểm thử chéo (chưa nghiệm thu thủ công):

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| H2-M19 | Nha sĩ gửi đơn từ Lịch của tôi | Đơn chờ duyệt, ca giữ nguyên; xuất hiện trong hộp quản lý |
| H2-M20 | Quản lý kiểm tra ca có lịch Đã đặt/Đã đến | Hiện danh sách lịch ảnh hưởng, khóa Duyệt; sau xử lý lịch hẹn kiểm tra lại được |
| H2-M21 | Nhập lý do từ chối rồi xác nhận | Ca giữ nguyên; nhân viên thấy kết quả và lý do; có thể gửi lại |
| H2-M22 | Ca bị dời sau khi nhân viên gửi đơn | Duyệt bị chặn; từ chối đơn cũ được |
| H2-M23 | Lý do rỗng, >255; ca kết thúc; gửi trùng đơn chờ | Không cho gửi/xử lý sai |
| H2-M24 | Nha sĩ/Lễ tân mở hộp cá nhân | Chỉ thấy đơn bản thân; không có nút duyệt/từ chối |
| H2-M25 | Duyệt nghỉ sáng rồi thử đặt khám sáng/chiều | Sáng bị chặn; chiều vẫn được nếu có ca và khung giờ hợp lệ |
| H2-M26 | Hai người duyệt đồng thời / hủy xác nhận | Chỉ một quyết định được lưu / hủy không ghi dữ liệu |
| H2-M27 | Quản lý gửi đơn của bản thân | Có trong hộp cá nhân và hộp tiếp nhận; dữ liệu tách theo quyền xem |

API ca trực bàn giao cho Long L2: lịch DA_PHAN_CONG mới dùng để đặt lịch, NGHI_PHEP không dùng; dùng khóa nghiệp vụ BClinic:lich trong transaction đặt/dời lịch. Đơn CHO_DUYET không đổi trạng thái ca. H2 integration đã gọi LichHenDAO.them thật để kiểm chứng hợp đồng này; không đổi API/file thuộc Long. API duyệt trực tiếp trên ca cũ bị chặn, dùng DonNghiPhepDAO.duyet(maDon)/tuChoi(maDon,lyDo).

### Sửa hiển thị đơn sau khi gửi (04/11/2026)

Sau khi gửi thành công, hộp cá nhân xóa bộ lọc ngày/tìm kiếm/trạng thái, tải lại và tự chọn đúng đơn vừa gửi với trạng thái Chờ duyệt. Nút Làm mới ở hộp cá nhân trả về tất cả đơn của người đang đăng nhập; hộp quản lý vẫn giữ bộ lọc khi làm mới. Bộ lọc ngày có nhãn Ngày nghỉ, không lọc theo ngày gửi. Nếu đơn bị bộ lọc che, thông báo nêu rõ có đơn nhưng không khớp bộ lọc.

Kiểm thử hồi quy: gửi khi đang lọc ngày khác + từ khóa không khớp + trạng thái Từ chối; đơn mới vẫn hiện Chờ duyệt và được chọn. Làm mới hộp cá nhân khi lọc ngày không khớp phải hiện lại đơn đã gửi.

### Bố cục và kiểm tra tự động UC015 (04/11/2026)

Nhân viên: khối Tạo đơn ở trên (ca hợp lệ, lý do, gửi); bên dưới là Đơn nghỉ phép của tôi, lọc trạng thái/ngày nghỉ, tìm mã đơn, Xóa lọc và Làm mới. Màu trạng thái: chờ duyệt vàng, đã duyệt xanh, từ chối đỏ nhạt; nền/thẻ/nút theo UiConstants. Các quy tắc gửi đơn và quyền xem giữ nguyên.

Quản lý: chọn đơn chờ là tự kiểm tra lịch hẹn bằng SwingWorker, bỏ nút kiểm tra thủ công. Trong lúc kiểm tra khóa hành động; chỉ bật Duyệt khi kiểm tra thành công và không có lịch ảnh hưởng. Lỗi/ca đã thay đổi vẫn cho từ chối kèm lý do. Chọn lại đơn hoặc Làm mới rồi chọn đơn để kiểm tra lại sau khi xử lý lịch hẹn. Khi duyệt, DAO tiếp tục revalidate trong transaction.
