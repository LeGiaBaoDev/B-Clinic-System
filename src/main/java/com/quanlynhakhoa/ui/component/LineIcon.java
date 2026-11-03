package com.quanlynhakhoa.ui.component;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/** Icon nét mảnh, vẽ bằng Java2D để sắc nét theo DPI và không phụ thuộc font biểu tượng. */
public final class LineIcon implements Icon {
    public enum Type { GRID, PEOPLE, CLIPBOARD, CALENDAR, CHART, VOUCHER, SETTINGS, SEARCH,
        BELL, CHEVRON, CHECK, INVOICE, PLUS, HELP, ARROW, LOGOUT }
    private final Type type;
    private final Color color;
    private final int size;
    public LineIcon(Type type, Color color, int size) { this.type=type; this.color=color; this.size=size; }
    public int getIconWidth() { return size; }
    public int getIconHeight() { return size; }

    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.translate(x,y);
        g.scale(size/24.0,size/24.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(type) {
            case GRID -> {
                g.draw(new RoundRectangle2D.Double(3,3,7,7,1,1));
                g.draw(new RoundRectangle2D.Double(14,3,7,7,1,1));
                g.draw(new RoundRectangle2D.Double(3,14,7,7,1,1));
                g.draw(new RoundRectangle2D.Double(14,14,7,7,1,1));
            }
            case PEOPLE -> {
                g.draw(new Ellipse2D.Double(6,3,7,7));
                g.draw(new Arc2D.Double(2,12,15,15,0,180,Arc2D.OPEN));
                g.draw(new Arc2D.Double(12,3,7,7,-70,140,Arc2D.OPEN));
                g.draw(new Arc2D.Double(13,12,9,13,-10,90,Arc2D.OPEN));
            }
            case CLIPBOARD -> {
                g.draw(new RoundRectangle2D.Double(5,4,14,18,2,2));
                g.setColor(UiConstants.SURFACE); g.fillRoundRect(9,2,6,5,2,2); g.setColor(color);
                g.drawRoundRect(9,2,6,5,2,2); g.drawLine(9,11,15,11); g.drawLine(9,15,15,15);
            }
            case CALENDAR -> {
                g.drawRoundRect(3,5,18,16,2,2); g.drawLine(3,10,21,10);
                g.drawLine(7,2,7,7); g.drawLine(17,2,17,7);
                for(int i=7;i<=17;i+=5) { g.drawLine(i,14,i,14); g.drawLine(i,17,i,17); }
            }
            case CHART -> {
                g.drawLine(4,21,4,14); g.drawLine(10,21,10,10); g.drawLine(16,21,16,7);
                Path2D path=new Path2D.Double(); path.moveTo(3,11); path.lineTo(10,5); path.lineTo(14,8); path.lineTo(21,2);
                g.draw(path); g.drawLine(16,2,21,2); g.drawLine(21,2,21,7);
            }
            case VOUCHER -> {
                g.drawRoundRect(3,8,18,13,2,2);
                Path2D path=new Path2D.Double(); path.moveTo(3,8); path.lineTo(12,3); path.lineTo(21,8); g.draw(path);
                g.drawLine(12,4,12,8); g.drawLine(8,12,16,12);
            }
            case SETTINGS -> {
                for(int i=0;i<8;i++) {
                    double a=Math.PI*i/4; g.draw(new Line2D.Double(12+8*Math.cos(a),12+8*Math.sin(a),12+10*Math.cos(a),12+10*Math.sin(a)));
                }
                g.draw(new Ellipse2D.Double(4,4,16,16)); g.draw(new Ellipse2D.Double(9,9,6,6));
            }
            case SEARCH -> { g.drawOval(4,3,12,12); g.drawLine(14,14,21,21); }
            case BELL -> {
                Path2D p=new Path2D.Double(); p.moveTo(5,17); p.lineTo(7,14); p.lineTo(7,9);
                p.curveTo(7,2,17,2,17,9); p.lineTo(17,14); p.lineTo(19,17); p.closePath(); g.draw(p);
                g.draw(new Arc2D.Double(10,18,4,4,180,180,Arc2D.OPEN)); g.drawLine(12,2,12,4);
            }
            case CHEVRON -> { g.drawLine(7,10,12,15); g.drawLine(12,15,17,10); }
            case CHECK -> { g.drawOval(2,2,20,20); g.drawLine(7,12,10,15); g.drawLine(10,15,17,8); }
            case INVOICE -> {
                g.drawRoundRect(5,2,14,20,1,1); g.drawLine(9,7,15,7); g.drawLine(9,17,15,17);
                g.draw(new Arc2D.Double(9,9,6,4,40,250,Arc2D.OPEN)); g.draw(new Arc2D.Double(9,12,6,4,220,250,Arc2D.OPEN)); g.drawLine(12,8,12,17);
            }
            case PLUS -> { g.drawLine(12,5,12,19); g.drawLine(5,12,19,12); }
            case HELP -> {
                g.drawOval(3,3,18,18); g.draw(new Arc2D.Double(9,7,6,6,0,180,Arc2D.OPEN));
                g.drawLine(15,10,12,13); g.drawLine(12,13,12,14); g.drawLine(12,17,12,17);
            }
            case ARROW -> { g.drawLine(4,12,20,12); g.drawLine(15,7,20,12); g.drawLine(15,17,20,12); }
            case LOGOUT -> { g.drawLine(12,4,5,4); g.drawLine(5,4,5,20); g.drawLine(5,20,12,20); g.drawLine(10,12,21,12); g.drawLine(17,8,21,12); g.drawLine(17,16,21,12); }
        }
        g.dispose();
    }
}
