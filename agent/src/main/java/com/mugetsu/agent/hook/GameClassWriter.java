package com.mugetsu.agent.hook;

import org.objectweb.asm.ClassWriter;

/**
 * ClassWriter that uses the transforming class's own classloader and falls back
 * to Object when getCommonSuperClass fails (handles Mixin/Fabric types safely).
 */
public class GameClassWriter extends ClassWriter {

    private final ClassLoader loader;

    public GameClassWriter(ClassLoader loader) {
        super(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.loader = loader != null ? loader : ClassLoader.getSystemClassLoader();
    }

    @Override
    protected ClassLoader getClassLoader() {
        return loader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable t) {
            // Mixin-injected types (CallbackInfo etc.) may not be resolvable.
            // Returning Object is always safe — it just widens the frame type.
            return "java/lang/Object";
        }
    }
}
