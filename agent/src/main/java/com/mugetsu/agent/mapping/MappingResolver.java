package com.mugetsu.agent.mapping;

import org.objectweb.asm.Type;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class MappingResolver {

    public static ResolvedNames resolve(Instrumentation inst) {
        ResolvedNames r = new ResolvedNames();

        // --- Resolve classes ---
        Class<?>[] loaded = inst.getAllLoadedClasses();
        r.minecraftClass        = findClass(loaded, KnownMappings.CLASSES.get("Minecraft"),        ClassFingerprint::isMinecraftClass);
        r.entityClass           = findClass(loaded, KnownMappings.CLASSES.get("Entity"),           ClassFingerprint::isEntityClass);
        r.livingEntityClass     = findClass(loaded, KnownMappings.CLASSES.get("LivingEntity"),     null);
        r.gameRendererClass     = findClass(loaded, KnownMappings.CLASSES.get("GameRenderer"),     ClassFingerprint::isGameRendererClass);
        r.levelRendererClass    = findClass(loaded, KnownMappings.CLASSES.get("LevelRenderer"),    null);
        r.interactionManagerClass = findClass(loaded, KnownMappings.CLASSES.get("InteractionManager"), null);
        r.aabbClass             = findClass(loaded, KnownMappings.CLASSES.get("AABB"),             null);
        r.inGameHudClass        = findClass(loaded, KnownMappings.CLASSES.get("InGameHud"),        null);
        r.optionsClass          = findClass(loaded, KnownMappings.CLASSES.get("Options"),          null);
        r.localPlayerClass      = findClass(loaded, KnownMappings.CLASSES.get("LocalPlayer"),      null);

        // --- ASM internal names (for HookTransformer class matching) ---
        if (r.entityClass != null)
            r.entityInternalName = r.entityClass.getName().replace('.', '/');
        if (r.gameRendererClass != null)
            r.gameRendererInternalName = r.gameRendererClass.getName().replace('.', '/');
        if (r.levelRendererClass != null)
            r.levelRendererInternalName = r.levelRendererClass.getName().replace('.', '/');
        if (r.interactionManagerClass != null)
            r.interactionManagerInternalName = r.interactionManagerClass.getName().replace('.', '/');
        if (r.inGameHudClass != null)
            r.inGameHudInternalName = r.inGameHudClass.getName().replace('.', '/');
        r.keyboardHandlerClass = findClass(loaded, KnownMappings.CLASSES.get("KeyboardHandler"), null);
        if (r.keyboardHandlerClass != null)
            r.keyboardHandlerInternalName = r.keyboardHandlerClass.getName().replace('.', '/');

        // --- Resolve MC instance + game classloader ---
        if (r.minecraftClass != null) {
            r.mcInstance = getMcInstance(r.minecraftClass);
            r.gameLoader = r.minecraftClass.getClassLoader();
            r.minecraftInternalName = r.minecraftClass.getName().replace('.', '/');
            System.out.println("[Mugetsu] MC class: " + r.minecraftClass.getName());
            Method tick = resolveMethod(r.minecraftClass, KnownMappings.METHODS.get("Minecraft.tick"), 0, void.class);
            if (tick != null) {
                r.minecraftTickMethodName = tick.getName();
                r.minecraftTickMethodDesc = Type.getMethodDescriptor(tick);
                System.out.println("[Mugetsu] tick() -> " + tick.getName());
            }
        }

        // --- Resolve fields on Minecraft class ---
        if (r.minecraftClass != null) {
            r.playerField       = resolveField(r.minecraftClass, KnownMappings.FIELDS.get("Minecraft.player"));
            r.gameRendererField = resolveField(r.minecraftClass, KnownMappings.FIELDS.get("Minecraft.gameRenderer"));
            r.levelRendererField = resolveField(r.minecraftClass, KnownMappings.FIELDS.get("Minecraft.levelRenderer"));
            r.gameModeField     = resolveField(r.minecraftClass, KnownMappings.FIELDS.get("Minecraft.gameMode"));
            r.optionsField      = resolveField(r.minecraftClass, KnownMappings.FIELDS.get("Minecraft.options"));
        }
        if (r.optionsClass != null) {
            r.gammaField = resolveField(r.optionsClass, KnownMappings.FIELDS.get("Options.gamma"));
        }

        // --- Resolve method names/descriptors for ASM patches ---
        if (r.entityClass != null) {
            Method m = resolveMethod(r.entityClass, KnownMappings.METHODS.get("Entity.travel"), 1, void.class);
            if (m == null) m = ClassFingerprint.findTravelMethod(r.entityClass);
            if (m != null) {
                r.travelMethodName = m.getName();
                r.travelMethodDesc = Type.getMethodDescriptor(m);
                System.out.println("[Mugetsu] travel() -> " + m.getName() + " " + r.travelMethodDesc);
            }
            Method fall = resolveMethod(r.entityClass, KnownMappings.METHODS.get("Entity.causeFallDamage"), -1, null);
            if (fall != null) {
                r.causeFallDamageMethodName = fall.getName();
                r.causeFallDamageMethodDesc = Type.getMethodDescriptor(fall);
            }
            Method aabb = resolveMethod(r.entityClass, KnownMappings.METHODS.get("Entity.getBoundingBox"), 0, null);
            if (aabb != null) {
                r.getBoundingBoxMethodName = aabb.getName();
                r.getBoundingBoxMethodDesc = Type.getMethodDescriptor(aabb);
            }
        }
        if (r.gameRendererClass != null) {
            Method m = resolveMethod(r.gameRendererClass, KnownMappings.METHODS.get("GameRenderer.renderLevel"), -1, null);
            if (m != null) {
                r.renderLevelMethodName = m.getName();
                r.renderLevelMethodDesc = Type.getMethodDescriptor(m);
                System.out.println("[Mugetsu] renderLevel() -> " + m.getName());
            }
        }
        if (r.interactionManagerClass != null) {
            Method pick = resolveMethod(r.interactionManagerClass, KnownMappings.METHODS.get("InteractionManager.getPickRange"), -1, null);
            if (pick != null) {
                r.getPickRangeMethodName = pick.getName();
                r.getPickRangeMethodDesc = Type.getMethodDescriptor(pick);
                r.getPickRangeMethod = pick;
            }
            Method atk = resolveMethod(r.interactionManagerClass, KnownMappings.METHODS.get("InteractionManager.attack"), -1, null);
            if (atk != null) {
                r.attackMethodName = atk.getName();
                r.attackMethodDesc = Type.getMethodDescriptor(atk);
                r.attackMethod = atk;
                r.attackMethod.setAccessible(true);
            }
        }
        if (r.inGameHudClass != null) {
            Method m = resolveMethod(r.inGameHudClass, KnownMappings.METHODS.get("InGameHud.render"), -1, void.class);
            if (m != null) {
                r.hudRenderMethodName = m.getName();
                r.hudRenderMethodDesc = Type.getMethodDescriptor(m);
            }
        }

        // --- Resolve KeyboardHandler.onKey ---
        if (r.keyboardHandlerClass != null) {
            // onKey is private; use getDeclaredMethods to find it
            for (java.lang.reflect.Method m : r.keyboardHandlerClass.getDeclaredMethods()) {
                String n = m.getName();
                boolean nameMatch = false;
                for (String candidate : KnownMappings.METHODS.get("KeyboardHandler.onKey")) {
                    if (n.equals(candidate)) { nameMatch = true; break; }
                }
                // Signature: (JIIII)V — window(long) + key,scancode,action,mods(int)
                if (nameMatch && m.getParameterCount() == 5
                        && m.getParameterTypes()[0] == long.class
                        && m.getReturnType() == void.class) {
                    r.onKeyMethodName = m.getName();
                    r.onKeyMethodDesc = Type.getMethodDescriptor(m);
                    System.out.println("[Mugetsu] onKey() -> " + m.getName() + " " + r.onKeyMethodDesc);
                    break;
                }
            }
        }

        // --- Resolve GLFW ---
        resolveGlfw(loaded, r);

        return r;
    }

    // ---- Helpers ----

    private static Class<?> findClass(Class<?>[] loaded, String[] candidates, java.util.function.Predicate<Class<?>> fingerprint) {
        // Pass 1: try known names
        if (candidates != null) {
            for (String name : candidates) {
                for (Class<?> cls : loaded) {
                    if (cls.getName().equals(name)) return cls;
                }
            }
        }
        // Pass 2: fingerprint fallback
        if (fingerprint != null) {
            for (Class<?> cls : loaded) {
                try { if (fingerprint.test(cls)) return cls; } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private static Object getMcInstance(Class<?> mcClass) {
        for (Field f : mcClass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() != mcClass) continue;
            try {
                f.setAccessible(true);
                Object inst = f.get(null);
                if (inst != null) return inst;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Field resolveField(Class<?> cls, String[] names) {
        if (names == null) return null;
        for (String name : names) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    /**
     * Finds a method by trying known names. paramCount=-1 means any count.
     * returnType=null means any return type.
     */
    private static Method resolveMethod(Class<?> cls, String[] names, int paramCount, Class<?> returnType) {
        if (names == null) return null;
        for (String name : names) {
            for (Method m : cls.getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (paramCount >= 0 && m.getParameterCount() != paramCount) continue;
                if (returnType != null && m.getReturnType() != returnType) continue;
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static void resolveGlfw(Class<?>[] loaded, ResolvedNames r) {
        for (Class<?> cls : loaded) {
            if (!cls.getName().equals("org.lwjgl.glfw.GLFW")) continue;
            for (Method m : cls.getMethods()) {
                if (m.getName().equals("glfwGetKey") && m.getParameterCount() == 2) {
                    r.glfwGetKey = m;
                }
                if (m.getName().equals("glfwGetCursorPos") && m.getParameterCount() == 3) {
                    r.glfwGetCursorPos = m;
                }
                if (m.getName().equals("glfwGetMouseButton") && m.getParameterCount() == 2) {
                    r.glfwGetMouseButton = m;
                }
            }
            break;
        }
        // Resolve window handle
        if (r.mcInstance != null) {
            r.windowHandle = resolveWindowHandle(r.mcInstance, r.minecraftClass);
        }
    }

    private static long resolveWindowHandle(Object mcInstance, Class<?> mcClass) {
        // Try known window getters
        for (String name : new String[]{"getWindow", "method_22683", "getMainWindow"}) {
            try {
                Method m = mcClass.getMethod(name);
                Object window = m.invoke(mcInstance);
                if (window == null) continue;
                for (String h : new String[]{"getHandle", "method_4490"}) {
                    try {
                        Method hm = window.getClass().getMethod(h);
                        if (hm.getReturnType() == long.class) {
                            long handle = (long) hm.invoke(window);
                            if (handle != 0) return handle;
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
        // Scan fields
        for (Field f : mcClass.getDeclaredFields()) {
            if (f.getType().isPrimitive() || f.getType().isArray()) continue;
            if (!f.getType().getName().startsWith("net.minecraft")) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(mcInstance);
                if (val == null) continue;
                for (Method m : val.getClass().getMethods()) {
                    if (m.getReturnType() == long.class && m.getParameterCount() == 0) {
                        long handle = (long) m.invoke(val);
                        if (handle > 0xFFFFL) return handle;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    private MappingResolver() {}
}
