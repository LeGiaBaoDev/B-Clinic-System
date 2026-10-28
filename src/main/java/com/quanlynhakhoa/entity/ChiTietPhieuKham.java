package com.quanlynhakhoa.entity;

import java.util.Objects;

public class ChiTietPhieuKham {
    private int maCTPK;
    private String trieuChung;
    private String chanDoan;
    private double donGia;
    private int soLuong;
    private String donViTinh;
    private double mucThueGTGT;
    private String ketQuaDieuTri;
    private PhieuKham phieuKham;
    private DichVu dichVu;
    private NhanVien nhaSi;

    public ChiTietPhieuKham() {
    }

    public ChiTietPhieuKham(int maCTPK, String trieuChung, String chanDoan, double donGia, int soLuong, String donViTinh, double mucThueGTGT, String ketQuaDieuTri, PhieuKham phieuKham, DichVu dichVu, NhanVien nhaSi) {
        setMaCTPK(maCTPK);
        setTrieuChung(trieuChung);
        setChanDoan(chanDoan);
        setDonGia(donGia);
        setSoLuong(soLuong);
        setDonViTinh(donViTinh);
        setMucThueGTGT(mucThueGTGT);
        setKetQuaDieuTri(ketQuaDieuTri);
        setPhieuKham(phieuKham);
        setDichVu(dichVu);
        setNhaSi(nhaSi);
    }

    public ChiTietPhieuKham(ChiTietPhieuKham other) {
        this.maCTPK = other.maCTPK;
        this.trieuChung = other.trieuChung;
        this.chanDoan = other.chanDoan;
        this.donGia = other.donGia;
        this.soLuong = other.soLuong;
        this.donViTinh = other.donViTinh;
        this.mucThueGTGT = other.mucThueGTGT;
        this.ketQuaDieuTri = other.ketQuaDieuTri;
        this.phieuKham = other.phieuKham;
        this.dichVu = other.dichVu;
        this.nhaSi = other.nhaSi;
    }

    public int getMaCTPK() {
        return maCTPK;
    }

    public void setMaCTPK(int maCTPK) {
        this.maCTPK = maCTPK;
    }

    public String getTrieuChung() {
        return trieuChung;
    }

    public void setTrieuChung(String trieuChung) {
        if (trieuChung == null || trieuChung.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập triệu chứng và chẩn đoán");
        }
        this.trieuChung = trieuChung.trim();
    }

    public String getChanDoan() {
        return chanDoan;
    }

    public void setChanDoan(String chanDoan) {
        if (chanDoan == null || chanDoan.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập triệu chứng và chẩn đoán");
        }
        this.chanDoan = chanDoan.trim();
    }

    public double getDonGia() {
        return donGia;
    }

    public void setDonGia(double donGia) {
        if (donGia <= 0) {
            throw new IllegalArgumentException("Đơn giá phải lớn hơn 0");
        }
        this.donGia = donGia;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
        this.soLuong = soLuong;
    }

    public String getDonViTinh() {
        return donViTinh;
    }

    public void setDonViTinh(String donViTinh) {
        if (donViTinh == null || donViTinh.isBlank()) {
            throw new IllegalArgumentException("Đơn vị tính không được rỗng");
        }
        this.donViTinh = donViTinh.trim();
    }

    public double getMucThueGTGT() {
        return mucThueGTGT;
    }

    public void setMucThueGTGT(double mucThueGTGT) {
        if (mucThueGTGT != 0 && mucThueGTGT != 5 && mucThueGTGT != 10) {
            throw new IllegalArgumentException("Mức thuế GTGT chỉ nhận 0, 5 hoặc 10");
        }
        this.mucThueGTGT = mucThueGTGT;
    }

    public String getKetQuaDieuTri() {
        return ketQuaDieuTri;
    }

    public void setKetQuaDieuTri(String ketQuaDieuTri) {
        this.ketQuaDieuTri = (ketQuaDieuTri == null || ketQuaDieuTri.isBlank()) ? null : ketQuaDieuTri.trim();
    }

    public PhieuKham getPhieuKham() {
        return phieuKham;
    }

    public void setPhieuKham(PhieuKham phieuKham) {
        if (phieuKham == null) {
            throw new IllegalArgumentException("Chi tiết phải thuộc một phiếu khám");
        }
        this.phieuKham = phieuKham;
    }

    public DichVu getDichVu() {
        return dichVu;
    }

    public void setDichVu(DichVu dichVu) {
        if (dichVu == null) {
            throw new IllegalArgumentException("Phải chọn dịch vụ");
        }
        this.dichVu = dichVu;
    }

    public NhanVien getNhaSi() {
        return nhaSi;
    }

    public void setNhaSi(NhanVien nhaSi) {
        if (nhaSi == null) {
            throw new IllegalArgumentException("Phải có nha sĩ chỉ định");
        }
        this.nhaSi = nhaSi;
    }

    public double getThanhTien() {
        return soLuong * donGia;
    }

    public double getTienThue() {
        return Math.round(getThanhTien() * mucThueGTGT / 100);
    }

    public double getThanhTienSauThue() {
        return getThanhTien() + getTienThue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChiTietPhieuKham other)) {
            return false;
        }
        return maCTPK == other.maCTPK;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maCTPK);
    }

    @Override
    public String toString() {
        return "ChiTietPhieuKham[maCTPK=" + maCTPK + ", chanDoan=" + chanDoan + ", soLuong=" + soLuong + "]";
    }
}
