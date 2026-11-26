package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.quanlynhakhoa.entity.enums.TrangThaiThanhToan;

public class HoaDon {
    public static final String TIEN_MAT = "Tiền mặt";
    public static final String CHUYEN_KHOAN = "Chuyển khoản";

    private String maHD;
    private LocalDateTime ngayLap = LocalDateTime.now();
    private boolean trangThai;
    private String hinhThucThanhToan;
    private LocalDateTime ngayThanhToan;
    private BigDecimal tongKhuyenMai = BigDecimal.ZERO;
    private KhachHang khachHang;
    private String tenKhachHang;
    private String sdtKhachHang;
    private String diaChiKhachHang;
    private BigDecimal tongTienHang = BigDecimal.ZERO;
    private BigDecimal tongThueGTGT = BigDecimal.ZERO;
    private BigDecimal tongThanhToan = BigDecimal.ZERO;
    private BigDecimal tongDaThu = BigDecimal.ZERO;
    private final List<ChiTietHoaDon> chiTiet = new ArrayList<>();
    private final List<PhieuThu> phieuThu = new ArrayList<>();
    private PhieuKham phieuKham;
    private Voucher voucher;
    private NhanVien thuNgan;

    public HoaDon() {
    }

    public HoaDon(String maHD, LocalDateTime ngayLap, boolean trangThai, String hinhThucThanhToan, LocalDateTime ngayThanhToan, double tongKhuyenMai, PhieuKham phieuKham, Voucher voucher, NhanVien thuNgan) {
        setMaHD(maHD);
        setNgayLap(ngayLap);
        setHinhThucThanhToan(hinhThucThanhToan);
        setNgayThanhToan(ngayThanhToan);
        setTongKhuyenMai(tongKhuyenMai);
        setPhieuKham(phieuKham);
        setVoucher(voucher);
        setThuNgan(thuNgan);
        setTrangThai(trangThai);
    }

    public HoaDon(HoaDon other) {
        this.maHD = other.maHD;
        this.ngayLap = other.ngayLap;
        this.trangThai = other.trangThai;
        this.hinhThucThanhToan = other.hinhThucThanhToan;
        this.ngayThanhToan = other.ngayThanhToan;
        this.tongKhuyenMai = other.tongKhuyenMai;
        this.phieuKham = other.phieuKham;
        this.voucher = other.voucher;
        this.thuNgan = other.thuNgan;
        this.khachHang=other.khachHang; this.tenKhachHang=other.tenKhachHang;
        this.sdtKhachHang=other.sdtKhachHang; this.diaChiKhachHang=other.diaChiKhachHang;
        this.tongTienHang=other.tongTienHang; this.tongThueGTGT=other.tongThueGTGT;
        this.tongThanhToan=other.tongThanhToan; this.tongDaThu=other.tongDaThu;
        for(ChiTietHoaDon d:other.chiTiet) this.chiTiet.add(new ChiTietHoaDon(d));
        for(PhieuThu p:other.phieuThu) this.phieuThu.add(new PhieuThu(p));
    }

    public HoaDon(String ma,LocalDateTime ngayLap,boolean daTra,String hinhThuc,LocalDateTime ngayThu,
            BigDecimal giam,PhieuKham phieu,Voucher voucher,NhanVien thuNgan) {
        this(ma,ngayLap,daTra,hinhThuc,ngayThu,giam.doubleValue(),phieu,voucher,thuNgan);
        this.tongKhuyenMai=ChiTietHoaDon.tien(giam);
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        if (maHD != null && !maHD.matches("HD\\d{9}")) {
            throw new IllegalArgumentException("Mã hóa đơn không đúng định dạng");
        }
        this.maHD = maHD;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        if (ngayLap == null) {
            throw new IllegalArgumentException("Ngày lập không được để trống");
        }
        this.ngayLap = ngayLap;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        if (this.trangThai && !trangThai) {
            throw new IllegalStateException("Hóa đơn đã thanh toán không thể chuyển về chưa thanh toán");
        }
        this.trangThai = trangThai;
    }

    public String getHinhThucThanhToan() {
        return hinhThucThanhToan;
    }

    public void setHinhThucThanhToan(String hinhThucThanhToan) {
        if (hinhThucThanhToan == null || hinhThucThanhToan.isBlank()) {
            this.hinhThucThanhToan = null;
            return;
        }
        String hinhThuc = hinhThucThanhToan.trim();
        if (!hinhThuc.equals(TIEN_MAT) && !hinhThuc.equals(CHUYEN_KHOAN)) {
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ");
        }
        this.hinhThucThanhToan = hinhThuc;
    }

    public LocalDateTime getNgayThanhToan() {
        return ngayThanhToan;
    }

