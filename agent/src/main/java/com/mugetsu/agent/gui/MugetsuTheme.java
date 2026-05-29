package com.mugetsu.agent.gui;

/** Void Purple color palette — all in-game GUI components read constants from here. */
public final class MugetsuTheme {

    // --- GL float colors (r, g, b, alpha) for GlHelper.drawRect / color ---
    public static final float[] BG_DARK    = {0.039f, 0.039f, 0.078f, 0.88f};
    public static final float[] BG_PANEL   = {0.078f, 0.078f, 0.118f, 0.82f};
    public static final float[] ACCENT     = {0.467f, 0.133f, 0.800f, 0.95f};
    public static final float[] ACCENT_TOP = {0.333f, 0.067f, 0.600f, 1.00f}; // darker top for gradient
    public static final float[] HIGHLIGHT  = {0.600f, 0.200f, 1.000f, 0.50f}; // enabled module row bg
    public static final float[] BORDER_H   = {0.333f, 0.067f, 0.600f, 1.00f}; // panel outline
    public static final float[] UNINJECT_N = {0.467f, 0.133f, 0.800f, 0.90f}; // normal uninject btn
    public static final float[] UNINJECT_H = {0.600f, 0.200f, 1.000f, 0.95f}; // hovered uninject btn

    // --- FontHelper int colors (ARGB) ---
    public static final int TEXT_COLOR  = 0xFFE0E0FF;
    public static final int LABEL_COLOR = 0xFFBBBBEE;
    public static final int DIM_COLOR   = 0xFF8888BB;

    // --- Layout ---
    public static final double PANEL_W      = 128;
    public static final double PANEL_HEADER = 14;
    public static final double ROW_H        = 14;
    public static final double COL_SPACING  = 138;

    private MugetsuTheme() {}
}
