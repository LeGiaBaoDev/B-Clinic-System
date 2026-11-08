# Dữ liệu mẫu nghiệp vụ

Backup/restore H5 đã được triển khai trong ứng dụng (menu quản lý). Hướng dẫn tại `../../docs/H5/HUONG_DAN_BACKUP_RESTORE.md`; `Run-AutoBackup.ps1` hỗ trợ Windows Task Scheduler khi app đóng. Không dùng script seed để thay thế chức năng restore.

## Demo lúc 13:00 ngày 06/11/2026

Đã nạp ảnh chụp MÔ PHỎNG theo mốc báo cáo: sáng có khám/thu tiền, chiều có lịch tiếp nhận; ưu tiên hieu.quanly/hieu.letan/hieu.nhasi. Seed bằng Run-SeedReportNoon.ps1, kiểm SQL verify_report_noon.sql và DAO VerifyReportNoon.java. Hướng dẫn tài khoản, màn hình, mã phiếu/lịch và số liệu tại `../../docs/DEMO_06_11_2026.md` (ngoài repo). Marker chống nạp trùng; không reset thao tác demo; không phải migration Flyway.

## Bộ dữ liệu 4 ngày 06–09/11/2026

Đã nạp bộ dữ liệu theo thời gian thực: tương lai chỉ đặt/hủy; các tình huống khám và thanh toán nằm ở quá khứ. Chi tiết bảng, tài khoản, tình huống và kiểm chứng tại [FOUR_DAYS_SCENARIOS.md](FOUR_DAYS_SCENARIOS.md). Chạy bằng `Run-SeedFourDays.ps1` (preview) / `-Apply`; xác minh bằng `verify_four_days.sql`. Marker ngăn nạp trùng. Mã hóa đơn phải khớp phiếu; có thể có khoảng trống khi phiếu chưa sinh hóa đơn.

## Chuẩn hóa mã — 05/11/2026

Đã áp dụng `normalize_identifiers.sql` trên BClinic_Dev sau backup COPY_ONLY/CHECKSUM và RESTORE VERIFYONLY. Kết quả: NV001–NV010, TK001–TK010, KH00001–KH00058, HS00001–HS00058, DV001–DV015, VC001–VC012, LLV00001–LLV00134. LH/PK/HD giữ tiền tố và ngày, số thứ tự liên tục trong từng ngày. LDV/DNP vốn liên tục nên giữ nguyên; mã nhật ký và identity chi tiết phiếu không đánh lại.

Mọi FK được cập nhật theo metadata thực tế, gồm tài khoản, ca trực, đơn nghỉ, lịch, phiếu, chi tiết, hóa đơn và nhật ký. Transaction khóa các bảng, dùng mã tạm để tránh va chạm, tạm tắt rồi bật và kiểm chứng lại FK/CHECK/trigger. Đối chiếu mọi cột/mọi dòng với bản đồ mã; tên, mật khẩu, quyền, trạng thái, tiền và nội dung điều trị giữ nguyên. Nội dung văn bản lịch sử có nhắc mã cũ vẫn giữ để tra cứu bằng bảng đối chiếu.

`normalize_identifiers.sql` mặc định `@Apply=0` (rollback). Chỉ đổi thành 1 khi bảo trì và đã có backup; đóng ứng dụng trước khi chạy. `Invoke-DevSql.ps1` đọc cấu hình local, chỉ cho phép BClinic_Dev và không in thông tin đăng nhập. Kiểm tra sau lưu bằng `verify_identifier_normalization.sql` và `verify_normalized_data.sql`; chạy preview lần nữa phải có 0 mã cần đổi.

Các script seed/verify_demo/verify_scenarios/verify_h4 cũ dùng mã fixture cố định trước chuẩn hóa; không dùng chúng để xác nhận DB đã đổi mã hoặc nạp bổ sung vào DB này. Marker seed lịch sử giữ nguyên để ngăn nạp lại. Với DB mới, seed trước rồi chuẩn hóa mã cuối cùng. Bảng đối chiếu hiện hành và log xác minh ở `target/identifier-apply.log`, `identifier-verify.log`, `identifier-idempotent.log`; backup được ghi tại đường dẫn server trong `target/identifier-backup.log`.

