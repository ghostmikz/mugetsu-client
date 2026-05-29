package com.mugetsu.agent.module.combat;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventUpdate;

import java.lang.reflect.Method;

/**
 * AutoClicker — automatically attacks targeted entities at a configurable CPS.
 * Keybind: none (ClickGUI)
 */
public class AutoClickerModule extends Module {

    public int cps = 12;
    private long lastClick = 0;
    private Method attackMethod;
    private boolean methodResolved = false;

    public AutoClickerModule(ResolvedNames names) {
        super("AutoClicker", "Combat", 0, names);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        long interval = 1000L / cps;
        if (now - lastClick < interval) return;

        Object target = getTarget();
        if (target == null) return;

        Object gameMode = getGameMode();
        if (gameMode == null) return;

        resolveAttack(gameMode);
        if (attackMethod == null) return;

        try {
            attackMethod.invoke(gameMode, target);
            lastClick = now;
        } catch (Throwable ignored) {}
    }

    private Object getTarget() {
        Object mc = mc();
        if (mc == null) return null;
        // crosshairPickEntity or hitResult that targets an entity
        for (String name : new String[]{"crosshairPickEntity", "targetedEntity", "pointedEntity",
                "field_1643", "f_91097_"}) {
            try {
                var f = mc.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(mc);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private void resolveAttack(Object gameMode) {
        if (methodResolved) return;
        methodResolved = true;
        if (names.attackMethod != null) { attackMethod = names.attackMethod; return; }
        for (String name : new String[]{"attack", "attackEntity", "method_2879"}) {
            for (Method m : gameMode.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    attackMethod = m;
                    return;
                }
            }
        }
    }
}
