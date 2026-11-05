# Bộ dữ liệu 06–09/11/2026

Đã lưu vào BClinic_Dev ngày 05/11/2026. Dữ liệu giả lập dùng cho đồ án; tên/liên hệ không đại diện bệnh nhân thật. Theo lựa chọn người dùng, tương lai chỉ có lịch đặt/hủy, không có phiếu khám hoặc thanh toán trong 06–09/11.

## Các bảng nghiệp vụ

| Bảng | Bổ sung | Trường hợp |
|---|---:|---|
| NhanVien | 4 | 3 nha sĩ, 1 lễ tân; mã NV011–NV014 |
| TaiKhoan | 4 | son.nhasi, hoa.nhasi, tram.nhasi, anh.letan; hash PBKDF2; dùng lại quản lý và tài khoản bị khóa hiện hữu |
| KhachHang | 16 | KH00059–KH00074, tên đầy đủ, SĐT riêng, ngày sinh hợp lệ |
| HoSoBenhAn | 16 | HS00059–HS00074, 1–1 khách; dị ứng, bệnh nền, không ghi nhận bệnh nền |
| LoaiDichVu | Dùng lại 3 | KCT, 5%, 10%; không tạo nhóm trùng |
| DichVu | 3 | DV016–DV018, ba mức thuế; dịch vụ ngừng dùng lại từ danh mục hiện hữu |
| LichLamViec | 36 | Ca cho nha sĩ/lễ tân; thêm 4 ca khi chuyển H4 cũ về quá khứ |
| DonNghiPhep | 3 | Chờ duyệt, đã duyệt gắn ca NGHI_PHEP, từ chối giữ ca được phân công |
| LichHen | 88 | 48 lịch tương lai, 36 lịch quá khứ, 4 lịch nguồn tái khám |
| PhieuKham | 28 | 24 phiếu 01–04/11, 4 phiếu nguồn 30/10–02/11; chờ/đang/hoàn tất, có khách trực tiếp không lịch |
| ChiTietPhieuKham | 32 | Một/nhiều dịch vụ; số lượng 1 và 2, ba mức thuế, lưu giá chỉ định |
| HoaDon | 24 | 12 chưa thanh toán, 8 tiền mặt, 4 chuyển khoản; STT khớp phiếu |
| Voucher | 5 | Giảm tiền; giảm 10% chạm trần 100.000; hết hạn; bắt đầu 08/11; vô hiệu |
| NhatKyHeThong | 5 | 4 mục mô phỏng hoạt động quá khứ và 1 marker nạp dữ liệu chống trùng |

Tài khoản mới dùng mật khẩu mặc định theo QuyTacTaiKhoan, bắt buộc đổi khi đăng nhập. Không thay mật khẩu hoặc quyền tài khoản cũ.

## Lịch tương lai

Mỗi ngày bổ sung 12 lịch: 10 đặt và 2 hủy, gồm tái khám 7 ngày sau phiếu nguồn và dời từ 14:00 sang 16:30. Khách có từ 3 lần hủy chỉ được xếp khung đầu/cuối ngày. Sau cộng lịch có sẵn: 06/07/08 mỗi ngày 11 đặt + 2 hủy; 09 có 12 đặt + 2 hủy. Lịch nằm trong ca, đúng nha sĩ hoạt động, không trùng khách và cách nhau ít nhất 15 phút sau thời lượng dịch vụ.

Khám và thanh toán dùng 01–04/11; thêm nguồn 30/10–02/11 để tái khám 06–09/11 đạt khoảng 7–14 ngày và cùng khách/nha sĩ/dịch vụ. Phiếu chờ/đang khám quá khứ là trạng thái mẫu phục vụ xem màn hình, không khẳng định đây là lượt chăm sóc thực đang diễn ra.

## Điều chỉnh dữ liệu cũ được cho phép

8 lượt H4 có phiếu hoàn tất tương lai 06–09/11 được chuyển về 23–26/10, đồng bộ ngày, mã LH/PK/HD và FK, giữ nội dung/giá/thuế/số lượng. Lịch được sửa thành HOAN_TAT khớp phiếu. Đồng thời sửa 5 mã hóa đơn 02/11 lệch STT phiếu. Không xóa dòng hoặc sửa nội dung điều trị. Các fixture H4 ngoài 06–09/11 không thuộc lượt bảo trì này.

## Kiểm chứng và chạy lại

Backup COPY_ONLY/CHECKSUM đã VERIFYONLY: `BClinic_Dev_before_four_days_20261105_201551.bak` trong thư mục Backup mặc định SQL Server. Log ở target/four-days-backup.log.

`Run-SeedFourDays.ps1` preview mặc định rollback; `-Apply` để lưu. Có marker chống trùng. `verify_four_days.sql` kiểm thời gian, ca, trùng lịch/15 phút, hủy nhiều lần, nguồn tái khám, giá/thuế, hiệu lực và giảm voucher, mã HD/PK và ràng buộc DB. Log apply/verify/idempotent ở target/four-days-*.log. Không dùng normalize_identifiers để xóa khoảng trống HD: mã HD phải theo phiếu, kể cả phiếu chưa sinh HD.

Đây là tập các tình huống nghiệp vụ hợp lệ đã liệt kê, không phải kiểm chứng mọi tổ hợp lỗi hoặc đồng thời. Các dữ liệu cố ý sai như lịch trùng, giá âm, thanh toán hai lần không được lưu vào DB.
