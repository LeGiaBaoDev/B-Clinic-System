package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.BaoCaoQuanLyDAO.*;
import com.quanlynhakhoa.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaoCaoQuanLyDAOTest {
    @Test void tuan_batDauThuHai_kyTruocKhongGiaoNhau() {
        var ky = KyBaoCao.tao(CheDo.TUAN, LocalDate.of(2026, 11, 5), null);
        assertEquals(LocalDate.of(2026, 11, 2), ky.hienTai().tu());
        assertEquals(LocalDate.of(2026, 11, 8), ky.hienTai().den());
        assertEquals(LocalDate.of(2026, 10, 26), ky.kyTruoc().tu());
        assertEquals(LocalDate.of(2026, 11, 1), ky.kyTruoc().den());
    }

    @Test void thangNhuan_soSanhThangLichLienTruoc() {
        var ky = KyBaoCao.tao(CheDo.THANG, LocalDate.of(2024, 3, 15), null);
        assertEquals(LocalDate.of(2024, 2, 1), ky.kyTruoc().tu());
        assertEquals(LocalDate.of(2024, 2, 29), ky.kyTruoc().den());
        assertEquals(31, ky.hienTai().soNgay());
    }

    @Test void tuyChon_kyTruocCungDoDai_vaQuaNam() {
        var ky = KyBaoCao.tao(CheDo.TUY_CHON, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));
        assertEquals(LocalDate.of(2025, 12, 29), ky.kyTruoc().tu());
        assertEquals(LocalDate.of(2025, 12, 31), ky.kyTruoc().den());
    }

    @Test void khoangNgaySai_biTuChoiTruocKhiKetNoiDb() {
        LocalDate day = LocalDate.of(2026, 11, 5);
        assertThrows(BusinessException.class, () -> KyBaoCao.tao(CheDo.TUY_CHON, day, day.minusDays(1)));
        assertThrows(BusinessException.class, () -> KyBaoCao.tao(CheDo.TUY_CHON, day, day.plusDays(366)));
        assertThrows(BusinessException.class, () -> KyBaoCao.tao(CheDo.NGAY, null, null));
    }

    @Test void tyLeHoanTat_chiTinhLichDaKetThuc_khongChiaChoKhong() {
        var stats = new KetQuaLich(100, 50, 20, 24, 4, 2);
        assertEquals(80.0, stats.tyLeHoanTat(), 0.001);
        assertEquals(0.0, KetQuaLich.TRONG.tyLeHoanTat());
    }
}
