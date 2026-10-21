package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.MucThue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoaiDichVu {
    private String maLoaiDV;
    private String tenLoaiDV;
    private MucThue mucThueGTGT;
}
