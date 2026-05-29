package com.mugetsu.agent.module.movement;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventUpdate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Fly — sets vertical motion to 0 every tick so gravity is negated.
 * Space/Shift ascend/descend via direct velocity manipulation.
 * Keybind: F (GLFW 70)
 */
public class FlyModule extends Module {

    private static final int GLFW_KEY_F     = 70;
    private static final int GLFW_KEY_SPACE = 32;
    private static final int GLFW_KEY_LSHIFT= 340;
    private static final int GLFW_PRESS     = 1;

    // Cached velocity reflection handles
    private Method getVelocity, setVelocity;
    private boolean velResolved = false;

    public FlyModule(ResolvedNames names) {
        super("Fly", "Movement", GLFW_KEY_F, names);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (!isEnabled()) return;
        Object player = event.getPlayer();
        if (player == null) return;

        resolveVelocity(player);
        if (getVelocity == null || setVelocity == null) return;

        try {
            Object vel = getVelocity.invoke(player);
            double x = getComp(vel, "x", 0);
            double z = getComp(vel, "z", 2);
            double y = 0.0;

            if (isDown(GLFW_KEY_SPACE))  y =  0.2;
            if (isDown(GLFW_KEY_LSHIFT)) y = -0.2;

            Object newVel = makeVec(vel.getClass(), x * 0.7, y, z * 0.7);
            if (newVel != null) setVelocity.invoke(player, newVel);
        } catch (Throwable ignored) {}
    }

    private void resolveVelocity(Object player) {
        if (velResolved) return;
        velResolved = true;
        for (Method m : player.getClass().getMethods()) {
            String n = m.getName();
            if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()
                    && m.getReturnType().getName().startsWith("net.minecraft")) {
                if (n.equals("getVelocity") || n.equals("getDeltaMovement") || n.equals("method_18798")) {
                    getVelocity = m;
                }
            }
            if (m.getParameterCount() == 1 && !m.getParameterTypes()[0].isPrimitive()) {
                if (n.equals("setVelocity") || n.equals("setDeltaMovement") || n.equals("method_18799")) {
                    setVelocity = m;
                }
            }
        }
    }

    private double getComp(Object vec, String field, int idx) {
        try { return (double) vec.getClass().getField(field).get(vec); } catch (Throwable ignored) {}
        int i = 0;
        for (Field f : vec.getClass().getDeclaredFields()) {
            if (f.getType() == double.class) {
                if (i == idx) {
                    try { f.setAccessible(true); return (double) f.get(vec); } catch (Throwable ignored2) {}
                }
                i++;
            }
        }
        return 0;
    }

    private Object makeVec(Class<?> cls, double x, double y, double z) {
        try { return cls.getConstructor(double.class, double.class, double.class).newInstance(x, y, z); }
        catch (Throwable ignored) { return null; }
    }

    private boolean isDown(int key) {
        if (names.glfwGetKey == null || names.windowHandle == 0) return false;
        try { return (int) names.glfwGetKey.invoke(null, names.windowHandle, key) == GLFW_PRESS; }
        catch (Throwable ignored) { return false; }
    }
}
