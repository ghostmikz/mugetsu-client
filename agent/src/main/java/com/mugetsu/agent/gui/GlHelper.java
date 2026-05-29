package com.mugetsu.agent.gui;

import java.lang.reflect.Method;

/**
 * GL rendering helper. Legacy GL11 (glBegin/glEnd/glPushMatrix) crashes on
 * modern OpenGL core profile contexts (Wayland/Mesa). All draw calls are
 * stubbed out until a proper RenderSystem/GuiGraphics renderer is implemented.
 * Constants are kept so patches still compile.
 */
public final class GlHelper {

    public static final int GL_LINES          = 1;
    public static final int GL_QUADS          = 7;
    public static final int GL_DEPTH_TEST     = 0x0B71;
    public static final int GL_TEXTURE_2D     = 0x0DE1;
    public static final int GL_BLEND          = 0x0BE2;
    public static final int GL_PROJECTION     = 0x1701;
    public static final int GL_MODELVIEW      = 0x1700;
    public static final int GL_SRC_ALPHA      = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;

    // All rendering is disabled — stubs prevent crashes on GL core profile
    public static void init(ClassLoader loader) { /* no-op */ }

    public static void beginESP()  { /* no-op */ }
    public static void endESP()    { /* no-op */ }

    public static void color(float r, float g, float b, float a) { /* no-op */ }

    public static void drawBox(double x1, double y1, double z1,
                               double x2, double y2, double z2) { /* no-op */ }

    public static void begin2D(int w, int h) { /* no-op */ }
    public static void end2D()              { /* no-op */ }

    public static void drawRect(double x, double y, double w, double h,
                                float r, float g, float b, float a) { /* no-op */ }

    public static void drawBorder(double x, double y, double w, double h,
                                  float r, float g, float b, float a) { /* no-op */ }

    public static void drawGradientRectV(double x, double y, double w, double h,
                                         float r1, float g1, float b1, float a1,
                                         float r2, float g2, float b2, float a2) { /* no-op */ }

    private GlHelper() {}
}
