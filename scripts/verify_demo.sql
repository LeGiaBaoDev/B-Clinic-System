-- Chi doc. Moi kiem tra nghiep vu tra loi bang THROW neu co du lieu sai.
SET NOCOUNT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51100,N'Chọn BClinic_Dev.',1;
IF NOT EXISTS(SELECT 1 FROM NhatKyHeThong WHERE maNhatKy='NK990099' AND hanhDong=N'DEMO_SEED_V1')
    THROW 51101,N'Chưa có dấu hoàn tất bộ dữ liệu mẫu.',1;
IF EXISTS(SELECT 1 FROM KhachHang kh LEFT JOIN HoSoBenhAn hs ON hs.maKH=kh.maKH WHERE hs.maHSBA IS NULL)
    THROW 51102,N'Khách hàng thiếu hồ sơ bệnh án.',1;
IF EXISTS(SELECT 1 FROM LichHen lh JOIN DichVu dv ON dv.maDV=lh.maDV
    WHERE NOT EXISTS(SELECT 1 FROM TaiKhoan tk WHERE tk.maNV=lh.maNV AND tk.vaiTro='NHASI')
    OR NOT EXISTS(SELECT 1 FROM LichLamViec ll WHERE ll.maNV=lh.maNV AND ll.ngayTruc=CAST(lh.ngayGioHen AS DATE)
        AND ll.trangThai='DA_PHAN_CONG' AND CAST(lh.ngayGioHen AS TIME)>=ll.gioBatDau
        AND CAST(DATEADD(MINUTE,dv.thoiLuong,lh.ngayGioHen) AS TIME)<=ll.gioKetThuc)
    OR dv.trangThai=0)
    THROW 51103,N'Lịch hẹn sai vai trò, ngoài ca hoặc dùng dịch vụ ngừng cung cấp.',1;
-- Kiem tra ca lich su HOAN_TAT, khong chi DA_DAT/DA_DEN nhu trigger hien tai.
IF EXISTS(SELECT 1 FROM LichHen a JOIN LichHen b ON a.maLH<b.maLH
    JOIN DichVu da ON da.maDV=a.maDV JOIN DichVu db ON db.maDV=b.maDV
    WHERE a.trangThai NOT IN('DA_HUY','KHONG_DEN') AND b.trangThai NOT IN('DA_HUY','KHONG_DEN')
    AND ((a.maNV=b.maNV AND a.ngayGioHen<DATEADD(MINUTE,db.thoiLuong+15,b.ngayGioHen)
        AND b.ngayGioHen<DATEADD(MINUTE,da.thoiLuong+15,a.ngayGioHen))
      OR (a.maKH=b.maKH AND a.ngayGioHen<DATEADD(MINUTE,db.thoiLuong,b.ngayGioHen)
        AND b.ngayGioHen<DATEADD(MINUTE,da.thoiLuong,a.ngayGioHen))))
    THROW 51104,N'Trùng lịch hoặc thiếu khoảng cách 15 phút.',1;
IF EXISTS(SELECT 1 FROM LichHen lh JOIN PhieuKham src ON src.maPK=lh.maPKNguon
    JOIN LichHen old ON old.maLH=src.maLH WHERE src.trangThai<>'HOAN_TAT'
    OR lh.maNV<>old.maNV OR lh.maKH<>old.maKH OR lh.maDV<>old.maDV
    OR DATEDIFF(DAY,src.ngayLap,lh.ngayGioHen) NOT BETWEEN 7 AND 14)
    THROW 51105,N'Tái khám không khớp phiếu nguồn/nha sĩ/khoảng 7–14 ngày.',1;
IF EXISTS(SELECT 1 FROM PhieuKham pk JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA
    JOIN LichHen lh ON lh.maLH=pk.maLH WHERE hs.maKH<>lh.maKH
    OR NOT EXISTS(SELECT 1 FROM TaiKhoan tk WHERE tk.maNV=pk.maNV_LapPhieu AND tk.vaiTro='LETAN'))
    THROW 51106,N'Phiếu sai khách hàng hoặc nhân viên tiếp nhận.',1;
IF EXISTS(SELECT 1 FROM ChiTietPhieuKham ct JOIN PhieuKham pk ON pk.maPK=ct.maPK
    JOIN LichHen lh ON lh.maLH=pk.maLH JOIN DichVu dv ON dv.maDV=ct.maDV
    JOIN LoaiDichVu ld ON ld.maLoaiDV=dv.maLoaiDV
    WHERE ct.maNV_NhaSi<>lh.maNV OR ct.mucThueGTGT<>ld.mucThueGTGT OR ct.donGia<>dv.donGia)
    THROW 51107,N'Chi tiết sai nha sĩ, đơn giá hoặc thuế chụp tại thời điểm seed.',1;
