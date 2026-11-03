package com.quanlynhakhoa.ui.component;

import java.awt.Font;
import javax.swing.JLabel;

public final class FitLabel extends JLabel {

    private final Font preferred;
    private final int minimum;

    public FitLabel(String text, Font preferred, int minimum) {
        super(text);
        this.preferred = preferred;
        this.minimum = minimum;
        setFont(preferred);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (preferred == null || width <= 0) {
            return;
        }
        Font fitted = preferred;
        int available = width - getInsets().left - getInsets().right;
        int availableHeight = height - getInsets().top - getInsets().bottom;
        while (fitted.getSize() > minimum && (getFontMetrics(fitted).stringWidth(getText()) > available
                || getFontMetrics(fitted).getHeight() > availableHeight)) {
            fitted = fitted.deriveFont((float) fitted.getSize() - 1);
        }
        if (!fitted.equals(getFont())) {
            setFont(fitted);
        }
    }
}
