package com.mugetsu.agent.gui;

import com.mugetsu.agent.MugetsuClient;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.agent.module.ModuleManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-game module toggle overlay — Void Purple theme with UNINJECT button. */
public class ClickGUI {

    private final ResolvedNames names;
    private final List<GuiPanel> panels = new ArrayList<>();
    private FontHelper font;
    private boolean fontsInit = false;

    private static final int GLFW_MOUSE_BUTTON_LEFT = 0;
    private static final int GLFW_PRESS             = 1;

    // Uninject button state
    private static final double UNINJECT_W = 90;
    private static final double UNINJECT_H = 16;
    private double uninjectX, uninjectY;
    private boolean uninjectHovered = false;

    private boolean prevClick = false;

    public ClickGUI(ResolvedNames names) {
        this.names = names;
    }

    public void init() {
        buildPanels();
        GlHelper.init(names.gameLoader);
    }

    private void buildPanels() {
        panels.clear();
        Map<String, List<Module>> byCategory = new LinkedHashMap<>();
        for (Module m : ModuleManager.INSTANCE.getAll()) {
            byCategory.computeIfAbsent(m.getCategory(), k -> new ArrayList<>()).add(m);
        }
        double startX = 5;
        for (Map.Entry<String, List<Module>> entry : byCategory.entrySet()) {
            panels.add(new GuiPanel(entry.getKey(), entry.getValue(), startX, 5));
            startX += MugetsuTheme.COL_SPACING;
        }
    }

    public void render(int screenW, int screenH) {
        if (!fontsInit) { font = FontHelper.fromMC(names.mcInstance); fontsInit = true; }

        GlHelper.begin2D(screenW, screenH);

        double[] mouse = getMousePos();
        boolean clicking = isMouseDown();

        // Render panels + handle interaction
        for (GuiPanel panel : panels) {
            if (clicking && !prevClick) {
                panel.onClick(mouse[0], mouse[1]);
                panel.startDrag(mouse[0], mouse[1]);
            } else if (clicking) {
                panel.drag(mouse[0], mouse[1]);
            } else {
                panel.stopDrag();
            }
            panel.render(font);
        }

        // Position uninject button below the tallest panel
        double maxBottom = 5 + MugetsuTheme.PANEL_HEADER;
        for (GuiPanel p : panels) maxBottom = Math.max(maxBottom, p.bottomY());
        uninjectX = 5;
        uninjectY = maxBottom + 6;

        // Uninject button hover check
        uninjectHovered = mouse[0] >= uninjectX && mouse[0] <= uninjectX + UNINJECT_W
                       && mouse[1] >= uninjectY && mouse[1] <= uninjectY + UNINJECT_H;

        float[] btnColor = uninjectHovered ? MugetsuTheme.UNINJECT_H : MugetsuTheme.UNINJECT_N;
        GlHelper.drawRect(uninjectX, uninjectY, UNINJECT_W, UNINJECT_H,
            btnColor[0], btnColor[1], btnColor[2], btnColor[3]);
        float[] bd = MugetsuTheme.BORDER_H;
        GlHelper.drawBorder(uninjectX, uninjectY, UNINJECT_W, UNINJECT_H, bd[0], bd[1], bd[2], bd[3]);
        if (font != null)
            font.drawString("UNINJECT", (float)(uninjectX + 20), (float)(uninjectY + 4), MugetsuTheme.TEXT_COLOR);

        // Fire uninject on click
        if (clicking && !prevClick && uninjectHovered) {
            MugetsuClient.shutdown();
        }

        prevClick = clicking;
        GlHelper.end2D();
    }

    private double[] getMousePos() {
        if (names.glfwGetCursorPos == null || names.windowHandle == 0) return new double[]{0, 0};
        try {
            java.nio.DoubleBuffer xBuf = java.nio.ByteBuffer.allocateDirect(8).asDoubleBuffer();
            java.nio.DoubleBuffer yBuf = java.nio.ByteBuffer.allocateDirect(8).asDoubleBuffer();
            names.glfwGetCursorPos.invoke(null, names.windowHandle, xBuf, yBuf);
            return new double[]{ xBuf.get(0), yBuf.get(0) };
        } catch (Throwable ignored) {}
        return new double[]{0, 0};
    }

    private boolean isMouseDown() {
        if (names.glfwGetMouseButton == null || names.windowHandle == 0) return false;
        try {
            return (int) names.glfwGetMouseButton.invoke(null, names.windowHandle, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        } catch (Throwable ignored) { return false; }
    }
}
