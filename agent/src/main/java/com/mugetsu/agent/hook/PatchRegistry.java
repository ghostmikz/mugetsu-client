package com.mugetsu.agent.hook;

import com.mugetsu.agent.hook.patch.*;
import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** Maps internal class names to their chain of ClassVisitor patches. */
public final class PatchRegistry {

    public static final PatchRegistry INSTANCE = new PatchRegistry();

    // internal name → list of patch factories
    private final Map<String, List<BiFunction<ClassVisitor, ResolvedNames, ClassVisitor>>> registry = new HashMap<>();

    public void init(ResolvedNames names) {
        registry.clear();

        register(names.entityInternalName,              (cv, r) -> new EntityPatch(cv, r), names);
        register(names.gameRendererInternalName,        (cv, r) -> new GameRendererPatch(cv, r), names);
        register(names.interactionManagerInternalName,  (cv, r) -> new PlayerControllerPatch(cv, r), names);
        register(names.inGameHudInternalName,           (cv, r) -> new GuiPatch(cv, r), names);
        // LevelRenderer patch not implemented yet — placeholder
    }

    private void register(String internalName,
                          BiFunction<ClassVisitor, ResolvedNames, ClassVisitor> factory,
                          ResolvedNames names) {
        if (internalName == null) return;
        registry.computeIfAbsent(internalName, k -> new ArrayList<>()).add(factory);
    }

    /** Returns true if this class name has any registered patches. */
    public boolean hasPatches(String internalName) {
        return registry.containsKey(internalName);
    }

    /**
     * Applies all registered patches for the given class by wrapping the downstream visitor.
     * Patches are applied in registration order (outermost last).
     */
    public ClassVisitor applyPatches(String internalName, ClassVisitor downstream, ResolvedNames names) {
        List<BiFunction<ClassVisitor, ResolvedNames, ClassVisitor>> patches = registry.get(internalName);
        if (patches == null) return downstream;
        ClassVisitor cv = downstream;
        for (var factory : patches) {
            cv = factory.apply(cv, names);
        }
        return cv;
    }

    private PatchRegistry() {}
}
