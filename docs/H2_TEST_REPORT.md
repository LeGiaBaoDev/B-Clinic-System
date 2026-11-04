# Báo cáo kiểm chứng kỹ thuật H2 — 04/11/2026

Nhánh feat/h2. Kiểm chứng tự động và tự rà soát; chưa thay thế kiểm thử chéo, review và PR tích hợp.

Phạm vi chốt theo yêu cầu: hoàn thiện và kiểm thử H2 trong mã nguồn, không đóng gói JAR/RAR. Cấu hình đóng gói tạm thêm trước đó đã được bỏ.

## Kết quả tự động

```powershell
mvn -o -q '-Dbclinic.integration=true' '-Dtest=*,!H1IntegrationTest' test
```

Kết quả: **165 test chạy, 0 failure, 0 error, 0 skipped trong tập được chọn**. Gồm 164 test thường và 1 bài tích hợp H2 có nhiều kịch bản SQL. H1IntegrationTest không được chọn vì lỗi timeout thoát JVM đã biết; XML H1 cũ trong target không thuộc lượt này. Không tuyên bố toàn bộ H1 đã được sửa.

| Bộ test | Số test | Failure | Error | Skipped |
|---|---:|---:|---:|---:|
| entity.HoSoBenhAnTest | 15 | 0 | 0 | 0 |
| entity.KhachHangTest | 38 | 0 | 0 | 0 |
| entity.NhanVienTest | 1 | 0 | 0 | 0 |
| entity.TaiKhoanTest | 6 | 0 | 0 | 0 |
| integration.H2DaoIntegrationTest | 1 | 0 | 0 | 0 |
| module.DangNhapModuleTest | 5 | 0 | 0 | 0 |
| module.DoiMatKhauModuleTest | 7 | 0 | 0 | 0 |
| ui.dialog.DlgDoiCaTest | 8 | 0 | 0 | 0 |
| ui.dialog.DlgDoiMatKhauLanDauTest | 4 | 0 | 0 | 0 |
| ui.dialog.DlgNghiPhepTest | 13 | 0 | 0 | 0 |
| ui.dialog.DlgThemNhanVienTest | 14 | 0 | 0 | 0 |
| ui.panel.PnlLichCaNhanTest | 6 | 0 | 0 | 0 |
| ui.panel.PnlLichLamViecTest | 12 | 0 | 0 | 0 |
| ui.panel.PnlLichNhaSiTest | 7 | 0 | 0 | 0 |
| ui.panel.PnlNhanVienTest | 5 | 0 | 0 | 0 |
| ui.panel.PnlTiepNhanTest | 2 | 0 | 0 | 0 |
| util.KiemTraXacThucTest | 9 | 0 | 0 | 0 |
| util.PasswordHasherTest | 8 | 0 | 0 | 0 |
| util.QuyTacTaiKhoanTest | 4 | 0 | 0 | 0 |

Bằng chứng SQL tại target/h2-dao-*/result.log: ISOLATED_DATABASE_CONFIRMED, H2_UC013_UI_SAVE_AND_DUPLICATE_OK, H2_LEAVE_BLOCKED_UI_AND_DAO_OK, UC012_PERSONAL_ONLY_*_OK, UC014_MOVE_SWAP_PERMISSION_APPOINTMENT_ROLLBACK_CONCURRENT_OK, UC015_EMPLOYEE_REQUEST_INBOX_APPROVE_REJECT_SNAPSHOT_ROLLBACK_CONCURRENT_PRIVACY_OK, H2_DAO_INTEGRATION_OK. Output Maven xác nhận TEMP_TEST_DB_REMOVED.

H2 integration tạo DB UUID riêng, nâng V2→V3 và so dữ liệu ca cũ, xác nhận DB_NAME, kiểm tra quyền ba vai trò, sinh mã, trùng ca/đơn, snapshot đã đổi, đơn quá hạn, lịch hẹn sau preview, chặn đặt lịch sau nghỉ, rollback nhật ký và cạnh tranh tạo/duyệt. Không migrate hoặc ghi nghiệp vụ lên DB ứng dụng.

## Tự rà soát

- UI H2 không SQL/java.sql/DatabaseConnection; DAO H2 không Swing/AWT/createStatement. Các truy vấn dùng tham số qua JdbcHelper.
- Ghi lịch/đơn/nhật ký dùng transaction và khóa BClinic:lich; quyền kiểm tra lại từ DB, không chỉ ẩn nút.
- Migration V3 riêng; V1/V2 không thay đổi. FK/CHECK có tên; unique index một đơn chờ/ca.
- git diff --check đạt. Cấu hình thật và target được Git ignore. Đã so pom.xml với HEAD: không còn thay đổi cấu hình đóng gói hoặc dependency/plugin trong H2. Các link nội bộ trong hồ sơ bàn giao đã kiểm tra tồn tại.
- Ảnh UI đã xem ở các lượt hoàn thiện: target/uc015-don-cua-toi.png và target/uc015-hop-thu.png (1050×700). Test kiểm tra form nhân viên nằm trên danh sách, màu trạng thái và quản lý tự kiểm tra khi chọn đơn. Chưa tuyên bố thao tác thủ công trên mọi độ phân giải.
- Log còn cảnh báo logging provider có sẵn và log tình huống lỗi cố ý trong test; exit code test bằng 0. Không phát sinh thay đổi thư viện runtime.

## Kiểm thử chéo cần thành viên ghi nhận

Các dòng dưới là phiếu kiểm thử, không phải kết quả đã đạt. Người thực hiện khác người viết code; thực hiện trên DB thử nghiệm và điền kết quả/người/ngày trước nghiệm thu.

