package com.mugetsu.agent.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Resolved MC class/method/field references, populated once at agent startup. */
public class ResolvedNames {

    // Resolved classes
    public Class<?> minecraftClass;
    public Class<?> localPlayerClass;
    public Class<?> gameRendererClass;
    public Class<?> levelRendererClass;
    public Class<?> interactionManagerClass;
    public Class<?> entityClass;
    public Class<?> livingEntityClass;
    public Class<?> aabbClass;
    public Class<?> inGameHudClass;
    public Class<?> optionsClass;

    // Internal ASM names (slash-separated) — used by HookTransformer to match classes
    public String entityInternalName;
    public String gameRendererInternalName;
    public String levelRendererInternalName;
    public String interactionManagerInternalName;
    public String inGameHudInternalName;

    // Resolved method names (for ASM patch method matching)
    public String travelMethodName;
    public String travelMethodDesc;
    public String causeFallDamageMethodName;
    public String causeFallDamageMethodDesc;
    public String getBoundingBoxMethodName;
    public String getBoundingBoxMethodDesc;
    public String renderLevelMethodName;
    public String renderLevelMethodDesc;
    public String getPickRangeMethodName;
    public String getPickRangeMethodDesc;
    public String attackMethodName;
    public String attackMethodDesc;
    public String hudRenderMethodName;
    public String hudRenderMethodDesc;

    // Cached reflective fields for modules
    public Field playerField;
    public Field gameRendererField;
    public Field levelRendererField;
    public Field gameModeField;
    public Field optionsField;
    public Field gammaField;

    // Cached reflective methods for modules
    public Method attackMethod;
    public Method getPickRangeMethod;

    // Live MC instance (set during resolution)
    public Object mcInstance;

    // The game's classloader — needed by GlHelper and GameClassWriter
    public ClassLoader gameLoader;

    // GLFW input — populated by MappingResolver
    public long windowHandle;
    public Method glfwGetKey;
    public Method glfwGetCursorPos;
    public Method glfwGetMouseButton;

    // Extra per-module caches
    public final Map<String, Object> extras = new HashMap<>();

    /** Returns the list of classes that HookTransformer should retransform. */
    public Class<?>[] getTargetClasses() {
        return new Class<?>[]{ entityClass, gameRendererClass, levelRendererClass,
                               interactionManagerClass, inGameHudClass };
    }

    public boolean isCriticalResolved() {
        return minecraftClass != null && entityClass != null && gameRendererClass != null;
    }
}
