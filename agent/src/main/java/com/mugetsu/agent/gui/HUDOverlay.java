package com.mugetsu.agent.gui;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.agent.module.ModuleManager;

import java.util.List;

/** Draws active module names in the top-right corner with a Void Purple background. */
public class HUDOverlay {

    private final ResolvedNames names;
    private FontHelper font;
    private boolean fontInit = false;

    private static final float LINE_H   = 9f;
    private static final float PAD      = 2f;
    private static final float APPROX_W = 80f;

    public HUDOverlay(ResolvedNames names) {
        this.names = names;
    }

    public void render(int screenW, int screenH) {
        if (!fontInit) { font = FontHelper.fromMC(names.mcInstance); fontInit = true; }

        List<Module> enabled = ModuleManager.INSTANCE.getEnabled();
        if (enabled.isEmpty()) return;

        float totalH = enabled.size() * LINE_H + PAD * 2;
        float bgX    = screenW - APPROX_W - PAD * 2;
        float bgY    = PAD;

        GlHelper.begin2D(screenW, screenH);

        // Background rect
        float[] bg = MugetsuTheme.BG_DARK;
        GlHelper.drawRect(bgX, bgY, APPROX_W + PAD * 2, totalH, bg[0], bg[1], bg[2], bg[3]);

        // Border
        float[] bd = MugetsuTheme.BORDER_H;
        GlHelper.drawBorder(bgX, bgY, APPROX_W + PAD * 2, totalH, bd[0], bd[1], bd[2], bd[3]);

        // Module names
        float ty = bgY + PAD;
        for (Module m : enabled) {
            String label = "▌ " + m.getName(); // ▌ prefix
            if (font != null) font.drawString(label, bgX + PAD, ty, MugetsuTheme.LABEL_COLOR);
            ty += LINE_H;
        }

        GlHelper.end2D();
    }
}
