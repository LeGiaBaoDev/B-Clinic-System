package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.MucThue;

import java.util.Objects;

public class LoaiDichVu {
    public static final String DIEU_TRI = "Điều trị bệnh lý";
    public static final String VAT_TU = "Vật tư/thiết bị bán kèm";
    public static final String THAM_MY = "Thẩm mỹ";

    private String maLoaiDV;
    private String tenLoaiDV;
    private MucThue mucThueGTGT;

    public LoaiDichVu() {
    }

    public LoaiDichVu(String maLoaiDV, String tenLoaiDV, MucThue mucThueGTGT) {
        setMaLoaiDV(maLoaiDV);
        setTenLoaiDV(tenLoaiDV);
        setMucThueGTGT(mucThueGTGT);
    }

    public LoaiDichVu(LoaiDichVu other) {
        this.maLoaiDV = other.maLoaiDV;
        this.tenLoaiDV = other.tenLoaiDV;
        this.mucThueGTGT = other.mucThueGTGT;
    }

    public String getMaLoaiDV() {
        return maLoaiDV;
    }

    public void setMaLoaiDV(String maLoaiDV) {
        if (maLoaiDV != null && !maLoaiDV.matches("LDV\\d{2}")) {
            throw new IllegalArgumentException("Mã loại dịch vụ không đúng định dạng");
        }
        this.maLoaiDV = maLoaiDV;
    }

    public String getTenLoaiDV() {
        return tenLoaiDV;
    }

    public void setTenLoaiDV(String tenLoaiDV) {
        if (tenLoaiDV == null || tenLoaiDV.isBlank()) {
            throw new IllegalArgumentException("Tên loại dịch vụ không được rỗng");
        }
        String ten = tenLoaiDV.trim();
        if (!ten.equals(DIEU_TRI) && !ten.equals(VAT_TU) && !ten.equals(THAM_MY)) {
            throw new IllegalArgumentException("Tên loại dịch vụ chỉ nhận: " + DIEU_TRI + ", " + VAT_TU + ", " + THAM_MY);
        }
        if (mucThueGTGT != null && mucThueGTGT != mucThueTheoTen(ten)) {
            throw new IllegalArgumentException("Tên loại dịch vụ không khớp mức thuế GTGT");
        }
        this.tenLoaiDV = ten;
    }

    public MucThue getMucThueGTGT() {
        return mucThueGTGT;
    }

    public void setMucThueGTGT(MucThue mucThueGTGT) {
        if (mucThueGTGT == null) {
            throw new IllegalArgumentException("Mức thuế GTGT không được để trống");
        }
        if (tenLoaiDV != null && mucThueGTGT != mucThueTheoTen(tenLoaiDV)) {
            throw new IllegalArgumentException("Mức thuế GTGT không khớp tên loại dịch vụ");
        }
        this.mucThueGTGT = mucThueGTGT;
    }

    public static MucThue mucThueTheoTen(String tenLoaiDV) {
        return switch (tenLoaiDV) {
            case DIEU_TRI -> MucThue.THUE_0;
            case VAT_TU -> MucThue.THUE_5;
            case THAM_MY -> MucThue.THUE_10;
            default -> throw new IllegalArgumentException("Tên loại dịch vụ không hợp lệ: " + tenLoaiDV);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoaiDichVu other)) {
            return false;
        }
        return maLoaiDV != null && maLoaiDV.equals(other.maLoaiDV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maLoaiDV);
    }

    @Override
    public String toString() {
        return "LoaiDichVu[maLoaiDV=" + maLoaiDV + ", tenLoaiDV=" + tenLoaiDV + ", mucThueGTGT=" + mucThueGTGT + "]";
    }
}
