# Hướng dẫn tầng DAO

## Khởi tạo và phân quyền

DAO không chứa Swing hoặc SQL trong UI. Constructor mặc định dùng `database.properties`, phiên `SessionManager` và giờ Việt Nam. Sau khi đăng nhập, UI phải gọi `SessionManager.getInstance().login(taiKhoan)` như hiện tại. Mỗi API nghiệp vụ kiểm tra lại vai trò/trạng thái tài khoản từ database.

```java
TaiKhoan taiKhoan = new TaiKhoanDAO().dangNhap(tenDangNhap, matKhau);
SessionManager.getInstance().login(taiKhoan);
List<DichVu> danhMuc = new DichVuDAO().timKiem("", true);
```

Test hoặc công cụ có thể truyền `DaoContext(DataSource, Supplier<TaiKhoan>, Clock)`; không đổi file cấu hình thật để chạy test. Không truyền nhà cung cấp người dùng không tin cậy từ input bên ngoài. Các overload nhận `Connection` chỉ dùng nội bộ package DAO để chia sẻ transaction.

## API theo module

Màn Tổng quan Quản lý đã nối dữ liệu thực qua `ThongKeDoanhThuDAO.layTongQuanQuanLy()`; xem [TONG_QUAN_QUAN_LY.md](TONG_QUAN_QUAN_LY.md) về phạm vi công nợ, cách tính và tải nền.

| DAO | API chính | Quyền |
|---|---|---|
| NhanVienDAO | layTatCa, layTheoMa, timKiem, layNhaSiDangHoatDong, them, capNhat | Đọc: người đã đăng nhập; ghi: quản lý |
| TaiKhoanDAO | dangNhap, xacMinhMaNV, doiMatKhau, taoNhanVienVaTaiKhoan, them, capNhat, khoaTaiKhoan/xoa, layTatCa, layTheoMa, timKiem | Đăng nhập/reset theo luồng hiện có; quản lý tài khoản: quản lý; xem bản thân: chính chủ |
| NhatKyHeThongDAO | ghi, layTatCa, timKiem | Ghi theo người đang đăng nhập; đọc: quản lý |
| LoaiDichVuDAO | layTatCa, layTheoMa, them, capNhat | Đọc: đã đăng nhập; ghi: quản lý |
| DichVuDAO | layTatCa, layTheoMa, timKiem, them, capNhat, ngungCungCap/xoa | Đọc: đã đăng nhập; ghi: quản lý |
| VoucherDAO | layTatCa, layTheoMa, layConHieuLuc, them, capNhat, xoa | Quản lý danh mục: quản lý; đọc voucher tính tiền: lễ tân/quản lý |
| KhachHangDAO | layTatCa, layTheoMa, timKiem, timTrung, them(kh, tienSu), capNhat | Ghi: lễ tân/quản lý; nha sĩ chỉ đọc khách đã được phân công |
| HoSoBenhAnDAO | layTatCa, layTheoMa, them | Đọc: quản lý/nha sĩ được phân công; tạo hồ sơ: lễ tân/quản lý |
| LichLamViecDAO | layTatCa, layTheoMa, layTheoNhanVien, them, capNhat, duyetNghiPhep | Ghi: quản lý; nhân viên xem lịch của mình |
| LichHenDAO | layTatCa, layTheoMa, timKiem, them, capNhat, huy, goiYKhungTrong, demSoLanHuyTrongThang, capNhatKhongDen | Ghi: lễ tân/quản lý; nha sĩ chỉ đọc lịch được phân công |
| PhieuKhamDAO | layTatCa, layTheoMa, timKiem, taoTuLichHen, batDauKham, hoanTatPhieuKham | Tiếp nhận: lễ tân/quản lý; khám: nha sĩ được phân công; lễ tân không nhận nội dung bệnh án |
| ChiTietPhieuKhamDAO | layTheoPhieuKham, them, layDongThanhToan | Điều trị: nha sĩ được phân công; quản lý đọc; lễ tân chỉ đọc record tính tiền |
| HoaDonDAO | layTatCa, layTheoMa, timKiem, apDungVoucher, tinhTongTien, thanhToan | Lễ tân/quản lý |
| ThongKeLichHenDAO | thongKe | Nha sĩ chỉ thống kê lịch của mình; quản lý/lễ tân xem lịch |
| ThongKeDoanhThuDAO | thongKe, dichVuSuDung, soSanh, tongQuanTrongNgay | Quản lý; lễ tân chỉ gọi thongKe cho một ngày |
| ThongKeBenhNhanDAO | thongKe, demBenhNhanKhacNhau | Quản lý/nha sĩ, nha sĩ chỉ tính bệnh nhân được phân công |
| BackupDAO | backup, verify, docThongTin, restore, validateDuongDan, layLichSu | Quản lý; loại FULL/AUTO/PRE_RESTORE |