## Chuẩn hóa dữ liệu hiện hữu — 05/11/2026

Đã chạy `normalize_demo_data.sql` trên BClinic_Dev: 8 khách KH88001–KH88008 dùng họ tên đầy đủ, email/địa chỉ theo hồ sơ; 6 tài khoản đổi từ demo.* sang `huong.nhasi`, `nam.nhasi`, `lan.nhasi`, `an.nhasi`, `vy.nhasi`, `ha.letan`. Mật khẩu không đổi, `vy.nhasi` vẫn khóa. Các tài khoản hieu.* và anh.quanly giữ tên đăng nhập cũ. Chuyên môn/vị trí nhân viên thống nhất Quản lý/Nha sĩ/Lễ tân theo quyền.

Script mặc định preview rollback; chỉ đổi @Preview=0 để lưu sau khi kiểm tra. Kiểm tra đúng DB, tên cũ và xung đột trước cập nhật; marker nhật ký ngăn chạy lại. Không đổi mã, ngày sinh, SĐT, giới tính, hash mật khẩu, quyền hoặc trạng thái; không sửa bệnh án/chi tiết điều trị/hóa đơn/lịch sử cũ. `verify_normalized_data.sql` chỉ đọc, xác nhận tên/mã/SĐT/tài khoản/vị trí và số lượng.

Đây vẫn là dữ liệu mẫu dùng họ tên và địa chỉ hợp lý, không phải thông tin đã xác minh của bệnh nhân thật; email giữ miền example.com. Hai script seed thủ công H4/scenarios đã cập nhật tên/tài khoản mới cho lần nạp mới, không sửa Flyway V1–V3. Bộ H4 cũ nhận biết bằng mã/ghi chú, không phụ thuộc tên khách mới.

## Bổ sung H4 — 05 đến 12/11/2026

`seed_h4_dashboard.sql` dành riêng cho **BClinic_Dev** đang có dữ liệu nền. Mặc định `@Preview=1` thực hiện trong transaction rồi rollback; kiểm tra kết quả trước khi đổi thành 0 để lưu. Không đặt trong Flyway migration. `verify_h4_dashboard.sql` chỉ đọc để kiểm tra sau khi lưu.

- Thêm 8 khách có họ tên đầy đủ/hồ sơ (`KH88001`–`KH88008`), 8 ca NV103, 29 lịch, 19 phiếu và 17 hóa đơn. Không thay tài khoản/mật khẩu hoặc dữ liệu cũ. Dừng khi mã/ca xung đột; lỗi rollback toàn bộ.
- Hôm nay 05/11: 8 lịch đủ 5 trạng thái, 3 hóa đơn với tạm tính 600.000 ₫, đã trả 2 hóa đơn tổng 350.000 ₫, chưa trả 1 hóa đơn 250.000 ₫; có phiếu đang khám/chờ khám.
- 06–12/11: mỗi ngày 3 lịch và 2 hóa đơn **mô phỏng tương lai chưa trả**, tạm tính từ 390.000 đến 630.000 ₫. Đây là fixture để xem trước dashboard, không phải giao dịch khám thực hoặc khoản đã thu. Không có thanh toán tương lai giả.
- Giá 150.000–340.000 ₫ trên từng dòng là giá giả lập có chủ đích, không sửa bảng giá dịch vụ. Nha sĩ dùng NV103, không đụng ca nghỉ NV002 hoặc các ca đã có của nha sĩ khác.
- Ghi chú `H4_DEMO_20261105` trên lịch/phiếu/hồ sơ và marker nhật ký `NK880005` ngăn nạp trùng. Chạy lại không thêm. Các tổng trên chỉ là phần mới; màn hình có thể cộng dữ liệu cũ cùng ngày (ví dụ lịch 09/11 đã có từ trước).
- Đã preview rollback, lưu và xác minh trực tiếp ngày 05/11/2026. Chọn ngày trên Tổng quan rồi F5 để xem. Không tự chạy seed lúc mở ứng dụng hoặc dịch ngày dữ liệu đã lưu.

