# Dữ liệu thực tế trên màn Tổng quan Quản lý

Màn hình đọc database đang cấu hình trong `database.properties`, không giữ số liệu mẫu của bản thiết kế. Các bản ghi demo đã nạp trước đó cũng là dữ liệu hiện có trong database; màn hình hiển thị đúng trạng thái lưu, không tự sinh thêm giao dịch hoặc tự hoàn tất phiếu khám.

## Nguồn và cách tính

`ThongKeDoanhThuDAO.layTongQuanQuanLy()` trả snapshot chỉ đọc với cùng mốc giờ theo `DaoContext` (mặc định Asia/Ho_Chi_Minh). Quyền truy cập được kiểm tra lại trong database, chỉ QUANLY.

| Thành phần | Dữ liệu/công thức |
|---|---|
| Ngày, lời chào, đồng hồ | Ngày giờ Việt Nam thực tế; lời chào sáng/chiều/tối theo giờ |
| Lịch hẹn hôm nay | Tất cả LichHen có ngayGioHen trong ngày, kể cả hủy/không đến; phân nhóm đúng enum |
| Hóa đơn đã thanh toán | Đếm hóa đơn có trạng thái đã trả và ngayThanhToan từ đầu ngày đến thời điểm tải |
| Đã thu hôm nay | Tổng tiền chi tiết sau thuế, trừ tongKhuyenMai, chỉ hóa đơn đã trả trong ngày |
| Hóa đơn chưa thanh toán | Tất cả hóa đơn chưa trả có ngayLap không ở tương lai, bao gồm công nợ các ngày trước |
| Còn phải thu | Tổng tiền các hóa đơn chưa trả nói trên; không giả định phải là hóa đơn lập hôm nay |
| Doanh thu dự kiến | Đã thu hôm nay + còn phải thu; không cộng giá dịch vụ của lịch hẹn chưa có hóa đơn |
| Biểu đồ 7 ngày | Doanh thu đã trả theo ngày thanh toán, gồm hôm nay và 6 ngày trước; bổ sung 0 cho ngày không phát sinh |
| Tổng trên biểu đồ | Tổng 7 ngày đang hiển thị |
| Tăng/giảm | So tổng 7 ngày hiện tại với 7 ngày liền trước; kỳ trước=0 và kỳ này>0 hiển thị chưa có kỳ trước; cả hai=0 hiển thị 0%, không chia cho 0 |
| Bảng lịch hẹn | Giờ, khách hàng, nha sĩ và trạng thái lấy bằng JOIN; không nạp chẩn đoán để hiển thị danh sách |
| Dòng nổi bật/thông báo | Lịch sắp tới DA_DAT còn trong ngày hoặc khách DA_DEN đang chờ khám, loại phiếu đã hoàn tất; không dùng số ghế giả |
| Trạng thái nha sĩ | Danh sách tài khoản NHASI đang hoạt động; đang khám dựa trên phiếu DANG_KHAM đã bắt đầu; nếu không, kiểm tra ca hiện tại để phân biệt rảnh/nghỉ phép/ngoài ca |
| Ca ở sidebar | Lịch làm việc của người quản lý đang đăng nhập trong ngày và bao phủ giờ hiện tại; không có thì thông báo không có ca hiện tại |

Tiền được tính bằng BigDecimal. Tổng chi tiết được gom theo maPK trước khi JOIN hóa đơn, tránh nhân đôi tiền giảm khi có nhiều dịch vụ. Thuế từng dòng làm tròn HALF_UP đến đồng theo đặc tả hiện hành. Lượt khám đã bắt đầu nhưng chưa hoàn tất vẫn báo đang khám dù đã qua ca; DAO không tự kết thúc bệnh án dựa theo thời gian dự kiến.

Không hiển thị Quận 3 hay Ghế 01/02 vì schema hiện tại chưa có chi nhánh/ghế. Tên và ảnh chữ viết tắt ở tài khoản vẫn lấy từ phiên đăng nhập. Menu CRUD/báo cáo chưa triển khai vẫn giữ trang chờ hoàn thiện; không thuộc phạm vi nối dữ liệu tổng quan lần này.

## Tải dữ liệu và trạng thái

- `PnlTongQuanQuanLy` tạo DAO và truy vấn trong `SwingWorker.doInBackground`, cập nhật component ở `done` trên EDT.
- Tự tải khi panel hiển thị, tự làm mới mỗi 60 giây, có nút Làm mới và phím F5. Ctrl+K lọc bảng đã tải, không truy vấn theo mỗi ký tự.
- Không cho hai lượt tải chồng nhau. Panel bị tháo/đăng xuất sẽ dừng timer, hủy worker và bỏ kết quả muộn.
- Lần đầu đang tải dùng dấu “—”; chưa tải được không giả thành số 0. Nếu lần làm mới lỗi, giữ bản thành công gần nhất và thông báo rõ dữ liệu cũ cùng giờ cập nhật.
- Khi truy vấn thành công nhưng database không có dữ liệu, số liệu thực là 0, bảng rỗng có thông báo, biểu đồ vẫn đủ ngày và hiển thị chưa phát sinh doanh thu.
- Màn hình chỉ đọc; không gọi cập nhật không đến/ghi nhật ký/đổi mật khẩu khi làm mới. Trạng thái quá hạn chưa được nghiệp vụ cập nhật vẫn phản ánh đúng giá trị đang lưu.

## Kiểm chứng

- Unit test: bổ sung ngày trống, không chia cho 0, tỷ lệ âm.
- Integration test trên BClinic_Test_<random>: công nợ cũ vẫn được tính, số liệu sau thanh toán thay đổi đúng, đủ 7 ngày, giờ ngoài ca, đang khám, kiểm tra quyền quản lý.
- Test Swing: supplier chạy ngoài EDT; không tải trùng; bảng dùng dữ liệu DAO và lọc đúng; dữ liệu rỗng; giữ dữ liệu cũ và báo lỗi khi tải lại thất bại.
- Kiểm tra chỉ đọc BClinic_Dev lúc 23:36 ngày 02/11/2026: 2 lịch hôm nay, 0 hóa đơn trả hôm nay, 2 chưa trả, còn phải thu 1.200.000 đồng, 1 nha sĩ; truy vấn snapshot khoảng 570 ms trên máy hiện tại. Đây là kết quả tại thời điểm kiểm tra, không phải số cố định trong code.
