package com.quanlynhakhoa.ui.component;

import java.awt.Color;
import java.awt.Font;

/** Bảng màu và typography dùng chung cho giao diện quản lý. */
public final class UiConstants {
    public static final Color BACKGROUND = new Color(0xF3F5F8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(0x24344D);
    public static final Color MUTED = new Color(0x7B8BA5);
    public static final Color BORDER = new Color(0xDCE4EE);
    public static final Color TEAL = new Color(0x008991);
    public static final Color TEAL_DARK = new Color(0x007B83);
    public static final Color TEAL_LIGHT = new Color(0xE7F4F3);
    public static final Color GREEN = new Color(0x16875D);
    private UiConstants() { }

    public static Font font(int size) { return new Font("Segoe UI", Font.PLAIN, size); }
    public static Font bold(int size) { return new Font("Segoe UI", Font.BOLD, size); }
}
