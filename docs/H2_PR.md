# Nội dung PR chuẩn bị sẵn

Tiêu đề: `feat(lich-lam-viec): hoàn thiện lịch cá nhân, phân ca, đổi ca và đơn nghỉ phép`

Nhánh nguồn: feat/h2. Nhánh đích: develop. Chưa mở PR.

## Mô tả

Nhân viên xem ca làm của mình và gửi đơn xin nghỉ; Quản lý phân ca, dời/hoán đổi ca và tiếp nhận đơn. Đơn mới ở trạng thái Chờ duyệt, chưa đổi ca. Khi Quản lý chọn đơn, hệ thống tự kiểm tra lịch hẹn ảnh hưởng; chỉ duyệt khi hợp lệ, từ chối bắt buộc có lý do. Duyệt cập nhật ca, đơn và nhật ký trong cùng transaction.

Lịch tuần/tháng có calendar chọn kỳ và bộ lọc. Phân công chặn ngày quá khứ, ca trùng/nghỉ phép. Đổi ca kiểm tra bản chụp và lịch hẹn, không tự đổi nha sĩ của bệnh nhân. Lịch cá nhân và lịch sử đơn giới hạn theo Actor trong DB cho cả ba vai trò.

V3 thêm DonNghiPhep, giữ V1/V2 và dữ liệu ca cũ. API duyệt trực tiếp trên ca được chặn; dùng mã đơn đã gửi. Không thêm thư viện runtime, không sửa DAO lịch hẹn của Long.

Phạm vi PR là mã nguồn, migration, test và tài liệu; không bao gồm đóng gói JAR/RAR.

## Kiểm chứng

Chạy từ B-Clinic-System:

```powershell
mvn -o -q '-Dbclinic.integration=true' '-Dtest=*,!H1IntegrationTest' test
```

Kết quả và giới hạn: [H2_TEST_REPORT.md](H2_TEST_REPORT.md). Tích hợp SQL Server trên DB UUID riêng kiểm tra quyền, trùng ca/đơn, lịch hẹn phát sinh sau preview, snapshot, rollback và cạnh tranh ghi. Kiểm tra nâng cấp V2→V3 giữ dữ liệu. Ảnh Swing render tại target/uc015-don-cua-toi.png, target/uc015-hop-thu.png; cần đính kèm ảnh khi mở PR.

H1IntegrationTest được loại khỏi lượt này vì lỗi timeout thoát JVM đã biết; không tuyên bố toàn bộ H1 đã được sửa. H2 còn cần thành viên khác thực hiện checklist thủ công, ghi người/ngày/kết quả và approve trước tích hợp.

## Bàn giao

[H2_HANDOFF.md](H2_HANDOFF.md) chốt API, quyền, trạng thái và khóa transaction BClinic:lich cho Long L2. Không commit database.properties, target, bản backup hoặc dữ liệu kiểm thử. Thành viên tự commit/push và mở PR theo quy định Git của dự án.
