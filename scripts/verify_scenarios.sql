SET NOCOUNT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51300,N'Chọn BClinic_Dev.',1;
DECLARE @ngay DATE=(SELECT CAST(thoiDiem AS DATE) FROM NhatKyHeThong WHERE maNhatKy='NK980099' AND hanhDong=N'DEMO_SCENARIOS_V2');
IF @ngay IS NULL THROW 51301,N'Chưa nạp bộ nhiều tình huống.',1;
IF (SELECT COUNT(DISTINCT caTruc) FROM LichLamViec)<>3 THROW 51302,N'Chưa đủ ba loại ca.',1;
IF (SELECT COUNT(DISTINCT trangThai) FROM LichHen)<>5 THROW 51303,N'Chưa đủ năm trạng thái lịch hẹn.',1;
IF (SELECT COUNT(DISTINCT trangThai) FROM PhieuKham)<>3 THROW 51304,N'Chưa đủ ba trạng thái phiếu khám.',1;
IF (SELECT COUNT(DISTINCT hinhThucThanhToan) FROM HoaDon WHERE trangThai=1)<>2 THROW 51305,N'Chưa đủ tiền mặt/chuyển khoản.',1;
IF NOT EXISTS(SELECT 1 FROM TaiKhoan WHERE maTK='TK105' AND trangThai=0) THROW 51306,N'Thiếu tài khoản khóa.',1;
IF EXISTS(SELECT 1 FROM TaiKhoan WHERE maTK BETWEEN 'TK101' AND 'TK106' AND matKhau NOT LIKE 'PBKDF2$%')
    THROW 51307,N'Mật khẩu mới chưa dùng PBKDF2.',1;
IF (SELECT COUNT(DISTINCT matKhau) FROM TaiKhoan WHERE maTK BETWEEN 'TK101' AND 'TK106')<>6
    THROW 51308,N'Các tài khoản mới phải có salt khác nhau.',1;
IF (SELECT COUNT(DISTINCT CAST(hd.ngayThanhToan AS DATE)) FROM HoaDon hd
    WHERE hd.maNV_ThuNgan='NV106' AND hd.trangThai=1 AND hd.ngayThanhToan>=DATEADD(DAY,-13,@ngay) AND hd.ngayThanhToan<DATEADD(DAY,1,@ngay))<13
    THROW 51309,N'Chưa có doanh thu trên các ngày lịch sử.',1;
IF NOT EXISTS(SELECT 1 FROM HoaDon hd CROSS APPLY(
    SELECT SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0)) tong FROM ChiTietPhieuKham ct WHERE ct.maPK=hd.maPK) t
    WHERE hd.maVoucher='VC102' AND hd.trangThai=1 AND hd.tongKhuyenMai=t.tong)
    THROW 51310,N'Thiếu trường hợp giảm bằng toàn bộ giá trị hóa đơn.',1;
IF NOT EXISTS(SELECT 1 FROM LichHen lh JOIN PhieuKham pk ON pk.maPK=lh.maPKNguon
    WHERE lh.maNV='NV101' AND pk.trangThai='HOAN_TAT' AND DATEDIFF(DAY,pk.ngayLap,lh.ngayGioHen) BETWEEN 7 AND 14)
    THROW 51311,N'Thiếu tình huống tái khám liên kết.',1;
IF EXISTS(SELECT 1 FROM LichLamViec a JOIN LichLamViec b ON a.maNV=b.maNV AND a.ngayTruc=b.ngayTruc AND a.maLLV<b.maLLV
    WHERE a.gioBatDau<b.gioKetThuc AND b.gioBatDau<a.gioKetThuc)
    THROW 51312,N'Có nhân viên bị trùng ca.',1;
IF EXISTS(SELECT 1 FROM LichHen lh JOIN TaiKhoan tk ON tk.maNV=lh.maNV WHERE tk.vaiTro<>'NHASI' OR tk.trangThai=0)
    THROW 51313,N'Lịch hẹn gán sai vai trò hoặc tài khoản khóa.',1;
IF EXISTS(SELECT 1 FROM HoaDon hd JOIN TaiKhoan tk ON tk.maNV=hd.maNV_ThuNgan WHERE tk.vaiTro<>'LETAN')
    THROW 51314,N'Nhân viên thu ngân sai vai trò.',1;
IF EXISTS(SELECT 1 FROM ChiTietPhieuKham ct JOIN HoaDon hd ON hd.maPK=ct.maPK GROUP BY hd.maHD,hd.tongKhuyenMai
    HAVING SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0))-hd.tongKhuyenMai<0)
    THROW 51315,N'Tổng hóa đơn âm.',1;

SELECT N'PASS: dữ liệu nhiều tình huống và liên kết nghiệp vụ.' AS ketQua;
SELECT caTruc,trangThai,COUNT(*) AS soLuong FROM LichLamViec GROUP BY caTruc,trangThai;
SELECT trangThai,COUNT(*) AS soLuong FROM LichHen GROUP BY trangThai;
SELECT trangThai,COUNT(*) AS soLuong FROM PhieuKham GROUP BY trangThai;
SELECT trangThai,hinhThucThanhToan,COUNT(*) AS soLuong FROM HoaDon GROUP BY trangThai,hinhThucThanhToan;
SELECT CAST(hd.ngayThanhToan AS DATE) AS ngay,COUNT(*) AS hoaDon,
    SUM(t.tong-hd.tongKhuyenMai) AS doanhThu
FROM HoaDon hd CROSS APPLY(SELECT SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0)) tong FROM ChiTietPhieuKham ct WHERE ct.maPK=hd.maPK) t
WHERE hd.trangThai=1 AND hd.ngayThanhToan>=DATEADD(DAY,-13,@ngay)
GROUP BY CAST(hd.ngayThanhToan AS DATE) ORDER BY ngay;
