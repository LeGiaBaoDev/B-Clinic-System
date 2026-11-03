package com.quanlynhakhoa.ui.component;

import java.awt.*;
import javax.swing.JPanel;

public class SurfacePanel extends JPanel {

    private final int radius;

    public SurfacePanel(LayoutManager layout) {
        this(layout, 8);
    }

    public SurfacePanel(LayoutManager layout, int radius) {
        super(layout);
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UiConstants.SURFACE);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g.setColor(UiConstants.BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g.dispose();
        super.paintComponent(graphics);
    }
}