Không cung cấp CRUD xóa/sửa một cách máy móc cho mọi bảng: hồ sơ/chi tiết điều trị và nhật ký chỉ thêm; phiếu hoàn tất không sửa; tài khoản/dịch vụ/voucher dùng khóa hoặc vô hiệu thay xóa cứng. Các tên `xoa` của tài khoản, dịch vụ, voucher là vô hiệu hóa. Loại dịch vụ phải tuân theo ba loại/thuế đã quy định trong entity và schema.

Mã được sinh và giữ khóa đến commit ngay trong method thêm; caller để mã null. Không tự gọi SELECT MAX hoặc đặt mã trước ở UI. API đọc trả null nếu không tìm thấy trong các method tra cứu đơn giản; thao tác nghiệp vụ ném `BusinessException` nếu thiếu bản ghi hoặc sai trạng thái. SQL lỗi hạ tầng được bọc trong `DataAccessException` và giữ nguyên cause.

## Luồng nguyên tử

1. `TaiKhoanDAO.taoNhanVienVaTaiKhoan(nv, tk)`: tạo cả hai hoặc rollback cả hai. `tk.matKhau` đầu vào là mật khẩu gốc; lớp DAO hash trước khi lưu. Tra cứu không trả hash mật khẩu.
2. `KhachHangDAO.them(kh, tienSuBenh)`: tạo khách và hồ sơ 1–1 cùng transaction. Ngày sinh đã lưu không được thay đổi.
3. `LichHenDAO.them/capNhat`: kiểm tra người dùng, dịch vụ, ca trực, nghỉ phép, khoảng cách 15 phút, khách trùng giờ, giới hạn hủy trong tháng và tái khám. `goiYKhungTrong` chỉ là gợi ý; lúc lưu luôn kiểm tra lại trong transaction. Dời lịch giữ nguyên mã được cấp lúc tạo.
4. `PhieuKhamDAO.taoTuLichHen`: chỉ nhận lịch DA_DAT cùng ngày, tạo phiếu CHO_KHAM và chuyển lịch DA_DEN.
5. `batDauKham` → `ChiTietPhieuKhamDAO.them` khi cần lưu từng dòng → `hoanTatPhieuKham(pk, dsThemMoi)`: thêm các dòng mới trước, khóa phiếu HOAN_TAT sau, tạo hóa đơn chưa trả cùng transaction. `dsThemMoi` chỉ gồm dòng chưa lưu, maCTPK=0; nếu đã lưu hết chi tiết, truyền danh sách rỗng. Không gửi lại các dòng đã lưu.
6. `HoaDonDAO.apDungVoucher`: một voucher/hóa đơn, tính lại tiền giảm ở DAO; giá/thuế theo ảnh chụp của chi tiết khám. `thanhToan` kiểm tra tiền nhận, voucher tại thời điểm thanh toán, cập nhật hóa đơn và lịch HOAN_TAT cùng transaction, trả tiền thối. Chuyển khoản phải khớp tổng tiền.

Đơn giá/thuế không tin từ input UI mà lấy từ danh mục khi thêm chi tiết. Tổng tiền/voucher/thống kê dùng BigDecimal và làm tròn HALF_UP đến đồng. Entity đang dùng double được giữ tương thích; DAO không đổi schema và không sửa giá lịch sử.

Lễ tân nhận `ChiTietPhieuKhamDAO.DongThanhToan` thay vì danh sách chẩn đoán. Đồ thị phiếu/lịch/hóa đơn trả cho lễ tân đã bỏ tiền sử bệnh và lời dặn; API chi tiết điều trị từ chối vai trò lễ tân. Phiếu nguồn tái khám có dữ liệu hồ sơ nhưng không nạp ngược lịch nguồn để tránh đồ thị vòng lặp; muốn xem chi tiết nguồn thì gọi API tra cứu phiếu với quyền tương ứng.

## Cấu hình và mật khẩu

`LichHenDAO(context, QuyDinhLichHen)` cho phép truyền khoảng chuẩn bị, giới hạn hủy, khung đầu/cuối ngày, thời gian trễ và khoảng ngày tái khám. Mặc định 15 phút/3 lần/15 phút trễ/7–14 ngày; khung giới hạn 07:00–09:30 và 16:00–17:30. Đây là khoảng chung hiện tại; schema chưa có cột min/max cho từng lộ trình nên không suy diễn con số từ chuỗi mô tả dịch vụ. Khoảng chuẩn bị không được nhỏ hơn 15 vì trigger V1 đang cố định 15. Ca được kiểm tra cả CA_SANG, CA_CHIEU, CA_NGAY theo schema; chức năng này không đổi dữ liệu seed đã tạo trước đó.

`capNhatKhongDen` là thao tác cập nhật riêng. UI/job phải gọi trước khi tải danh sách cần trạng thái hiện tại; method đọc không âm thầm ghi database. DAO không chứa scheduler. Schema hiện chưa lưu trạng thái yêu cầu/duyệt nghỉ chờ xử lý: `duyetNghiPhep` áp dụng quyết định quản lý, từ chối nếu còn lịch khám cần dời.

