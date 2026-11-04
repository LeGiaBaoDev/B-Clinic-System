# Bàn giao H2 — Lịch làm việc (UC012–015)

Bản hiện tại trên nhánh `feat/h2`, ngày 04/11/2026. Đây là gói kỹ thuật để review; không xác nhận PR đã mở/merge hoặc người nhận đã tích hợp.

## Luồng sử dụng

| Vai trò | Điểm vào | Chức năng |
|---|---|---|
| Quản lý | Lịch làm việc | Lịch tuần/tháng; lọc; phân ca; dời/hoán đổi ca; tiếp nhận đơn nghỉ |
| Cả ba vai trò | Lịch của tôi | Chỉ xem ca bản thân; chọn ngày để mở đúng tuần/tháng; gửi và xem đơn nghỉ của mình |
| Quản lý | Lịch làm việc → Đơn nghỉ phép | Chọn đơn chờ để tự kiểm tra lịch hẹn; duyệt khi không vướng lịch; từ chối có lý do |
| Nha sĩ | Lịch trực | Lịch hẹn/khám hiện có thuộc UC022, tách với ca làm cá nhân UC012 |

Nhân viên tạo đơn ở khối trên (ca hợp lệ, lý do); khối dưới hiển thị đơn, lọc trạng thái/ngày nghỉ, tìm mã và làm mới. Gửi đơn chưa thay ca. Duyệt đổi ca sang NGHI_PHEP; từ chối giữ ca và lưu lý do. Chờ duyệt vàng, đã duyệt xanh, từ chối đỏ nhạt trong bảng đơn. Trên lịch ca, nghỉ phép vàng và ca đang trực xanh theo đúng buổi/giờ.

## API công khai

Tất cả API đọc/ghi đều xác thực Actor từ tài khoản đang hoạt động trong DB. UI gọi bằng SwingWorker, không SQL/JDBC. DAO tự quản lý connection và transaction bằng JdbcHelper; không bọc các API public vào transaction khác để mong chúng commit chung.

| API | Đầu vào / kết quả | Quyền và quy tắc |
|---|---|---|
| LichLamViecDAO.layLichCaNhan() | List<LichLamViec>, không nhận maNV | Cả ba vai trò; luôn của Actor, kể cả Quản lý |
| layTatCa() | List<LichLamViec> | Quản lý toàn bộ; vai trò khác chỉ bản thân |
| layTheoNhanVien(maNV, tu, den) | Khoảng LocalDate đóng hai đầu | Quản lý đọc nhân viên khác; còn lại chỉ bản thân; khoảng phải hợp lệ |
| layTheoMa(maLLV) | LichLamViec hoặc null | Cùng phạm vi đọc; mã tồn tại nhưng không có quyền thì báo lỗi |
| them(ca), capNhat(ca) | boolean; mã LLV + 5 số gán sau commit | Quản lý; chỉ DA_PHAN_CONG; nhân viên hoạt động; ngày không quá khứ; không trùng ca/chồng giờ/nghỉ phép |
| kiemTraDoiCa(request), doiCa(request) | YeuCauDoiCa(caNguon, caDich, ngayMoi, caMoi) | Quản lý; ca nguồn/dích là bản chụp đã đọc; kiểm tra lại trước ghi |
| DonNghiPhepDAO.guiDon(maLLV, lyDo) | Mã DNP + 6 số | Cả ba vai trò, chỉ ca bản thân chưa kết thúc/đang phân công; lý do 1–255; một đơn chờ mỗi ca |
| layDonCuaToi() | List<Don> | Chỉ đơn bản thân, kể cả Quản lý |
| layDonQuanLy() | List<Don> | Chỉ Quản lý, gồm chờ và lịch sử |
| kiemTra(maDon) | List<LichHenAnhHuong> | Quản lý, đơn chờ; kiểm tra ca gốc/nhân viên và lịch DA_DAT/DA_DEN chồng giờ |
| duyet(maDon) | boolean | Quản lý; kiểm tra lại ca/lịch, cập nhật đơn + ca + nhật ký nguyên tử |
| tuChoi(maDon, lyDoTuChoi) | boolean | Quản lý; lý do 1–255; vẫn đóng được đơn có ca đã đổi/kết thúc |
| layThongBaoDoiCaGanNhat() | ThongBaoDoiCa(maTK, thoiDiem, noiDung) hoặc null | Thông báo gần nhất riêng Actor khi làm mới lịch cá nhân |

`YeuCauDoiCa.caDich != null`: hoán đổi hai nhân viên cùng chức vụ, giữ mã ca và chủ sở hữu, đổi hai khung ngày/giờ trong một transaction. `caDich == null`: dời caNguon sang ngayMoi/caMoi. Chỉ đổi ca chưa bắt đầu, không nghỉ phép, không tạo trùng và không làm mất ca phục vụ lịch hẹn đã đặt/đã đến.

