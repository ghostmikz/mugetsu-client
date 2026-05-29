package com.mugetsu.agent.module.render;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventUpdate;

import java.lang.reflect.Method;

/**
 * Xray — forces chunk re-mesh while enabled so transparent rendering is applied.
 * The actual block opacity hook requires additional ASM work in a BlockStatePatch;
 * this module provides the on/off switch and forces re-mesh on toggle.
 * Keybind: X (GLFW 88)
 */
public class XrayModule extends Module {

    public static volatile boolean active = false;

    public XrayModule(ResolvedNames names) {
        super("Xray", "Render", 88, names);
    }

    @Override
    public void onEnable() {
        active = true;
        triggerRebuild();
    }

    @Override
    public void onDisable() {
        active = false;
        triggerRebuild();
    }

    private void triggerRebuild() {
        // Force a chunk re-render by calling LevelRenderer.allChanged() or markForRerender
        if (names.levelRendererField == null || mc() == null) return;
        try {
            Object lr = names.levelRendererField.get(mc());
            if (lr == null) return;
            for (String name : new String[]{"allChanged", "reload", "method_3664"}) {
                for (Method m : lr.getClass().getMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == 0) {
                        m.invoke(lr);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