PasswordHasher dùng PBKDF2WithHmacSHA256, salt ngẫu nhiên 16 byte, 210.000 vòng, khóa 256 bit. Đăng nhập chấp nhận plaintext/SHA-256 cũ rồi nâng cấp riêng tài khoản đó trong transaction khi xác thực thành công. Không chạy migration đổi đồng loạt mật khẩu. Luồng quên mật khẩu mã nhân viên được giữ để không phá UI hiện có, có hạn xác minh 5 phút; mã nhân viên đơn lẻ vẫn là hạn chế xác minh danh tính, cần nhóm thiết kế luồng xác minh mạnh hơn ở task riêng.

Driver SQL Server có tùy chọn sendTimeAsDatetime mặc định ảnh hưởng so sánh cột TIME. DaoSupport tắt tùy chọn này khi bind LocalTime và bind rõ JDBC TIME/DATE/TIMESTAMP. Nguồn: [Microsoft JDBC](https://learn.microsoft.com/en-us/sql/connect/jdbc/configuring-how-java-sql-time-values-are-sent-to-the-server?view=sql-server-ver17).

## BackupDAO

Constructor mặc định lấy backup.dir trong app.properties nếu có, nếu không dùng InstanceDefaultBackupPath của SQL Server. Có thể cấu hình `backup.history=backups/backup-history.csv` và `backup.timeoutSeconds=120`. Thư mục phía máy chủ phải tồn tại và tài khoản dịch vụ SQL Server phải có quyền ghi. Không cần đưa mật khẩu vào app.properties.

Backup dùng master connection, COPY_ONLY/CHECKSUM/INIT, COMPRESSION khi không phải Express; chỉ báo thành công sau VERIFYONLY. Tên file có database, loại và timestamp; kiểm tra để không ghi đè file cùng tên. Có khóa trong ứng dụng và applock phiên master để tránh backup/restore đồng thời giữa các tiến trình cùng database.

`restore(file, matKhauQuanLy, xacNhanLanHai)` kiểm tra đường dẫn, full-backup/header đúng database và checksum, xác thực mật khẩu, tạo PRE_RESTORE, đóng pool trước restore và trả MULTI_USER trong finally. Pool được tạo lại sau bảo trì, DAO cũ lấy pool mới; restore thành công đăng xuất. Nếu còn connection ứng dụng hoạt động thì từ chối restore để người dùng đợi. Lỗi restore cần kiểm tra trạng thái database và PRE_RESTORE; không bảo đảm dữ liệu hiện tại giữ nguyên khi SQL Server đã bắt đầu restore rồi gặp lỗi.

Lịch sử CSV được giữ ngoài database và nhật ký database ghi sau restore khi tài khoản còn tồn tại. Constructor có overload tiêm master connector/lifecycle để test riêng. `layLichSu` trả dòng CSV đã escape. Phạm vi lần này chưa gồm UI xác nhận mật khẩu tối đa ba lần, SwingWorker/thanh tiến trình, scheduler định kỳ, dọn các file AUTO cũ hoặc file hỏng ở máy chủ. Các phần đó cần gọi DAO này khi triển khai UI/scheduler. API restore không được gọi trực tiếp từ EDT.

Nguồn SQL: [Microsoft BACKUP](https://learn.microsoft.com/en-us/sql/t-sql/statements/backup-transact-sql), [RESTORE HEADERONLY](https://learn.microsoft.com/en-us/sql/t-sql/statements/restore-statements-headeronly-transact-sql).

## Kiểm thử

```powershell
mvn -B -ntp test
mvn -B -ntp package '-Dbclinic.it=true'
```

Lệnh thứ nhất chạy unit test, bỏ qua integration test. Lệnh thứ hai dùng thông tin kết nối cục bộ để tạo database BClinic_Test_<random>, migrate V1/V2, nạp seed test, kiểm tra DAO và backup/restore **chỉ trên database vừa tạo**, rồi xóa đúng database đó. Tài khoản SQL cần quyền tạo/xóa DB và backup/restore; không chạy test ghi trên BClinic_Dev/BClinic. File backup test nằm tại thư mục backup của SQL Server, lịch sử test ở target/test-backups; file .bak không tự xóa.

Các tình huống đã viết test: map dữ liệu liên kết, thống kê không nhân đôi hóa đơn nhiều dòng, rollback nhân viên khi trùng tài khoản, tạo hồ sơ cùng khách, khóa ngày sinh, các loại ca, khoảng cách lịch đúng/sai 15 phút, hai người đặt cùng lúc chỉ một thành công, không cho nghỉ khi còn lịch, chặn truy cập bệnh án, hoàn tất khám/thanh toán, rollback giữa danh sách chi tiết, voucher giới hạn/hết hạn/chưa hiệu lực/vô hiệu, hạn chế khách hủy nhiều, gợi ý giờ trống, tái khám sai khoảng ngày, cập nhật không đến và backup/restore/logout.

Chưa thay thế review chéo/test giao diện theo báo cáo. Workspace chưa có DOCX nghiệp vụ gốc; triển khai dựa trên schema, entity và bộ tài liệu hiện có. Chưa thay đổi V1/V2 hoặc tạo migration mới. JUnit test dùng [JUnit 5](https://docs.junit.org/5.11.4/user-guide/index.html).
