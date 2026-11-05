# Handoff — Đợt chỉnh UI (6 yêu cầu từ screenshot)

Cập nhật: 2026-11-05. Nhánh: `develop`. **Chưa commit** (theo quy tắc chỉ commit khi được yêu cầu).
Trạng thái build: `mvn -o compile` → **BUILD SUCCESS**.

## 6 yêu cầu gốc của người dùng
a. Thống kê: "Không có dữ liệu" hiển thị đẹp hơn và **nằm gọn bên trong ô** (bảng + biểu đồ).
b. Chọn thời gian: **làm mờ / không cho chọn** ngày không hợp lệ ngay trên lịch (vd đã chọn "Từ ngày" thì "Đến ngày" không được trước đó) — thay vì chọn xong mới báo lỗi.
c. "Line bệnh nhân" chưa đẹp → làm đẹp hơn (bảng thống kê + thẻ bệnh nhân ở màn Tra cứu).
d. Thêm chữ "Làm mới" cho nút refresh; thêm chữ "Đăng xuất" nằm dưới cùng thanh nav dọc.
e. Bỏ chức năng "Tìm kiếm nhanh" + UI của nó (ô search ở header).
f. Thông báo (popup chuông + hướng dẫn) hiển thị đẹp hơn cho **toàn bộ các loại nhân viên**, và cả đăng xuất.

## ĐÃ LÀM XONG
- **(a)** `PnlThongKeBenhNhan.java`: bảng "Bệnh nhân trong ngày" dùng `CardLayout` ("bang"/"trong"); ô trống `oTrong(...)` căn giữa trong khung (icon + tiêu đề + mô tả). Biểu đồ vốn đã tự vẽ "Không có dữ liệu" căn giữa trong ô.
- **(b)** `PnlThongKeBenhNhan.addEvents()`: thêm listener `"date"` → `dpDen.setMinSelectableDate(dpTu)` và `dpTu.setMaxSelectableDate(dpDen)`; đã **xoá** dialog báo lỗi `den.isBefore(tu)` trong `taiDuLieu()`.
- **(c)** Bảng thống kê: thêm renderer `OBenhNhan` (căn lề, padding, canh giữa dọc) + `headerRenderer(...)`.
  Thẻ bệnh nhân `CardBenhNhan` trong `PnlTraCuuHoSo.java`: thêm **avatar tròn viết tắt**, pill trạng thái (`PillLabel`), chevron canh giữa dọc, thanh nhấn trái khi chọn, bo góc 10. Thêm helper `vietTat(...)`.
- **(d)** `PnlLichNhaSi.taoSidebar()`: thêm nút **"Đăng xuất"** (`taoNutDangXuat()`) dưới cùng nav (nền `RED_LIGHT`, icon LOGOUT).
  `PnlTongQuanNhaSi`: `btnLamMoi` đổi sang `PnlTongQuanQuanLy.button("Làm mới", CHART, false)` và **đã add vào header** (trước đây khai báo nhưng không hiển thị).
- **(e)** Đã xoá ô "Tìm kiếm nhanh" + phím tắt Ctrl+K + listener đồng bộ ở **cả 3 màn**:
  - `PnlLichNhaSi`: xoá field `txtTimKiemHeader`, `dongBoTimKiem`, binding Ctrl+K; listener `txtTimKiem` giờ gọi thẳng `apDungLoc()`.
  - `PnlLeTan`: xoá field `txtTimKiemNhanh`, block search header, binding Ctrl+K.
  - `PnlQuanLy`: xoá field `txtTimKiem`, block search header, binding Ctrl+K.
- **(f) phần chính (nha sĩ)** — `PnlLichNhaSi`:
  - `hienThongBaoLich()` viết lại thành popup đẹp (header + badge số lượng + từng dòng có giờ/tên/dịch vụ + pill trạng thái, click để nhảy tới lịch).
  - `hienHuongDan()` mới (panel có bullet, bỏ nhắc Ctrl+K). Xoá `thongBaoChuaCo` (dead code).
  - Bỏ dòng "Ctrl + K" trong help của `PnlQuanLy`.

## ĐỢT 2 — ĐÃ LÀM XONG
- **Component dùng chung** `ui/component/PopupThongBao.java`: tiêu đề + badge số lượng + từng dòng (giờ/tên/phụ/pill), dòng trống căn trái màu `MUTED`. Có `Dong.hanhDong` để click điều hướng.
- **(f) lễ tân** — `PnlLeTan`: chuông gọi `hienThongBao()`, tải lịch hôm nay bằng `LichHenDAO.timKiem(...)` trong `SwingWorker`, hiển thị popup đẹp; click dòng → mở trang "Lịch hẹn".
- **(f) quản lý** — `PnlQuanLy`: chuông gọi `hienThongBao()`, dùng `PnlTongQuanQuanLy.thongBaoLich()` (mới thêm) để hiện danh sách lịch chờ; click → về "Tổng quan".
- **(f) nha sĩ** — `PnlLichNhaSi`: `hienThongBaoLich()` refactor sang `PopupThongBao` (bỏ `dongThongBao` cũ), vẫn click để chọn lịch.
- **(f) đăng xuất** — thêm `ui/dialog/DlgXacNhan.java` (dialog xác nhận custom, nút đồng ý đỏ, ESC = hủy). `FrmMain.yeuCauDangXuat()` dùng dialog này cho **mọi vai trò**; bỏ `xacNhanDangXuat()` trùng lặp trong `PnlLichNhaSi` (trước đây bị hỏi xác nhận 2 lần).
- **Import thừa**: đã dọn `SurfacePanel`, `JTextField` ở `PnlLeTan` (các file khác không còn import thừa theo kiểm tra tự động).
- Build: `mvn -q compile` OK; `mvn -q test` → **290 tests, 0 failures, 0 errors, 6 skipped**.

## CÒN LẠI
1. **Kiểm thử trực quan**: vẫn **chưa chạy app**. Nên chạy để xem: lịch chọn ngày bị làm mờ; ô trống thống kê; thẻ bệnh nhân; popup chuông 3 vai trò; dialog đăng xuất mới; nút Đăng xuất/Làm mới.

## Lưu ý quy tắc
- KHÔNG commit/push trừ khi người dùng yêu cầu rõ ràng.
- Khi được yêu cầu commit: kết thúc message bằng `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`; PR body kết thúc bằng `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
- Giao tiếp tiếng Việt. Tuân thủ style code sẵn có (helper `button(...)`, `pill(...)`, `label(...)`, `SurfacePanel`, `LineIcon`, UiConstants).
