package com.mugetsu.agent.module.combat;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventUpdate;

import java.lang.reflect.Method;

/**
 * TriggerBot — attacks automatically when an entity is under the crosshair.
 * Checks crosshairPickEntity every tick and fires attack if present.
 * Keybind: T (GLFW 84)
 */
public class TriggerBotModule extends Module {

    private long lastTrigger = 0;
    private static final long TRIGGER_DELAY_MS = 80;
    private Method attackMethod;
    private boolean methodResolved = false;

    public TriggerBotModule(ResolvedNames names) {
        super("TriggerBot", "Combat", 84, names);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastTrigger < TRIGGER_DELAY_MS) return;

        Object mc = mc();
        if (mc == null) return;

        Object target = getCrosshairEntity(mc);
        if (target == null) return;

        // Only attack living entities
        if (!isLivingEntity(target)) return;

        Object gameMode = getGameMode();
        if (gameMode == null) return;

        resolveAttack(gameMode);
        if (attackMethod == null) return;

        try {
            attackMethod.invoke(gameMode, target);
            lastTrigger = now;
        } catch (Throwable ignored) {}
    }

    private Object getCrosshairEntity(Object mc) {
        for (String name : new String[]{"crosshairPickEntity", "targetedEntity",
                "pointedEntity", "field_1643", "f_91097_"}) {
            try {
                var f = mc.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(mc);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private boolean isLivingEntity(Object entity) {
        if (names.livingEntityClass == null) return entity != null;
        return names.livingEntityClass.isInstance(entity);
    }

    private void resolveAttack(Object gameMode) {
        if (methodResolved) return;
        methodResolved = true;
        if (names.attackMethod != null) { attackMethod = names.attackMethod; return; }
        for (String n : new String[]{"attack", "attackEntity", "method_2879"}) {
            for (Method m : gameMode.getClass().getMethods()) {
                if (m.getName().equals(n) && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    attackMethod = m;
                    return;
                }
            }
        }
    }
}
