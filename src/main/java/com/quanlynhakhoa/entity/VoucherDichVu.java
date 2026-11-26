package com.quanlynhakhoa.entity;

import java.util.Objects;

public class VoucherDichVu {
    private Voucher voucher;
    private DichVu dichVu;
    public VoucherDichVu() { }
    public VoucherDichVu(Voucher voucher,DichVu dichVu) { setVoucher(voucher);setDichVu(dichVu); }
    public VoucherDichVu(VoucherDichVu o) { voucher=o.voucher;dichVu=o.dichVu; }
    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher value) { voucher=Objects.requireNonNull(value,"Thiếu voucher."); }
    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu value) { dichVu=Objects.requireNonNull(value,"Thiếu dịch vụ."); }
    @Override public boolean equals(Object o) {
        return this==o || o instanceof VoucherDichVu v && voucher!=null && dichVu!=null
                && Objects.equals(voucher,v.voucher) && Objects.equals(dichVu,v.dichVu);
    }
    @Override public int hashCode() { return Objects.hash(voucher,dichVu); }
    @Override public String toString() { return "VoucherDichVu[voucher="+voucher+", dichVu="+dichVu+"]"; }
}