Đã có bộ bổ sung nhiều tình huống `SeedScenarios.java` + `seed_scenarios.sql` dành cho database đã nạp dữ liệu nền.

`seed_demo.sql` là script chạy thủ công trên **BClinic_Dev**, sau khi ứng dụng chạy Flyway V1/V2. Mở file bằng SSMS, chọn đúng database, chạy toàn bộ một lần. Sau đó chạy `verify_demo.sql` để kiểm tra. Không đưa các file này vào thư mục migration.

Script giữ nguyên nhân viên, tài khoản và mật khẩu hiện có. Yêu cầu NV001/TK001 là quản lý, NV002/TK002 là nha sĩ, NV003/TK003 là lễ tân; các bảng nghiệp vụ phải trống. Nếu có dữ liệu, script dừng thay vì ghi đè. Mọi thao tác thêm nằm trong một transaction; lỗi sẽ rollback. Dấu `DEMO_SEED_V1` trong nhật ký ngăn nạp trùng khi chạy lại.

## Các tình huống

- 3 loại dịch vụ theo mức thuế trong schema: KCT (giá trị lưu 0), 5%, 10%; 10 dịch vụ, gồm 1 dịch vụ đã ngừng.
- 20 khách hàng giả lập, mỗi người có 1 hồ sơ; có tiền sử dị ứng và bệnh nền để kiểm thử hiển thị. Email example.com; không phải thông tin bệnh nhân thật.
- Lịch làm việc cho ba nhân viên vào các ngày có lịch hẹn; một ca nghỉ phép của nha sĩ có lý do, không nhận lịch hẹn.
- 12 buổi điều trị lịch sử cách nhau 7 ngày; 2 phiếu hôm nay (chờ/đang khám), 3 lịch tương lai (có 1 tái khám), 3 lịch đã hủy cùng khách trong tháng trước, 1 không đến.
- Chi tiết được thêm trước khi khóa phiếu hoàn tất. Giá/thuế được lưu theo từng dòng; có dòng vật tư số lượng 2.
- 12 hóa đơn: 10 đã thanh toán (cả tiền mặt và chuyển khoản), 2 chưa thanh toán; chỉ lịch đã trả tiền chuyển HOAN_TAT.
- 6 voucher: giảm tiền, giảm phần trăm có/không giới hạn; hết hạn, chưa hiệu lực và vô hiệu. Voucher dùng trên hóa đơn luôn có hiệu lực lúc lập.
- Tái khám điều trị tủy sau 14 ngày, cùng khách hàng, dịch vụ, nha sĩ và phiếu nguồn đã hoàn tất.
- 5 bản ghi nhật ký được đánh dấu giả lập, gồm dấu hoàn tất seed. Không đại diện cho hành vi thực của người dùng.

Ngày gốc lấy theo múi giờ Việt Nam tại lần nạp đầu tiên; không dịch chuyển dữ liệu mỗi lần khởi động. Các trạng thái hôm nay là tình huống demo, không phải số liệu vận hành thực tế. Nếu dùng lại vào ngày khác, lịch tương lai cũ cần được xử lý theo nghiệp vụ. Không chạy lại seed để reset dữ liệu.

## Phạm vi kiểm chứng

Đối chiếu schema V1, entity hiện có và tài liệu DATABASE/business-rules/TASKS. Không có bản báo cáo DOCX gốc trong workspace để đối chiếu đủ mọi yêu cầu. Mức thuế và công thức giảm tiền theo đặc tả đồ án hiện tại, không thay đổi quy định thuế. Script không triển khai DAO/UI hoặc thay đổi cơ chế mật khẩu; T105 vẫn là công việc riêng. Dữ liệu mẫu không tự xuất hiện trên máy thành viên khác khi pull code.
