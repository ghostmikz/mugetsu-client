package com.mugetsu.agent.gui;

import com.mugetsu.agent.module.Module;

import java.util.List;

/** A draggable category panel in the ClickGUI — Void Purple theme. */
public class GuiPanel {

    private final String title;
    private final List<Module> modules;
    private double x, y;
    private boolean dragging = false;
    private double dragOffX, dragOffY;

    private static final double W      = MugetsuTheme.PANEL_W;
    private static final double HEADER = MugetsuTheme.PANEL_HEADER;
    private static final double ROW_H  = MugetsuTheme.ROW_H;

    public GuiPanel(String title, List<Module> modules, double x, double y) {
        this.title = title;
        this.modules = modules;
        this.x = x;
        this.y = y;
    }

    public void render(FontHelper font) {
        double totalH = HEADER + modules.size() * ROW_H;
        float[] bg = MugetsuTheme.BG_PANEL;
        float[] at = MugetsuTheme.ACCENT_TOP;
        float[] ac = MugetsuTheme.ACCENT;
        float[] hl = MugetsuTheme.HIGHLIGHT;
        float[] bd = MugetsuTheme.BORDER_H;

        // Body
        GlHelper.drawRect(x, y, W, totalH, bg[0], bg[1], bg[2], bg[3]);

        // Header gradient (darker top → accent bottom)
        GlHelper.drawGradientRectV(x, y, W, HEADER, at[0], at[1], at[2], at[3], ac[0], ac[1], ac[2], ac[3]);

        // Header text
        if (font != null) font.drawString(title, (float)(x + 4), (float)(y + 3), MugetsuTheme.TEXT_COLOR);

        // Module rows
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            double ry = y + HEADER + i * ROW_H;

            if (m.isEnabled()) {
                GlHelper.drawRect(x, ry, W, ROW_H, hl[0], hl[1], hl[2], hl[3]);
                if (font != null) font.drawString("■ " + m.getName(), (float)(x + 4), (float)(ry + 3), MugetsuTheme.TEXT_COLOR);
            } else {
                if (font != null) font.drawString("  " + m.getName(), (float)(x + 4), (float)(ry + 3), MugetsuTheme.LABEL_COLOR);
            }
        }

        // Panel border
        GlHelper.drawBorder(x, y, W, totalH, bd[0], bd[1], bd[2], bd[3]);
    }

    /** Returns true if a module was toggled. */
    public boolean onClick(double mx, double my) {
        for (int i = 0; i < modules.size(); i++) {
            double ry = y + HEADER + i * ROW_H;
            if (mx >= x && mx <= x + W && my >= ry && my <= ry + ROW_H) {
                modules.get(i).toggle();
                return true;
            }
        }
        return false;
    }

    public void startDrag(double mx, double my) {
        if (mx >= x && mx <= x + W && my >= y && my <= y + HEADER) {
            dragging = true;
            dragOffX = mx - x;
            dragOffY = my - y;
        }
    }

    public void drag(double mx, double my) {
        if (!dragging) return;
        x = mx - dragOffX;
        y = my - dragOffY;
    }

    public void stopDrag() { dragging = false; }

    public boolean contains(double mx, double my) {
        double totalH = HEADER + modules.size() * ROW_H;
        return mx >= x && mx <= x + W && my >= y && my <= y + totalH;
    }

    /** Returns the bottom Y edge of this panel. */
    public double bottomY() { return y + HEADER + modules.size() * ROW_H; }
    public double getX()    { return x; }
    public double getW()    { return W; }
}