IF EXISTS(SELECT 1 FROM PhieuKham pk LEFT JOIN HoaDon hd ON hd.maPK=pk.maPK
    WHERE pk.trangThai='HOAN_TAT' AND (hd.maHD IS NULL OR NOT EXISTS(SELECT 1 FROM ChiTietPhieuKham ct WHERE ct.maPK=pk.maPK)))
    THROW 51108,N'Phiếu hoàn tất thiếu chi tiết hoặc hóa đơn.',1;
IF EXISTS(SELECT 1 FROM HoaDon hd JOIN PhieuKham pk ON pk.maPK=hd.maPK
    JOIN LichHen lh ON lh.maLH=pk.maLH WHERE pk.trangThai<>'HOAN_TAT'
    OR (hd.trangThai=1 AND (lh.trangThai<>'HOAN_TAT' OR NOT EXISTS(SELECT 1 FROM TaiKhoan tk WHERE tk.maNV=hd.maNV_ThuNgan AND tk.vaiTro='LETAN')))
    OR (hd.trangThai=0 AND lh.trangThai<>'DA_DEN'))
    THROW 51109,N'Trạng thái hóa đơn/phiếu/lịch hoặc vai trò thu ngân không khớp.',1;
IF EXISTS(SELECT 1 FROM HoaDon hd JOIN Voucher vc ON vc.maVoucher=hd.maVoucher
    CROSS APPLY(SELECT SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0)) AS tong
        FROM ChiTietPhieuKham ct WHERE ct.maPK=hd.maPK) t
    CROSS APPLY(SELECT CASE WHEN vc.loaiGiam=1 THEN ROUND(t.tong*vc.giaGiam/100,0) ELSE vc.giaGiam END AS giam) g
    CROSS APPLY(SELECT CASE WHEN vc.loaiGiam=1 AND vc.giaTriGiamToiDa>0 AND g.giam>vc.giaTriGiamToiDa
        THEN vc.giaTriGiamToiDa ELSE g.giam END AS giam) capped
    WHERE vc.trangThai=0 OR CAST(hd.ngayLap AS DATE) NOT BETWEEN vc.ngayBatDau AND vc.ngayKetThuc
    OR hd.tongKhuyenMai<>CASE WHEN capped.giam>t.tong THEN t.tong ELSE capped.giam END)
    THROW 51110,N'Voucher ngoài hiệu lực hoặc số tiền giảm sai.',1;
IF EXISTS(SELECT 1 FROM LichHen lh WHERE lh.trangThai='DA_DAT'
    AND lh.ngayGioHen<SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time')
    THROW 51111,N'Lịch mẫu DA_DAT đã quá giờ; cần cập nhật trạng thái khi sử dụng lại bộ dữ liệu cũ.',1;
IF EXISTS(SELECT 1 FROM LichHen lh WHERE lh.trangThai NOT IN('DA_HUY','KHONG_DEN')
    AND (SELECT COUNT(*) FROM LichHen huy WHERE huy.maKH=lh.maKH AND huy.trangThai='DA_HUY'
        AND huy.ngayGioHen<lh.ngayGioHen
        AND YEAR(huy.ngayGioHen)=YEAR(lh.ngayGioHen) AND MONTH(huy.ngayGioHen)=MONTH(lh.ngayGioHen))>=3
    AND CAST(lh.ngayGioHen AS TIME)<>'07:00' AND DATEADD(MINUTE,(SELECT thoiLuong FROM DichVu WHERE maDV=lh.maDV),CAST(lh.ngayGioHen AS TIME))<>'17:30')
    THROW 51112,N'Khách hủy nhiều lần không ở khung đầu/cuối ngày.',1;
SELECT N'PASS: kiểm tra liên kết, phân quyền dữ liệu, lịch, tái khám, giá/thuế, voucher và thanh toán.' AS ketQua;
SELECT trangThai,COUNT(*) AS soLuong FROM LichHen GROUP BY trangThai;
SELECT trangThai,COUNT(*) AS soLuong FROM PhieuKham GROUP BY trangThai;
SELECT trangThai,hinhThucThanhToan,COUNT(*) AS soLuong FROM HoaDon GROUP BY trangThai,hinhThucThanhToan;
