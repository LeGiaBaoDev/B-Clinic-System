# Bộ dữ liệu nhiều tình huống — BClinic_Dev

Đợt bổ sung được nạp ngày **02/11/2026**, không sửa/xóa dữ liệu cũ và không đổi migration V1/V2. Đây là dữ liệu giả lập phục vụ phát triển/kiểm thử, được dashboard đọc từ database như mọi bản ghi khác.

## Tình huống có sẵn

| Nhóm | Trường hợp |
|---|---|
| Nhân viên/tài khoản | Quản lý, lễ tân, nhiều nha sĩ; một tài khoản nha sĩ khóa; tài khoản mới PBKDF2 có salt riêng |
| Ca làm việc | CA_SANG, CA_CHIEU, CA_NGAY; nghỉ phép có lý do; ca sáng kết thúc nên ngoài ca buổi chiều; không trùng ca |
| Lịch hẹn | DA_DAT, DA_DEN, HOAN_TAT, DA_HUY, KHONG_DEN; khách hủy ba lần trong tháng; lần đặt tiếp theo ở đầu ngày |
| Khám | CHO_KHAM, DANG_KHAM, HOAN_TAT; một/multiple dòng dịch vụ; ảnh chụp giá và thuế; không sửa phiếu đã khóa |
| Tái khám | Lịch tương lai nối phiếu nguồn hoàn tất, cùng bệnh nhân/dịch vụ/nha sĩ, sau 8 ngày |
| Thuế | Điều trị KCT (lưu 0), vật tư 5%, thẩm mỹ 10%; vật tư số lượng 2 |
| Hóa đơn | Tiền mặt/chuyển khoản, chưa trả, công nợ cũ, hóa đơn nhiều dòng, giảm hết tiền nên tổng bằng 0 |
| Voucher | Giảm tiền, giảm phần trăm có trần/không trần; còn hạn, hết hạn, chưa hiệu lực, bị vô hiệu; không dùng voucher sai hiệu lực trên hóa đơn |
| Doanh thu | Có giao dịch từng ngày từ 20/10 đến 02/11/2026; mức thu thay đổi, đủ dữ liệu so hai kỳ 7 ngày |
| Hồ sơ | Có dị ứng, tăng huyết áp, đái tháo đường và không ghi nhận bệnh nền; email example.com |

## Tài khoản mẫu mới

Mật khẩu gốc dùng thử: `Clinic@123`. Không đổi mật khẩu của ba tài khoản ban đầu.

| Tên đăng nhập | Mã NV | Vai trò/tình huống |
|---|---|---|
| demo.huong | NV101 | Nha sĩ Lê Mai Hương; cả ngày hôm nay, ca sáng các ngày khác; có phiếu đang khám |
| demo.nam | NV102 | Nha sĩ Trần Hoàng Nam; ca chiều; có lịch đã khám và lịch tương lai |
| demo.lan | NV103 | Nha sĩ Nguyễn Ngọc Lan; nghỉ phép ngày nạp và ngày kế tiếp |
| demo.an | NV104 | Nha sĩ Phạm Quốc An; ca sáng, rảnh trong ca, ngoài ca buổi chiều |
| demo.vy.locked | NV105 | Tài khoản bị khóa, đăng nhập phải bị từ chối; không được phân lịch |
| demo.thuha | NV106 | Lễ tân/thu ngân, ca cả ngày |

Đọc cùng dữ liệu bằng Clock ngày nạp lúc **16:45** đã kiểm chứng đủ bốn trạng thái: NV101/NV002 đang khám, NV102 rảnh trong ca, NV103 nghỉ phép, NV104 ngoài ca. Đây là kiểm thử logic trạng thái với clock cố định, không phải hệ thống lưu lịch sử đầy đủ mọi lần chuyển trạng thái. Khi mở ứng dụng ngoài giờ, các nha sĩ không có phiếu đang khám sẽ hiển thị ngoài ca; không kéo ca sang ban đêm để ép hiển thị rảnh.

## Số lượng sau bổ sung

| Bảng | Số lượng |
|---|---:|
| NhanVien / TaiKhoan | 9 / 9 |
| KhachHang / HoSoBenhAn | 50 / 50 |
| LoaiDichVu / DichVu | 3 / 15 |
| LichLamViec | 125 |
| LichHen | 77 |
| PhieuKham / ChiTietPhieuKham | 60 / 76 |
| Voucher | 12 |
| HoaDon | 56 |

Lịch hẹn: 7 đã đặt, 7 đã đến, 53 hoàn tất, 7 hủy, 3 không đến. Phiếu: 2 chờ, 2 đang khám, 56 hoàn tất. Hóa đơn: 53 đã trả (27 tiền mặt, 26 chuyển khoản), 3 chưa trả.

Dashboard tại cuối ngày nạp: 14 lịch hôm nay, 4 hóa đơn đã trả hôm nay, đã thu 3.212.000đ, còn phải thu 1.300.000đ, 5 nha sĩ hoạt động. Các số này thay đổi theo ngày/giờ và thao tác nghiệp vụ tiếp theo; không được cố định trong UI.

## Cách nạp/kiểm tra

Chỉ dùng cho BClinic_Dev đã có bộ `seed_demo.sql`. Mã vùng mới: NV/TK101–106, KH/HS00101–00130, DV101–105, VC101–106, LLV800xx/801xx, lịch/phiếu/hóa đơn có hậu tố 501–564. Nếu mã đã có dữ liệu khác, SQL lỗi và rollback thay vì ghi đè.

```powershell
mvn -B -ntp -DskipTests package dependency:build-classpath '-Dmdep.outputFile=target/classpath.txt'
$scenarioClasspath = 'target/classes;' + (Get-Content target/classpath.txt -Raw).Trim()
java -cp $scenarioClasspath scripts/SeedScenarios.java --preview
java -cp $scenarioClasspath scripts/SeedScenarios.java
```

`--preview` chạy cùng transaction rồi rollback; không lưu bản ghi. Lần chạy thật tạo marker `DEMO_SCENARIOS_V2`/NK980099. Các lần chạy sau bỏ qua nạp và chỉ kiểm tra, không reset ngày hoặc nhân đôi dữ liệu. Script Java sinh hash PBKDF2 riêng cho từng tài khoản rồi bind vào SQL; không chạy nguyên seed_scenarios.sql trong SSMS vì file có tham số JDBC.

Sau nạp, launcher chạy `verify_demo.sql` và `verify_scenarios.sql`. Cả hai đã PASS trên database hiện tại; chạy lại seed và so sánh số bản ghi cả 13 bảng không đổi. Flyway validate V1/V2 vẫn đạt. Các tình huống dữ liệu sai như trùng lịch, dùng voucher hết hạn hay sửa phiếu hoàn tất phải bị từ chối; chúng được kiểm tra bằng test, không cố chèn dữ liệu vi phạm vào database.

Mốc ngày lấy lúc chạy lần đầu theo giờ Việt Nam. Không tự đổi dữ liệu mỗi ngày: trạng thái đang khám/chờ khám là tình huống mẫu đang lưu, cần hoàn tất qua nghiệp vụ khi dùng lâu dài. Những lịch DA_DAT đã qua giờ cần xử lý qua chức năng cập nhật không đến; không nạp lại để ghi đè lịch sử.
