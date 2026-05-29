package com.mugetsu.agent.module.combat;

import com.mugetsu.agent.hook.CallbackRegistry;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventEntityAABB;
import com.mugetsu.common.event.EventHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * HitboxExpander — inflates all entity bounding boxes, making them easier to hit.
 * Hooks into Entity.getBoundingBox() via ASM + EventEntityAABB.
 * Keybind: none (ClickGUI)
 */
public class HitboxExpanderModule extends Module {

    public double expand = 0.3;
    private Method inflateMethod;
    private boolean resolved = false;

    public HitboxExpanderModule(ResolvedNames names) {
        super("HitboxExpander", "Combat", 0, names);
    }

    @Override public void onEnable()  { CallbackRegistry.hitboxExpandEnabled = true;  syncExpand(); }
    @Override public void onDisable() { CallbackRegistry.hitboxExpandEnabled = false; }

    @EventHandler
    public void onEntityAABB(EventEntityAABB event) {
        if (!isEnabled()) return;
        // Don't expand local player
        if (names.playerField != null && mc() != null) {
            try {
                Object lp = names.playerField.get(mc());
                if (event.entity == lp) return;
            } catch (Throwable ignored) {}
        }
        Object inflated = inflate(event.box, expand);
        if (inflated != null) event.box = inflated;
    }

    private Object inflate(Object box, double amount) {
        if (box == null) return null;
        resolveInflate(box);
        if (inflateMethod != null) {
            try { return inflateMethod.invoke(box, amount, amount, amount); }
            catch (Throwable ignored) {}
            try { return inflateMethod.invoke(box, amount); }
            catch (Throwable ignored) {}
        }
        return null;
    }

    private void resolveInflate(Object box) {
        if (resolved) return;
        resolved = true;
        for (String name : new String[]{"inflate", "expand", "grow", "method_1007"}) {
            for (Method m : box.getClass().getMethods()) {
                if (m.getName().equals(name) && (m.getParameterCount() == 1 || m.getParameterCount() == 3)) {
                    m.setAccessible(true);
                    inflateMethod = m;
                    return;
                }
            }
        }
    }

    private void syncExpand() { CallbackRegistry.hitboxExpand = expand; }
    public void setExpand(double e) { this.expand = e; CallbackRegistry.hitboxExpand = e; }
}
