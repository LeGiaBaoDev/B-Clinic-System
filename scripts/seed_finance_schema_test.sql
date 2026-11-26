SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
INSERT KhachHang(maKH,tenKH,gioiTinh,ngaySinh,sdt) VALUES('KH00001',N'Khách Kiểm Thử',1,'19900101','0900000001');
INSERT HoSoBenhAn(maHSBA,maKH) VALUES('HS00001','KH00001');
INSERT LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT,loaiGTGT) VALUES('LDV01',N'Thẩm mỹ',10,N'CHIU_THUE');
INSERT DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai) VALUES('DV001','LDV01',N'Dịch vụ kiểm thử',1000000,30,1);
INSERT PhieuKham(maPK,maHSBA,maNV_LapPhieu,ngayLap,trangThai,maNV_NhaSiPhuTrach) VALUES('PK261120001','HS00001','LT001','20261120 09:00','DANG_KHAM','NS001');
INSERT ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,donViTinh,soLuong,donGia,mucThueGTGT,loaiGTGT)
 VALUES('PK261120001','DV001','NS001',N'Kiểm thử',N'Kiểm thử',N'Lần',1,1000000,10,N'CHIU_THUE');
UPDATE PhieuKham SET trangThai='HOAN_TAT',ngayBatDauKham='20261120 09:00',ngayHoanTat='20261120 09:30' WHERE maPK='PK261120001';
INSERT Voucher(maVoucher,tenVoucher,loaiGiam,giaGiam,ngayBatDau,ngayKetThuc,trangThai) VALUES('VC001',N'Ưu đãi kiểm thử',0,100000,'20261101','20261201',1);
INSERT VoucherDichVu VALUES('VC001','DV001');
INSERT HoaDon(maHD,maPK,maKH,ngayLap,tenKhachHang,sdtKhachHang) VALUES('HD261120001','PK261120001','KH00001','20261120 09:30',N'Khách Kiểm Thử','0900000001');
INSERT ChiTietHoaDon(maHD,maPK,maCTPK,maDV,tenDichVu,donViTinh,soLuong,donGia,loaiGTGT,thueSuatGTGT,tienHang,tienGTGT,tienGiam,thanhTien,maVoucher,tenKhuyenMai,loaiGiam,giaTriGiam)
 SELECT 'HD261120001',maPK,maCTPK,maDV,N'Dịch vụ kiểm thử',N'Lần',1,1000000,N'CHIU_THUE',10,1000000,100000,100000,1000000,'VC001',N'Ưu đãi kiểm thử',0,100000
 FROM ChiTietPhieuKham WHERE maPK='PK261120001';