    public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
        if (ngayThanhToan != null && ngayLap != null && ngayThanhToan.isBefore(ngayLap)) {
            throw new IllegalArgumentException("Ngày thanh toán phải sau ngày lập hóa đơn");
        }
        this.ngayThanhToan = ngayThanhToan;
    }

    public double getTongKhuyenMai() {
        return tongKhuyenMai.doubleValue();
    }

    public BigDecimal getTongKhuyenMaiChinhXac() { return tongKhuyenMai; }

    public void setTongKhuyenMai(BigDecimal value) {
        kiemTraChuaThu(); tongKhuyenMai=ChiTietHoaDon.tien(value);
    }

    public void setTongKhuyenMai(double tongKhuyenMai) {
        if (!Double.isFinite(tongKhuyenMai) || tongKhuyenMai < 0) {
            throw new IllegalArgumentException("Tổng khuyến mãi không được âm");
        }
        setTongKhuyenMai(BigDecimal.valueOf(tongKhuyenMai));
    }

    public PhieuKham getPhieuKham() {
        return phieuKham;
    }

    public void setPhieuKham(PhieuKham phieuKham) {
        if (phieuKham == null) {
            throw new IllegalArgumentException("Hóa đơn phải gắn với một phiếu khám");
        }
        this.phieuKham = phieuKham;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    public NhanVien getThuNgan() {
        return thuNgan;
    }

    public void setThuNgan(NhanVien thuNgan) {
        this.thuNgan = thuNgan;
    }

    public double tinhTongTien(List<ChiTietPhieuKham> dsChiTiet) {
        return Math.max(0, PhieuKham.tinhTongChiPhi(dsChiTiet) - tongKhuyenMai.doubleValue());
    }

    public void apDungVoucher(Voucher v, List<ChiTietPhieuKham> dsChiTiet, LocalDate ngay) {
        if (trangThai) {
            throw new IllegalStateException("Hóa đơn đã thanh toán");
        }
        if (voucher != null) {
            throw new IllegalStateException("Mỗi hóa đơn chỉ được áp dụng 1 voucher");
        }
        if (!v.kiemTraHieuLuc(ngay)) {
            throw new IllegalArgumentException("Voucher đã hết hiệu lực");
        }
        this.voucher = v;
        setTongKhuyenMai(v.tinhTienGiam(PhieuKham.tinhTongChiPhi(dsChiTiet)));
    }

    public void thanhToan(String hinhThuc, NhanVien thuNganXacNhan) {
        if (trangThai) {
            throw new IllegalStateException("Hóa đơn đã được thanh toán");
        }
        if (thuNganXacNhan == null) {
            throw new IllegalArgumentException("Phải có nhân viên xác nhận thanh toán");
        }
        setHinhThucThanhToan(hinhThuc);
        if (this.hinhThucThanhToan == null) {
            throw new IllegalArgumentException("Vui lòng chọn hình thức thanh toán");
        }
        setNgayThanhToan(LocalDateTime.now());
        this.thuNgan = thuNganXacNhan;
        this.trangThai = true;
    }

    public static double tinhTienThoi(double tongTien, double soTienKhachDua) {
        if (soTienKhachDua < tongTien) {
            throw new IllegalArgumentException("Số tiền khách đưa chưa đủ");
        }
        return soTienKhachDua - tongTien;
    }

    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang value) { kiemTraChuaThu();khachHang=Objects.requireNonNull(value,"Thiếu khách hàng."); }
    public String getTenKhachHang() { return tenKhachHang; }
    public void setTenKhachHang(String value) { kiemTraChuaThu();tenKhachHang=banChup(value,100,true); }
    public String getSdtKhachHang() { return sdtKhachHang; }
    public void setSdtKhachHang(String value) { kiemTraChuaThu();sdtKhachHang=banChup(value,15,true); }
    public String getDiaChiKhachHang() { return diaChiKhachHang; }
    public void setDiaChiKhachHang(String value) { kiemTraChuaThu();diaChiKhachHang=banChup(value,255,false); }
    public void chotKhachHang(KhachHang value) {
        kiemTraChuaThu();Objects.requireNonNull(value,"Thiếu khách hàng.");
        String ten=banChup(value.getTenKH(),100,true), sdt=banChup(value.getSdt(),15,true);
        String diaChi=banChup(value.getDiaChi(),255,false);
        khachHang=value;tenKhachHang=ten;sdtKhachHang=sdt;diaChiKhachHang=diaChi;
    }
    public BigDecimal getTongTienHang() { return tongTienHang; }
    public BigDecimal getTongThueGTGT() { return tongThueGTGT; }
    public BigDecimal getTongThanhToan() { return tongThanhToan; }
    public BigDecimal getTongDaThu() { return tongDaThu; }
    public BigDecimal getConNo() { return tongThanhToan.subtract(tongDaThu).max(BigDecimal.ZERO); }
    public TrangThaiThanhToan getTrangThaiThanhToan() {
        if(trangThai) return TrangThaiThanhToan.DA_THANH_TOAN;
        return tongDaThu.signum()>0?TrangThaiThanhToan.THANH_TOAN_MOT_PHAN:TrangThaiThanhToan.CHUA_THANH_TOAN;
    }
    public List<ChiTietHoaDon> getChiTiet() { return chiTiet.stream().map(ChiTietHoaDon::new).toList(); }
    public List<PhieuThu> getPhieuThu() { return phieuThu.stream().map(PhieuThu::new).toList(); }
    public void chotChiTiet(List<ChiTietHoaDon> ds) {
        kiemTraChuaThu();
        if(ds==null || ds.isEmpty()) throw new IllegalArgumentException("Hóa đơn phải có dịch vụ.");
        List<ChiTietHoaDon> banSao=new ArrayList<>();
        BigDecimal hang=BigDecimal.ZERO, thue=BigDecimal.ZERO, giam=BigDecimal.ZERO;
        for(ChiTietHoaDon d:ds) {
            Objects.requireNonNull(d,"Dòng dịch vụ bị trống.");
            hang=hang.add(d.getTienHang());thue=thue.add(d.getTienGTGT());giam=giam.add(d.getTienGiam());
            banSao.add(new ChiTietHoaDon(d));
        }
        hang=ChiTietHoaDon.tien(hang);thue=ChiTietHoaDon.tien(thue);giam=ChiTietHoaDon.tien(giam);
        BigDecimal tong=ChiTietHoaDon.tien(hang.add(thue).subtract(giam));
        chiTiet.clear();chiTiet.addAll(banSao);
        tongTienHang=hang;tongThueGTGT=thue;tongKhuyenMai=giam;tongThanhToan=tong;
    }
    public void ghiNhanPhieuThu(PhieuThu value) {
        Objects.requireNonNull(value,"Thiếu phiếu thu.");
        if(trangThai) throw new IllegalStateException("Hóa đơn đã thanh toán.");
        if(value.getHoaDon()!=this && (maHD==null || value.getHoaDon()==null || !maHD.equals(value.getHoaDon().getMaHD()))) throw new IllegalArgumentException("Phiếu thu thuộc hóa đơn khác.");
        if(value.getKhoaYeuCau()==null || value.getThuNgan()==null || value.getHinhThucThanhToan()==null) throw new IllegalArgumentException("Phiếu thu thiếu thông tin.");
        if(value.getNgayThu().isBefore(ngayLap)) throw new IllegalArgumentException("Ngày thu trước ngày lập hóa đơn.");
        if(phieuThu.stream().anyMatch(p->p.getKhoaYeuCau().equals(value.getKhoaYeuCau()))) throw new IllegalStateException("Lần thu đã được ghi nhận.");
        BigDecimal amount=value.getSoTienThu();
        if(amount.signum()<=0 || amount.compareTo(getConNo())>0) throw new IllegalArgumentException("Số tiền vượt công nợ hoặc không hợp lệ.");
        phieuThu.add(new PhieuThu(value));tongDaThu=tongDaThu.add(amount);
        if(tongDaThu.compareTo(tongThanhToan)==0) {
            trangThai=true;ngayThanhToan=value.getNgayThu();hinhThucThanhToan=value.getHinhThucThanhToan();thuNgan=value.getThuNgan();
        }
    }
    private void kiemTraChuaThu() {
        if(trangThai || tongDaThu.signum()>0) throw new IllegalStateException("Nội dung hóa đơn đã chốt sau khi thu tiền.");
    }
    private static String banChup(String value,int max,boolean batBuoc) {
        if(value==null || value.isBlank()) { if(batBuoc) throw new IllegalArgumentException("Thông tin khách hàng không được trống.");return null; }
        if(value.trim().length()>max) throw new IllegalArgumentException("Thông tin khách hàng quá dài.");return value.trim();
    }

    /** Dựng một đối tượng mới từ bản chụp DB, không sửa hóa đơn đã thu tiền. */
    public static HoaDon tuDuLieuTaiChinh(HoaDon base, KhachHang kh, String ten, String sdt,
            String diaChi, BigDecimal hang, BigDecimal thue, BigDecimal giam,
            BigDecimal tong, BigDecimal daThu) {
        HoaDon hd=new HoaDon(base);
        hd.khachHang=Objects.requireNonNull(kh,"Thiếu khách hàng.");
        hd.tenKhachHang=banChup(ten,100,true);hd.sdtKhachHang=banChup(sdt,15,true);
        hd.diaChiKhachHang=banChup(diaChi,255,false);
        hd.tongTienHang=ChiTietHoaDon.tien(hang);hd.tongThueGTGT=ChiTietHoaDon.tien(thue);
        hd.tongKhuyenMai=ChiTietHoaDon.tien(giam);hd.tongThanhToan=ChiTietHoaDon.tien(tong);
        hd.tongDaThu=ChiTietHoaDon.tien(daThu);
        if(hang.add(thue).subtract(giam).compareTo(tong)!=0 || daThu.compareTo(tong)>0)
            throw new IllegalArgumentException("Các tổng tiền hóa đơn không khớp.");
        return hd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HoaDon other)) {
            return false;
        }
        return maHD != null && maHD.equals(other.maHD);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maHD);
    }

    @Override
    public String toString() {
        return "HoaDon[maHD=" + maHD + ", ngayLap=" + ngayLap + ", trangThai=" + trangThai + "]";
    }
}