| Mã | Thao tác | Kết quả mong đợi | Kết quả thực tế | Đạt? | Người test | Ngày |
|---|---|---|---|---|---|---|
| H2-M01 | Quản lý mở Lịch làm việc, chuyển tuần/tháng và Làm mới/F5 | Hiển thị đúng khoảng ngày và ca đã có; không đứng giao diện | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M02 | Lọc vị trí, chọn nhân viên và tạo ca mới | Chỉ nhân viên có tài khoản hoạt động được chọn; giờ đúng ca; báo thành công, bảng có ca mới | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M03 | Lưu lại cùng nhân viên/ngày/ca | Báo trùng ca, giữ lựa chọn để sửa; không tạo ca thứ hai | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M04 | Chọn ngày quá khứ hoặc nhập 31/02 | Báo lỗi ngày, không gửi lưu | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M05 | Nhấn Lưu nhiều lần khi đang xử lý | Chỉ một yêu cầu; không sửa nhân viên/ngày/ca trong lúc lưu | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M06 | Đăng nhập Nha sĩ/Lễ tân | Không có chức năng phân công trong menu; DAO vẫn kiểm tra quyền | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M07 | Thu nhỏ cửa sổ về 1366×768 hoặc kích thước tối thiểu | Các nút dùng được, form cuộn dọc, bảng cuộn ngang khi cần | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M08 | Quản lý chọn Lịch của tôi, rồi Lịch làm việc | Cá nhân chỉ có ca của mình; màn quản lý vẫn xem/phân công toàn bộ | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M09 | Nha sĩ chọn Lịch của tôi rồi quay lại Lịch trực | Cá nhân chỉ có ca của mình; Lịch trực giữ lịch hẹn/khám hiện có | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M10 | Lễ tân chọn Lịch của tôi rồi Bệnh nhân/Tiếp nhận | Cá nhân chỉ có ca của mình; các chức năng cũ vẫn dùng được | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M11 | Đổi tuần/tháng, Hôm nay, lọc Nghỉ phép, F5 | Đúng kỳ, đúng ca/nghỉ phép cá nhân; không sửa được dữ liệu | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M12 | Tài khoản chưa có ca trong kỳ | Hiển thị chưa có ca, không nhầm lỗi tải dữ liệu | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M13 | Dời một ca chưa bắt đầu sang ngày/ca trống | Kiểm tra → xác nhận → thành công; mã ca/nhân viên giữ nguyên | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M14 | Hủy hộp xác nhận | Không đổi dữ liệu, form vẫn dùng được | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M15 | Hoán đổi hai người cùng chức vụ | Hai khung ngày/giờ đổi cho nhau, không đổi một bên | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M16 | Chọn ca vướng lịch hẹn, nghỉ phép hoặc ca đã có | Báo lý do, không ghi thay đổi | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M17 | Hai cửa sổ cùng đổi một ca | Yêu cầu dùng dữ liệu cũ bị từ chối; tải lại trước khi đổi tiếp | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M18 | Nha sĩ/Lễ tân mở Lịch của tôi | Không có nút Đổi ca; nhận thông báo riêng nếu ca đã đổi | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M19 | Nha sĩ gửi đơn từ Lịch của tôi | Đơn chờ duyệt, ca giữ nguyên; xuất hiện trong hộp quản lý | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M20 | Quản lý kiểm tra ca có lịch Đã đặt/Đã đến | Hiện danh sách lịch ảnh hưởng, khóa Duyệt; sau xử lý lịch hẹn kiểm tra lại được | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M21 | Nhập lý do từ chối rồi xác nhận | Ca giữ nguyên; nhân viên thấy kết quả và lý do; có thể gửi lại | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M22 | Ca bị dời sau khi nhân viên gửi đơn | Duyệt bị chặn; từ chối đơn cũ được | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M23 | Lý do rỗng, >255; ca kết thúc; gửi trùng đơn chờ | Không cho gửi/xử lý sai | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M24 | Nha sĩ/Lễ tân mở hộp cá nhân | Chỉ thấy đơn bản thân; không có nút duyệt/từ chối | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M25 | Duyệt nghỉ sáng rồi thử đặt khám sáng/chiều | Sáng bị chặn; chiều vẫn được nếu có ca và khung giờ hợp lệ | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M26 | Hai người duyệt đồng thời / hủy xác nhận | Chỉ một quyết định được lưu / hủy không ghi dữ liệu | Chưa thực hiện kiểm thử chéo | Chờ | — | — |
| H2-M27 | Quản lý gửi đơn của bản thân | Có trong hộp cá nhân và hộp tiếp nhận; dữ liệu tách theo quyền xem | Chưa thực hiện kiểm thử chéo | Chờ | — | — |

## Điều kiện kết thúc

Sau kiểm thử chéo và review đạt, thành viên tự commit/push và mở PR vào develop theo quy định Git. Chỉ đánh dấu toàn H2 xong khi PR được tích hợp và Long xác nhận dùng hợp đồng API trên bản chung. Xem H2_HANDOFF.md và H2_PR.md để nhận bàn giao/mô tả PR. Báo cáo Word của nhóm cần cập nhật UC015/EER theo đơn nghỉ phép mới; file Word chưa sửa trong gói này.


Lượt xác nhận cuối 04/11/2026 22:36: chỉ chạy lifecycle test; 165 test, 0 failure/error/skipped trong tập chọn, exit code 0; có H2_DAO_INTEGRATION_OK và TEMP_TEST_DB_REMOVED. Không chạy package hoặc tạo JAR mới.
