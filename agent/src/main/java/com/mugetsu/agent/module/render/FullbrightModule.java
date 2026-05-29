package com.mugetsu.agent.module.render;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventUpdate;

import java.lang.reflect.Field;

/**
 * Fullbright — sets gamma to 100.0 every tick while enabled, restores on disable.
 * Requires no ASM — pure reflection on Options.gamma.
 * Keybind: G (GLFW 71)
 */
public class FullbrightModule extends Module {

    private double originalGamma = 1.0;
    private boolean gammaResolved = false;
    private Field gammaField;

    public FullbrightModule(ResolvedNames names) {
        super("Fullbright", "Render", 71, names);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (!isEnabled()) return;
        setGamma(100.0);
    }

    @Override
    public void onEnable() {
        Object opts = getOptions();
        if (opts == null) return;
        Field gf = resolveGammaField(opts);
        if (gf == null) return;
        try {
            Object val = gf.get(opts);
            // gamma may be a DoubleOption or just a double; handle both
            if (val instanceof Double) originalGamma = (Double) val;
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        setGamma(originalGamma);
    }

    private void setGamma(double value) {
        Object opts = getOptions();
        if (opts == null) return;
        Field gf = resolveGammaField(opts);
        if (gf == null) return;
        try {
            Object current = gf.get(opts);
            if (current instanceof Double) {
                gf.set(opts, value);
            } else if (current != null) {
                // Try setting via a setValue method (Fabric SimpleOption wrapper)
                for (var m : current.getClass().getMethods()) {
                    if (m.getName().contains("setValue") && m.getParameterCount() == 1) {
                        try { m.invoke(current, value); break; } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private Field resolveGammaField(Object opts) {
        if (gammaResolved) return gammaField;
        gammaResolved = true;
        // Try from ResolvedNames first
        if (names.gammaField != null) { gammaField = names.gammaField; return gammaField; }
        // Scan fields by name
        for (String name : new String[]{"gamma", "field_74334_X", "f_92080_"}) {
            try {
                Field f = opts.getClass().getDeclaredField(name);
                f.setAccessible(true);
                gammaField = f;
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }
}
