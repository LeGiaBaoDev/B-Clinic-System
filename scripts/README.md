# Dữ liệu mẫu nghiệp vụ

Đã có bộ bổ sung nhiều tình huống `SeedScenarios.java` + `seed_scenarios.sql` dành cho database đã nạp dữ liệu nền. Xem [DU_LIEU_TINH_HUONG.md](../docs/DU_LIEU_TINH_HUONG.md) về tài khoản, phạm vi, cách chạy thử và kiểm tra chống nạp trùng.

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
