package com.quanlynhakhoa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/*
 * UC009–UC011 – Khách hàng (Bảng 4.3)
 * [x] mã KH đúng "KH" + 5 số, cho phép null khi chưa sinh mã
 * [x] tên không rỗng, được trim
 * [x] ngày sinh không ở tương lai, cho phép null
 * [x] SĐT đúng 10 số bắt đầu bằng 0
 * [x] email rỗng -> null, sai định dạng -> lỗi
 * [x] địa chỉ rỗng -> null
 * [x] equals/hashCode theo mã
 * [x] constructor sao chép
 */
class KhachHangTest {

    private static KhachHang taoHopLe() {
        return new KhachHang("KH00001", "Nguyễn Văn An", LocalDate.of(1995, 5, 20), true,
                "0912345678", "Quận 1", "an@example.com");
    }

    @Test
    void constructor_duLieuHopLe_gachDungGiaTri() {
        KhachHang kh = taoHopLe();

        assertEquals("KH00001", kh.getMaKH());
        assertEquals("Nguyễn Văn An", kh.getTenKH());
        assertEquals(LocalDate.of(1995, 5, 20), kh.getNgaySinh());
        assertTrue(kh.isGioiTinh());
        assertEquals("0912345678", kh.getSdt());
        assertEquals("Quận 1", kh.getDiaChi());
        assertEquals("an@example.com", kh.getEmail());
    }

    @Test
    void setMaKH_null_choPhepKhiChuaSinhMa() {
        KhachHang kh = taoHopLe();
        kh.setMaKH(null);
        assertNull(kh.getMaKH());
    }

    @ParameterizedTest
    @ValueSource(strings = {"KH0001", "KH000001", "kh00001", "HS00001", "KH0000A", ""})
    void setMaKH_saiDinhDang_nemLoi(String ma) {
        KhachHang kh = new KhachHang();
        assertThrows(IllegalArgumentException.class, () -> kh.setMaKH(ma));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void setTenKH_rongHoacToanKhoangTrang_nemLoi(String ten) {
        KhachHang kh = new KhachHang();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> kh.setTenKH(ten));
        assertEquals("Tên khách hàng không được rỗng", ex.getMessage());
    }

    @Test
    void setTenKH_null_nemLoi() {
        KhachHang kh = new KhachHang();
        assertThrows(IllegalArgumentException.class, () -> kh.setTenKH(null));
    }

    @Test
    void setTenKH_coKhoangTrangDauCuoi_duocCat() {
        KhachHang kh = new KhachHang();
        kh.setTenKH("  Trần Thị Bình  ");
        assertEquals("Trần Thị Bình", kh.getTenKH());
    }

    @Test
    void setNgaySinh_ngayTuongLai_nemLoi() {
        KhachHang kh = new KhachHang();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> kh.setNgaySinh(LocalDate.now().plusDays(1)));
        assertEquals("Ngày sinh không hợp lệ", ex.getMessage());
    }

    @Test
    void setNgaySinh_homNay_hopLe() {
        KhachHang kh = new KhachHang();
        LocalDate homNay = LocalDate.now();
        kh.setNgaySinh(homNay);
        assertEquals(homNay, kh.getNgaySinh());
    }

    @Test
    void setNgaySinh_null_choPhep() {
        KhachHang kh = taoHopLe();
        kh.setNgaySinh(null);
        assertNull(kh.getNgaySinh());
    }

    @ParameterizedTest
    @CsvSource({
        "0912345678,true",
        "0398682230,true",
        "091234567,false",
        "09123456789,false",
        "1912345678,false",
        "09123abcde,false",
        "'',false"
    })
    void setSdt_kiemTraDinhDang(String sdt, boolean hopLe) {
        KhachHang kh = new KhachHang();
        if (hopLe) {
            kh.setSdt(sdt);
            assertEquals(sdt, kh.getSdt());
        } else {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> kh.setSdt(sdt));
            assertEquals("Số điện thoại phải gồm đúng 10 chữ số", ex.getMessage());
        }
    }

    @Test
    void setSdt_null_nemLoi() {
        KhachHang kh = new KhachHang();
        assertThrows(IllegalArgumentException.class, () -> kh.setSdt(null));
    }

    @Test
    void setSdt_coKhoangTrangDauCuoi_duocCat() {
        KhachHang kh = new KhachHang();
        kh.setSdt(" 0912345678 ");
        assertEquals("0912345678", kh.getSdt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"an@", "an.example.com", "@example.com", "an@example", "an @example.com"})
    void setEmail_saiDinhDang_nemLoi(String email) {
        KhachHang kh = new KhachHang();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> kh.setEmail(email));
        assertEquals("Email không hợp lệ", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void setEmail_rongHoacKhoangTrang_thanhNull(String email) {
        KhachHang kh = taoHopLe();
        kh.setEmail(email);
        assertNull(kh.getEmail());
    }

    @Test
    void setEmail_null_thanhNull() {
        KhachHang kh = taoHopLe();
        kh.setEmail(null);
        assertNull(kh.getEmail());
    }

    @Test
    void setDiaChi_rong_thanhNull() {
        KhachHang kh = taoHopLe();
        kh.setDiaChi("   ");
        assertNull(kh.getDiaChi());
    }

    @Test
    void constructor_sdtSai_nemLoiTruocKhiTaoDoiTuong() {
        assertThrows(IllegalArgumentException.class, () -> new KhachHang(null, "An", null, true,
                "123", null, null));
    }

    @Test
    void equals_cungMaKH_laBang() {
        KhachHang a = taoHopLe();
        KhachHang b = new KhachHang("KH00001", "Tên khác", null, false, "0987654321", null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_khacMaKH_khongBang() {
        KhachHang a = taoHopLe();
        KhachHang b = new KhachHang("KH00002", "Nguyễn Văn An", null, true, "0912345678", null, null);

        assertFalse(a.equals(b));
    }

    @Test
    void equals_chuaCoMa_khongBangNhau() {
        KhachHang a = new KhachHang(null, "An", null, true, "0912345678", null, null);
        KhachHang b = new KhachHang(null, "An", null, true, "0912345678", null, null);

        assertFalse(a.equals(b));
    }

    @Test
    void constructorSaoChep_saoChepDayDuGiaTri() {
        KhachHang goc = taoHopLe();
        KhachHang banSao = new KhachHang(goc);

        assertEquals(goc, banSao);
        assertEquals(goc.getTenKH(), banSao.getTenKH());
        assertEquals(goc.getNgaySinh(), banSao.getNgaySinh());
        assertEquals(goc.getSdt(), banSao.getSdt());
        assertEquals(goc.getEmail(), banSao.getEmail());
        assertEquals(goc.getDiaChi(), banSao.getDiaChi());
    }
}
