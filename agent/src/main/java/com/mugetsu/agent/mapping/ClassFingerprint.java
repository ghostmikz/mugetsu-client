package com.mugetsu.agent.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Identifies Minecraft classes by structure when name-based lookup fails. */
public final class ClassFingerprint {

    /**
     * Returns true if cls looks like the main Minecraft singleton class:
     * has a static field of its own type AND a field referencing a player-like type.
     */
    public static boolean isMinecraftClass(Class<?> cls) {
        if (!cls.getName().startsWith("net.minecraft")) return false;
        boolean hasSelfStatic = false;
        boolean hasPlayerField = false;
        for (Field f : cls.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == cls) hasSelfStatic = true;
            String tn = f.getType().getSimpleName().toLowerCase();
            if (tn.contains("localplayer") || tn.contains("clientplayer")) hasPlayerField = true;
        }
        return hasSelfStatic && hasPlayerField;
    }

    /**
     * Returns true if cls looks like the GameRenderer:
     * has a field referencing a class that contains "Camera" or "Frustum",
     * and has several float/double fields (fov, zoom, etc.).
     */
    public static boolean isGameRendererClass(Class<?> cls) {
        if (!cls.getName().startsWith("net.minecraft")) return false;
        int floatFields = 0;
        boolean hasCameraOrFrustum = false;
        for (Field f : cls.getDeclaredFields()) {
            if (f.getType() == float.class || f.getType() == double.class) floatFields++;
            String tn = f.getType().getSimpleName();
            if (tn.contains("Camera") || tn.contains("Frustum") || tn.contains("Shader")) {
                hasCameraOrFrustum = true;
            }
        }
        return floatFields >= 3 && hasCameraOrFrustum;
    }

    /**
     * Returns true if cls looks like the base Entity class:
     * has double fields x/y/z and a UUID field, is not an interface.
     */
    public static boolean isEntityClass(Class<?> cls) {
        if (!cls.getName().startsWith("net.minecraft")) return false;
        if (cls.isInterface() || cls.isEnum()) return false;
        if (cls.getSuperclass() == null || cls.getSuperclass() == Object.class) return false;
        boolean hasUUID = false;
        int doubleFields = 0;
        for (Field f : cls.getDeclaredFields()) {
            if (f.getType() == java.util.UUID.class) hasUUID = true;
            if (f.getType() == double.class) doubleFields++;
        }
        // Entity has many double fields (pos, velocity, rotation) + a UUID
        return hasUUID && doubleFields >= 6;
    }

    /**
     * Finds the travel(Vec3) method on an entity class by looking for a
     * void method taking a single object parameter whose type has 3 double fields.
     */
    public static Method findTravelMethod(Class<?> entityClass) {
        for (Method m : entityClass.getMethods()) {
            if (m.getReturnType() != void.class) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> param = m.getParameterTypes()[0];
            if (isVec3Like(param)) return m;
        }
        return null;
    }

    /** Returns true if cls looks like a Vec3 (has x, y, z double fields or 3 double fields). */
    public static boolean isVec3Like(Class<?> cls) {
        int doubleFields = 0;
        for (Field f : cls.getDeclaredFields()) {
            if (f.getType() == double.class) doubleFields++;
        }
        return doubleFields == 3;
    }

    private ClassFingerprint() {}
}