`Don` là DTO truy vấn chỉ đọc gồm maDon, ca (bản chụp ngày/giờ lúc gửi), lyDo, ngayGui, trangThai, nguoiXuLy, ngayXuLy, lyDoTuChoi. Trạng thái đơn: CHO_DUYET/DA_DUYET/TU_CHOI; không thêm các trạng thái này vào LichLamViec. Ca đổi sau lúc gửi làm đơn không thể duyệt; từ chối đơn cũ để nhân viên gửi lại.

## Hợp đồng với Long L2

1. Lịch hẹn chỉ được tạo/dời vào khoảng được một ca **DA_PHAN_CONG** của nha sĩ đang hoạt động bao phủ trọn thời lượng dịch vụ. Không dùng ca NGHI_PHEP. Đơn CHO_DUYET/TU_CHOI không tự khóa ca.
2. Đặt/dời/hủy lịch và đổi ca/duyệt nghỉ dùng cùng khóa `MaSinh.khoa(c, "BClinic:lich")` trong transaction. Đọc dữ liệu để quyết định ghi cần kiểm tra lại trên cùng connection, dùng khóa hàng thích hợp. Gợi ý khung trống chỉ là gợi ý tại thời điểm đọc; commit luôn revalidate.
3. Trong `LichHenDAO` hiện có: `them`, `capNhat`, `huy`, `goiYKhungTrong(maKH, maNV, maDV, ngay)`. Không gọi thêm LichLamViecDAO public bên trong transaction lịch hẹn để thay cho kiểm tra ca cùng connection.
4. LichLamViecDAO không đổi nha sĩ của lịch hẹn, không tự dời/hủy bệnh nhân. Nếu lịch ảnh hưởng còn DA_DAT/DA_DEN, người có quyền cần xử lý lịch hẹn trước rồi quản lý chọn lại đơn để kiểm tra.
5. Các API duyệt/từ chối trực tiếp trên ca cũ đã deprecated và báo BusinessException, không dùng để bỏ qua đơn nhân viên. Chuyển sang DonNghiPhepDAO.duyet/tuChoi theo maDon.

LichHenDAO thuộc Long: H2 chỉ đọc/kiểm chứng bằng API hiện có, không sửa file sở hữu của Long. H2DaoIntegrationTest gọi LichHenDAO.them thật để xác nhận lịch nghỉ bị chặn và ca còn hợp lệ vẫn đặt được.

## Lỗi và đồng thời

BusinessException / IllegalArgumentException: hiển thị thông báo nghiệp vụ tiếng Việt. DataAccessException hoặc lỗi hệ thống: UI báo chung, không lộ SQL/thông tin kết nối. Kiểm tra trước trên UI không thay thế revalidate trong DAO. Khi ca/đơn đã đổi ở máy khác, tải lại; không tự ghi đè bản chụp cũ. Lỗi nhật ký rollback cả dữ liệu nghiệp vụ. Mã mới không gán cho entity nếu transaction thất bại.

## Migration và chạy từ mã nguồn

- Java 21, SQL Server, cấu hình riêng `src/main/resources/database.properties` theo file example; không đưa cấu hình thật lên Git.
- V3__don_nghi_phep.sql thêm bảng đơn, FK/CHECK và unique index một đơn chờ/ca. Giữ nguyên V1/V2, không tạo đơn giả từ ca nghỉ cũ. Khởi động app chạy Flyway trước khi dùng hộp đơn; không chạy riêng V3 bằng tay.
- Kiểm thử tích hợp dùng DB UUID riêng, xác nhận DB_NAME, dọn DB/config sau chạy. Không chạy test trên dữ liệu ứng dụng.

H2 bàn giao mã nguồn để chạy trong IDE/môi trường phát triển hiện có; không đóng gói JAR/RAR ở bước này. Không đổi dependency hoặc cấu hình Maven đóng gói.

## Review và nhận bàn giao

Xem [báo cáo kiểm thử](H2_TEST_REPORT.md), [checklist và cách thao tác](../src/test/README.md), [nội dung PR chuẩn bị sẵn](H2_PR.md).

Trước đánh dấu T601–T603/H2 hoàn tất: thành viên khác ghi kết quả kiểm thử chéo, review thay đổi, tự mở PR vào develop và tích hợp; Long xác nhận gọi API trên bản đã tích hợp. Báo cáo Word cần đồng bộ UC015/EER với luồng gửi đơn mới. Không đánh dấu các bước này đã xong chỉ vì build/test tự động đạt.
