package com.mugetsu.agent.gui;

import java.lang.reflect.Method;

/**
 * Thin reflective wrapper around LWJGL3 GL11/RenderSystem calls.
 * All method handles are resolved lazily using the game classloader stored in
 * ResolvedNames, so agent.jar does not need LWJGL on its compile classpath.
 */
public final class GlHelper {

    // GL11 constants (same values in LWJGL 2 and 3)
    public static final int GL_LINES  = 1;
    public static final int GL_QUADS  = 7;
    public static final int GL_DEPTH_TEST   = 0x0B71;
    public static final int GL_TEXTURE_2D   = 0x0DE1;
    public static final int GL_BLEND        = 0x0BE2;
    public static final int GL_PROJECTION   = 0x1701;
    public static final int GL_MODELVIEW    = 0x1700;
    public static final int GL_SRC_ALPHA    = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;

    private static Method glBegin, glEnd, glVertex3d, glColor4f, glLineWidth;
    private static Method glEnable, glDisable;
    private static Method glPushMatrix, glPopMatrix, glLoadIdentity, glMatrixMode, glOrtho;
    private static Method glBlendFunc;
    private static boolean resolved = false;

    public static void init(ClassLoader loader) {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> gl11 = Class.forName("org.lwjgl.opengl.GL11", true, loader);
            glBegin      = get(gl11, "glBegin",    int.class);
            glEnd        = get(gl11, "glEnd");
            glVertex3d   = get(gl11, "glVertex3d", double.class, double.class, double.class);
            glColor4f    = get(gl11, "glColor4f",  float.class, float.class, float.class, float.class);
            glLineWidth  = get(gl11, "glLineWidth", float.class);
            glEnable     = get(gl11, "glEnable",   int.class);
            glDisable    = get(gl11, "glDisable",  int.class);
            glPushMatrix = get(gl11, "glPushMatrix");
            glPopMatrix  = get(gl11, "glPopMatrix");
            glLoadIdentity = get(gl11, "glLoadIdentity");
            glMatrixMode = get(gl11, "glMatrixMode", int.class);
            glOrtho      = get(gl11, "glOrtho",
                               double.class, double.class, double.class,
                               double.class, double.class, double.class);
            glBlendFunc  = get(gl11, "glBlendFunc", int.class, int.class);
        } catch (Throwable t) {
            System.err.println("[GlHelper] GL11 not available: " + t.getMessage());
        }
    }

    private static Method get(Class<?> cls, String name, Class<?>... params) {
        try { return cls.getMethod(name, params); } catch (Throwable ignored) { return null; }
    }

    // --- Public drawing API ---

    public static void beginESP() {
        invoke(glDisable, GL_DEPTH_TEST);
        invoke(glDisable, GL_TEXTURE_2D);
        invoke(glEnable,  GL_BLEND);
        invoke(glBlendFunc, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        invoke(glLineWidth, 1.5f);
    }

    public static void endESP() {
        invoke(glEnable, GL_DEPTH_TEST);
    }

    public static void color(float r, float g, float b, float a) {
        invoke(glColor4f, r, g, b, a);
    }

    public static void drawBox(double x1, double y1, double z1,
                               double x2, double y2, double z2) {
        invoke(glBegin, GL_LINES);
        // Bottom face
        line(x1, y1, z1,  x2, y1, z1);
        line(x2, y1, z1,  x2, y1, z2);
        line(x2, y1, z2,  x1, y1, z2);
        line(x1, y1, z2,  x1, y1, z1);
        // Top face
        line(x1, y2, z1,  x2, y2, z1);
        line(x2, y2, z1,  x2, y2, z2);
        line(x2, y2, z2,  x1, y2, z2);
        line(x1, y2, z2,  x1, y2, z1);
        // Verticals
        line(x1, y1, z1,  x1, y2, z1);
        line(x2, y1, z1,  x2, y2, z1);
        line(x2, y1, z2,  x2, y2, z2);
        line(x1, y1, z2,  x1, y2, z2);
        invoke(glEnd);
    }

    private static void line(double x1, double y1, double z1,
                             double x2, double y2, double z2) {
        invoke(glVertex3d, x1, y1, z1);
        invoke(glVertex3d, x2, y2, z2);
    }

    // --- 2D overlay helpers ---

    public static void begin2D(int w, int h) {
        invoke(glPushMatrix);
        invoke(glMatrixMode, GL_PROJECTION);
        invoke(glPushMatrix);
        invoke(glLoadIdentity);
        invoke(glOrtho, 0.0, (double) w, (double) h, 0.0, -1.0, 1.0);
        invoke(glMatrixMode, GL_MODELVIEW);
        invoke(glPushMatrix);
        invoke(glLoadIdentity);
        invoke(glDisable, GL_DEPTH_TEST);
        invoke(glDisable, GL_TEXTURE_2D);
        invoke(glEnable,  GL_BLEND);
        invoke(glBlendFunc, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void end2D() {
        invoke(glEnable, GL_DEPTH_TEST);
        invoke(glMatrixMode, GL_PROJECTION);
        invoke(glPopMatrix);
        invoke(glMatrixMode, GL_MODELVIEW);
        invoke(glPopMatrix);
        invoke(glPopMatrix);
    }

    public static void drawRect(double x, double y, double w, double h,
                                float r, float g, float b, float a) {
        invoke(glBegin, GL_QUADS);
        invoke(glColor4f, r, g, b, a);
        // Draw quad
        try {
            if (glVertex3d != null) {
                glVertex3d.invoke(null, x,     y,     0.0);
                glVertex3d.invoke(null, x,     y + h, 0.0);
                glVertex3d.invoke(null, x + w, y + h, 0.0);
                glVertex3d.invoke(null, x + w, y,     0.0);
            }
        } catch (Throwable ignored) {}
        invoke(glEnd);
    }

    /** Draws a 1px hollow rectangle outline. */
    public static void drawBorder(double x, double y, double w, double h,
                                  float r, float g, float b, float a) {
        invoke(glBegin, GL_LINES);
        invoke(glColor4f, r, g, b, a);
        try {
            if (glVertex3d != null) {
                // top
                glVertex3d.invoke(null, x,     y,     0.0);
                glVertex3d.invoke(null, x + w, y,     0.0);
                // bottom
                glVertex3d.invoke(null, x,     y + h, 0.0);
                glVertex3d.invoke(null, x + w, y + h, 0.0);
                // left
                glVertex3d.invoke(null, x,     y,     0.0);
                glVertex3d.invoke(null, x,     y + h, 0.0);
                // right
                glVertex3d.invoke(null, x + w, y,     0.0);
                glVertex3d.invoke(null, x + w, y + h, 0.0);
            }
        } catch (Throwable ignored) {}
        invoke(glEnd);
    }

    /**
     * Draws a vertical-gradient filled quad.
     * Top row of vertices uses color1, bottom row uses color2.
     */
    public static void drawGradientRectV(double x, double y, double w, double h,
                                         float r1, float g1, float b1, float a1,
                                         float r2, float g2, float b2, float a2) {
        invoke(glBegin, GL_QUADS);
        try {
            if (glVertex3d != null && glColor4f != null) {
                glColor4f.invoke(null, r1, g1, b1, a1);
                glVertex3d.invoke(null, x,     y,     0.0);
                glVertex3d.invoke(null, x + w, y,     0.0);
                glColor4f.invoke(null, r2, g2, b2, a2);
                glVertex3d.invoke(null, x + w, y + h, 0.0);
                glVertex3d.invoke(null, x,     y + h, 0.0);
            }
        } catch (Throwable ignored) {}
        invoke(glEnd);
    }

    private static void invoke(Method m, Object... args) {
        if (m == null) return;
        try { m.invoke(null, args); } catch (Throwable ignored) {}
    }

    private GlHelper() {}
}
