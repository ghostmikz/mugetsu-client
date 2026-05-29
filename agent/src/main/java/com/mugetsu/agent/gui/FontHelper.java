package com.mugetsu.agent.gui;

import java.lang.reflect.Method;

/**
 * Wraps the game's font renderer via reflection for drawing text in the GUI overlay.
 * Falls back to printing nothing if the font renderer isn't available.
 */
public class FontHelper {

    private final Object fontRenderer;
    private Method drawMethod;
    private boolean resolved = false;

    public FontHelper(Object fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    public void drawString(String text, float x, float y, int color) {
        if (fontRenderer == null) return;
        resolveDrawMethod();
        if (drawMethod == null) return;
        try {
            // Signature varies: drawString(String, float, float, int) or draw(PoseStack, String, ...)
            // Try the simple 4-arg form first
            drawMethod.invoke(fontRenderer, text, x, y, color);
        } catch (Throwable ignored) {}
    }

    private void resolveDrawMethod() {
        if (resolved) return;
        resolved = true;
        for (Method m : fontRenderer.getClass().getMethods()) {
            if (m.getParameterCount() == 4) {
                Class<?>[] p = m.getParameterTypes();
                if (p[0] == String.class && p[3] == int.class) {
                    m.setAccessible(true);
                    drawMethod = m;
                    return;
                }
            }
        }
    }

    /** Tries to obtain the font renderer from the MC instance. */
    public static FontHelper fromMC(Object mc) {
        if (mc == null) return new FontHelper(null);
        for (String name : new String[]{"font", "textRenderer", "fontRenderer",
                "field_1772", "f_91013_"}) {
            try {
                var f = mc.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return new FontHelper(f.get(mc));
            } catch (Throwable ignored) {}
        }
        return new FontHelper(null);
    }
}
