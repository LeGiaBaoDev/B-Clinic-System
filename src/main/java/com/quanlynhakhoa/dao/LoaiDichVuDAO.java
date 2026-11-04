package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.util.List;

public class LoaiDichVuDAO {

    private static final String SQL_SELECT = """
            SELECT maLoaiDV, tenLoaiDV, mucThueGTGT
            FROM LoaiDichVu
            """;

    public List<LoaiDichVu> layTatCa() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT + " ORDER BY maLoaiDV", this::mapRow);
        });
    }

    public LoaiDichVu layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.one(c, SQL_SELECT + " WHERE maLoaiDV=?", this::mapRow, ma);
        });
    }

    LoaiDichVu layHoacTao(Connection c, LoaiDichVu loai) throws SQLException {
        new LoaiDichVu(loai.getMaLoaiDV(), loai.getTenLoaiDV(), loai.getMucThueGTGT());
        if (loai.getMaLoaiDV() != null) {
            LoaiDichVu daLuu = JdbcHelper.one(c, SQL_SELECT + " WHERE maLoaiDV=?", this::mapRow,
                    loai.getMaLoaiDV());
            yeuCau(daLuu != null, "Loại dịch vụ không tồn tại.");
            return daLuu;
        }
        MaSinh.khoa(c, "BClinic:loai-dich-vu");
        LoaiDichVu daLuu = JdbcHelper.one(c, SQL_SELECT + " WITH(UPDLOCK,HOLDLOCK) WHERE tenLoaiDV=?",
                this::mapRow, loai.getTenLoaiDV());
        if (daLuu != null) {
            return daLuu;
        }
        String ma = MaSinh.maMoi(c, MaSinh.Bang.LOAI_DICH_VU, null);
        JdbcHelper.update(c, "INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES(?,?,?)",
                ma, loai.getTenLoaiDV(), thue(loai.getMucThueGTGT()));
        NhatKy.ghi(c, "THEM_LOAI_DICH_VU", ma);
        return new LoaiDichVu(ma, loai.getTenLoaiDV(), loai.getMucThueGTGT());
    }

    public boolean them(LoaiDichVu loai) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            new LoaiDichVu(null, loai.getTenLoaiDV(), loai.getMucThueGTGT());
            MaSinh.khoa(c, "BClinic:loai-dich-vu");
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LoaiDichVu WHERE tenLoaiDV=?",
                    loai.getTenLoaiDV()), "Loại dịch vụ đã tồn tại.");

            String ma = MaSinh.maMoi(c, MaSinh.Bang.LOAI_DICH_VU, null);
            JdbcHelper.update(c, "INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES(?,?,?)",
                    ma, loai.getTenLoaiDV(), thue(loai.getMucThueGTGT()));
            loai.setMaLoaiDV(ma);
            NhatKy.ghi(c, "THEM_LOAI_DICH_VU", ma);
            return true;
        });
    }

    public boolean capNhat(LoaiDichVu loai) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            new LoaiDichVu(loai.getMaLoaiDV(), loai.getTenLoaiDV(), loai.getMucThueGTGT());
            MaSinh.khoa(c, "BClinic:loai-dich-vu");
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LoaiDichVu WHERE tenLoaiDV=? AND maLoaiDV<>?",
                    loai.getTenLoaiDV(), loai.getMaLoaiDV()), "Loại dịch vụ đã tồn tại.");

            return JdbcHelper.update(c, "UPDATE LoaiDichVu SET tenLoaiDV=?,mucThueGTGT=? WHERE maLoaiDV=?",
                    loai.getTenLoaiDV(), thue(loai.getMucThueGTGT()), loai.getMaLoaiDV()) == 1;
        });
    }

    private LoaiDichVu mapRow(ResultSet r) throws SQLException {
        return new LoaiDichVu(
                r.getString("maLoaiDV"),
                r.getString("tenLoaiDV"),
                mucThue(r.getInt("mucThueGTGT")));
    }

    static int thue(MucThue value) {
        return switch (value) {
            case THUE_0 ->
                0;
            case THUE_5 ->
                5;
            case THUE_10 ->
                10;
        };
    }

    private static MucThue mucThue(int value) {
        return switch (value) {
            case 0 ->
                MucThue.THUE_0;
            case 5 ->
                MucThue.THUE_5;
            case 10 ->
                MucThue.THUE_10;
            default ->
                throw new BusinessException("Mức thuế GTGT không hợp lệ: " + value);
        };
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
