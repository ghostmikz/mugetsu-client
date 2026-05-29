package com.mugetsu.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinecraftHook {

    private static final UUID   SPEED_UUID       = UUID.fromString("2d13a678-5c4d-4b9e-a264-cac0cac0cac0");
    private static final double SPEED_MULTIPLIER = 0.5;
    private static final int    GLFW_KEY_R       = 82;
    private static final int    GLFW_KEY_Z       = 90;
    private static final int    GLFW_PRESS       = 1;
    // Extra Y velocity for 2-block higher jump (~0.42 base + 0.6 boost)
    private static final double JUMP_BOOST       = 0.6;

    private final Instrumentation inst;
    private Object mcInstance;
    private Field  playerField;
    private long   windowHandle = 0;
    private Method glfwGetKey   = null;

    public MinecraftHook(Instrumentation inst) {
        this.inst = inst;
    }

    public void start() {
        if (!resolveMinecraft()) {
            System.err.println("[Mugetsu] Could not find Minecraft class.");
            return;
        }
        resolveGLFW();
        if (windowHandle == 0 || glfwGetKey == null) {
            System.err.println("[Mugetsu] Could not get GLFW keyboard. Handle=" + windowHandle + " glfwGetKey=" + glfwGetKey);
            return;
        }
        System.out.println("[Mugetsu] Ready. Press R in-game to toggle 2x speed.");
        loop();
    }

    // -------------------------------------------------------------------------
    // Minecraft resolution
    // -------------------------------------------------------------------------

    private boolean resolveMinecraft() {
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            if (!isMinecraftClass(cls.getName())) continue;
            try {
                for (Field f : cls.getDeclaredFields()) {
                    if (f.getType() != cls) continue;
                    f.setAccessible(true);
                    Object instance = f.get(null);
                    if (instance == null) continue;
                    Field pf = findPlayerField(cls);
                    if (pf == null) continue;
                    mcInstance  = instance;
                    playerField = pf;
                    System.out.println("[Mugetsu] Found MC class: " + cls.getName());
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private boolean isMinecraftClass(String name) {
        return name.equals("net.minecraft.client.MinecraftClient")
            || name.equals("net.minecraft.client.Minecraft")
            || name.equals("net.minecraft.class_310");
    }

    private Field findPlayerField(Class<?> cls) {
        for (String name : new String[]{"player", "field_1724", "f_91074_"}) {
            try { Field f = cls.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
        }
        for (Field f : cls.getDeclaredFields()) {
            String t = f.getType().getSimpleName().toLowerCase();
            String n = f.getName().toLowerCase();
            if (t.contains("localplayer") || t.contains("clientplayer") || n.equals("player")) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // GLFW resolution — find glfwGetKey, then brute-force the window handle
    // by scanning MC instance fields for a long value that looks like a pointer
    // -------------------------------------------------------------------------

    private void resolveGLFW() {
        try {
            // Find GLFW class and glfwGetKey
            for (Class<?> cls : inst.getAllLoadedClasses()) {
                if (!cls.getName().equals("org.lwjgl.glfw.GLFW")) continue;
                for (Method m : cls.getMethods()) {
                    if (m.getName().equals("glfwGetKey") && m.getParameterCount() == 2) {
                        glfwGetKey = m;
                        break;
                    }
                }
                System.out.println("[Mugetsu] GLFW found, glfwGetKey=" + (glfwGetKey != null));
                break;
            }

            if (glfwGetKey == null) {
                System.err.println("[Mugetsu] GLFW class not found.");
                return;
            }

            // Find the Window object by calling getWindow() or similar on MC instance,
            // then call getHandle() / method_4490() on the result.
            // Only invoke methods — never access raw fields on live game objects.
            windowHandle = findHandleViaMethods();
            System.out.println("[Mugetsu] Window handle: " + windowHandle);

        } catch (Throwable e) {
            System.err.println("[Mugetsu] resolveGLFW error: " + e);
        }
    }

    private long findHandleViaMethods() {
        // Step 1: get the Window object from MC instance via known getter names
        Object windowObj = null;
        for (String name : new String[]{"getWindow", "method_22683", "getMainWindow"}) {
            try {
                Method m = mcInstance.getClass().getMethod(name);
                Object r = m.invoke(mcInstance);
                if (r != null) { windowObj = r; break; }
            } catch (Throwable ignored) {}
        }

        // Step 2: if no getter worked, scan declared fields of MC class only
        // (safe — we only look at the field type, not the value of nested objects)
        if (windowObj == null) {
            for (Field f : mcInstance.getClass().getDeclaredFields()) {
                if (f.getType().isPrimitive() || f.getType().isArray()) continue;
                String typeName = f.getType().getName();
                // Only consider net.minecraft types as the window candidate
                if (!typeName.startsWith("net.minecraft")) continue;
                try {
                    f.setAccessible(true);
                    Object val = f.get(mcInstance);
                    if (val == null) continue;
                    // Check if this object has a getHandle-like method returning long
                    for (Method m : val.getClass().getMethods()) {
                        if (m.getReturnType() == long.class && m.getParameterCount() == 0) {
                            long handle = (long) m.invoke(val);
                            if (handle > 0xFFFFL) {
                                System.out.println("[Mugetsu] Found handle via "
                                    + f.getName() + "." + m.getName() + "() = " + handle);
                                return handle;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (windowObj == null) return 0;

        // Step 3: call getHandle / method_4490 on the window object
        for (String name : new String[]{"getHandle", "method_4490", "getWindow"}) {
            try {
                Method m = windowObj.getClass().getMethod(name);
                if (m.getReturnType() == long.class) {
                    long handle = (long) m.invoke(windowObj);
                    if (handle != 0) return handle;
                }
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Main loop — toggle speed on R press (leading edge)
    // -------------------------------------------------------------------------

    private void loop() {
        boolean speedActive = false;
        boolean rWasDown    = false;
        boolean wasOnGround = true;

        while (true) {
            try {
                Thread.sleep(50);

                Object player = playerField.get(mcInstance);

                // R — toggle speed
                boolean rIsDown = isKeyDown(GLFW_KEY_R);
                if (rIsDown && !rWasDown) {
                    speedActive = !speedActive;
                    if (player != null) {
                        applySpeed(player, speedActive);
                        System.out.println("[Mugetsu] Speed " + (speedActive ? "ON" : "OFF"));
                    }
                }
                rWasDown = rIsDown;

                // Z — jump boost on takeoff
                if (player != null) {
                    boolean onGround = isOnGround(player);
                    if (isKeyDown(GLFW_KEY_Z) && wasOnGround && !onGround) {
                        boostJump(player);
                        System.out.println("[Mugetsu] Jump boost!");
                    }
                    wasOnGround = onGround;
                }

                if (speedActive && player == null) speedActive = false;

            } catch (InterruptedException e) {
                break;
            } catch (Throwable ignored) {}
        }
    }

    private boolean isKeyDown(int key) {
        try {
            return (int) glfwGetKey.invoke(null, windowHandle, key) == GLFW_PRESS;
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isOnGround(Object player) {
        for (Method m : player.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || m.getReturnType() != boolean.class) continue;
            String n = m.getName();
            if (n.equals("isOnGround") || n.equals("method_24828")) {
                try { return (boolean) m.invoke(player); } catch (Throwable ignored) {}
            }
        }
        return true;
    }

    private void boostJump(Object player) {
        try {
            Object vel = null;
            Method setVel = null;
            for (Method m : player.getClass().getMethods()) {
                String n = m.getName();
                if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()
                        && m.getReturnType().getName().startsWith("net.minecraft")) {
                    if (n.equals("getVelocity") || n.equals("getDeltaMovement") || n.equals("method_18798"))
                        try { vel = m.invoke(player); } catch (Throwable ignored) {}
                }
                if (m.getParameterCount() == 1 && !m.getParameterTypes()[0].isPrimitive()) {
                    if (n.equals("setVelocity") || n.equals("setDeltaMovement") || n.equals("method_18799"))
                        setVel = m;
                }
            }
            if (vel == null || setVel == null) return;

            double x = getVecComponent(vel, "x", 0);
            double y = getVecComponent(vel, "y", 1);
            double z = getVecComponent(vel, "z", 2);

            Object newVel = createVec3d(vel.getClass(), x, y + JUMP_BOOST, z);
            if (newVel != null) setVel.invoke(player, newVel);
        } catch (Throwable e) {
            System.err.println("[Mugetsu] boostJump error: " + e);
        }
    }

    private double getVecComponent(Object vec, String name, int index) {
        try { Field f = vec.getClass().getField(name); return (double) f.get(vec); }
        catch (Throwable ignored) {}
        int i = 0;
        for (Field f : vec.getClass().getDeclaredFields()) {
            if (f.getType() == double.class) {
                if (i == index) {
                    try { f.setAccessible(true); return (double) f.get(vec); } catch (Throwable ignored) {}
                }
                i++;
            }
        }
        return 0;
    }

    private Object createVec3d(Class<?> vecClass, double x, double y, double z) {
        try { return vecClass.getConstructor(double.class, double.class, double.class).newInstance(x, y, z); }
        catch (Throwable ignored) {}
        return null;
    }

    // -------------------------------------------------------------------------
    // Speed attribute
    // -------------------------------------------------------------------------

    private void applySpeed(Object player, boolean enable) {
        try {
            Object attrInstance = getSpeedAttrInstance(player);
            if (attrInstance == null) {
                System.err.println("[Mugetsu] Could not find speed attribute.");
                return;
            }
            if (enable) addModifier(attrInstance);
            else        removeModifier(attrInstance);
        } catch (Throwable e) {
            System.err.println("[Mugetsu] applySpeed error: " + e);
        }
    }

    private Object cachedAttrInstance = null;

    private Object getSpeedAttrInstance(Object player) {
        if (cachedAttrInstance != null) return cachedAttrInstance;

        // Probe every no-arg method on the player that returns a net.minecraft type,
        // then probe every Collection-returning method on that result.
        // The movement speed attribute instance has a base value near 0.1.
        for (Method m1 : player.getClass().getMethods()) {
            if (m1.getParameterCount() != 0) continue;
            if (m1.getReturnType().isPrimitive() || m1.getReturnType() == String.class) continue;
            if (!m1.getReturnType().getName().startsWith("net.minecraft")) continue;
            Object container;
            try { container = m1.invoke(player); }
            catch (Throwable ignored) { continue; }
            if (container == null) continue;

            for (Method m2 : container.getClass().getMethods()) {
                if (m2.getParameterCount() != 0) continue;
                if (!java.util.Collection.class.isAssignableFrom(m2.getReturnType())) continue;
                try {
                    java.util.Collection<?> col = (java.util.Collection<?>) m2.invoke(container);
                    if (col == null) continue;
                    for (Object item : col) {
                        double base = getBaseValue(item);
                        if (base > 0.05 && base < 0.2) {
                            System.out.println("[Mugetsu] Found speed attr via "
                                + m1.getName() + " -> " + m2.getName() + " base=" + base);
                            cachedAttrInstance = item;
                            return item;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        System.err.println("[Mugetsu] Could not find speed attribute.");
        return null;
    }

    private double getBaseValue(Object attrInstance) {
        for (Method m : attrInstance.getClass().getMethods()) {
            if (m.getReturnType() != double.class || m.getParameterCount() != 0) continue;
            try {
                double val = (double) m.invoke(attrInstance);
                if (val > 0.001 && val < 100.0) return val;
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    private Object cachedModifier = null;

    private void addModifier(Object attrInstance) throws Exception {
        removeModifier(attrInstance);

        Class<?> modClass = findClass("EntityAttributeModifier", "class_1322", "AttributeModifier");
        if (modClass == null) { System.err.println("[Mugetsu] modifier class not found"); return; }

        Class<?> opEnum = null;
        for (Class<?> inner : modClass.getDeclaredClasses()) {
            if (inner.isEnum()) { opEnum = inner; break; }
        }
        if (opEnum == null) { System.err.println("[Mugetsu] operation enum not found"); return; }

        Object addOp = ((Object[]) opEnum.getMethod("values").invoke(null))[0];

        Object modifier = null;

        // Try every constructor — fill args by type
        for (java.lang.reflect.Constructor<?> ctor : modClass.getConstructors()) {
            try {
                Class<?>[] params = ctor.getParameterTypes();
                Object[] args = new Object[params.length];
                boolean ok = true;
                for (int i = 0; i < params.length; i++) {
                    if (params[i] == double.class) {
                        args[i] = SPEED_MULTIPLIER;
                    } else if (params[i].isEnum()) {
                        args[i] = addOp;
                    } else if (params[i] == UUID.class) {
                        args[i] = SPEED_UUID;
                    } else if (params[i] == String.class) {
                        args[i] = "mugetsu_speed";
                    } else {
                        // Assume it's an Identifier/ResourceLocation — create one
                        Object id = createIdentifier(params[i], "mugetsu", "speed");
                        if (id != null) args[i] = id;
                        else { ok = false; break; }
                    }
                }
                if (ok) {
                    modifier = ctor.newInstance(args);
                    System.out.println("[Mugetsu] Created modifier via: " + ctor);
                    break;
                }
            } catch (Throwable ignored) {}
        }

        if (modifier == null) {
            StringBuilder sb = new StringBuilder("[Mugetsu] Could not create modifier. Constructors:");
            for (java.lang.reflect.Constructor<?> c : modClass.getConstructors())
                sb.append("\n  ").append(c);
            System.err.println(sb);
            return;
        }


        cachedModifier = modifier;
        callOneArg(attrInstance, modifier,
            "method_26837", "method_26835", "addPermanentModifier", "addModifier", "func_111121_a");
    }

    private void removeModifier(Object attrInstance) {
        if (cachedModifier == null) return;
        // Try removing by modifier object (method_6202) or by Identifier (method_6196)
        callOneArg(attrInstance, cachedModifier, "method_6202", "removeModifier", "func_111124_b");
    }

    private Object createIdentifier(Class<?> idClass, String namespace, String path) {
        // Try static factory: Identifier.of(namespace, path)
        for (Method m : idClass.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            if (m.getReturnType() != idClass && !m.getReturnType().isAssignableFrom(idClass)) continue;
            Class<?>[] p = m.getParameterTypes();
            try {
                if (p.length == 2 && p[0] == String.class && p[1] == String.class)
                    return m.invoke(null, namespace, path);
                if (p.length == 1 && p[0] == String.class)
                    return m.invoke(null, namespace + ":" + path);
            } catch (Throwable ignored) {}
        }
        // Try constructors
        try { return idClass.getConstructor(String.class, String.class).newInstance(namespace, path); }
        catch (Throwable ignored) {}
        try { return idClass.getConstructor(String.class).newInstance(namespace + ":" + path); }
        catch (Throwable ignored) {}
        return null;
    }

    private Class<?> findClass(String... names) {
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            for (String name : names) {
                if (cls.getSimpleName().equals(name) || cls.getName().equals(name)) return cls;
            }
        }
        return null;
    }

    private Object callOneArg(Object target, Object arg, String... names) {
        for (String name : names) {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1) {
                    try { return m.invoke(target, arg); } catch (Throwable ignored) {}
                }
            }
        }
        System.err.println("[Mugetsu] callOneArg: none of " + java.util.Arrays.toString(names) + " worked on " + target.getClass().getSimpleName());
        return null;
    }
}
