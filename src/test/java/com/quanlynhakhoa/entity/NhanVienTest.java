package com.quanlynhakhoa.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NhanVienTest {
    @Test
    void hoTen_phaiCoItNhatHaiTu() {
        NhanVien nv = new NhanVien();
        assertThrows(IllegalArgumentException.class, () -> nv.setHoTenNV("Anh"));
        assertThrows(IllegalArgumentException.class, () -> nv.setHoTenNV("  Anh  "));
        assertThrows(IllegalArgumentException.class, () -> nv.setHoTenNV("   "));
        assertThrows(IllegalArgumentException.class, () -> nv.setHoTenNV(null));
        assertDoesNotThrow(() -> nv.setHoTenNV("Văn Anh"));
        assertDoesNotThrow(() -> nv.setHoTenNV("Nguyễn\u00a0Văn\tAnh"));
        assertThrows(IllegalArgumentException.class, () -> nv.setHoTenNV("Nguyễn " + "a".repeat(100)));
    }
}
