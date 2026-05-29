package com.mugetsu.agent.hook;

import org.objectweb.asm.ClassWriter;

/**
 * ClassWriter that uses the transforming class's own classloader and gracefully
 * handles missing types (Mixin's CallbackInfo etc.) by falling back to Object.
 * Using COMPUTE_MAXS instead of COMPUTE_FRAMES avoids loading referenced classes
 * entirely, which is the main source of crashes on Fabric/Mixin environments.
 */
public class GameClassWriter extends ClassWriter {

    private final ClassLoader loader;

    public GameClassWriter(ClassLoader loader) {
        // COMPUTE_MAXS: recomputes stack/local sizes without loading referenced classes.
        // Safe on Fabric where COMPUTE_FRAMES would fail on Mixin-injected types.
        super(ClassWriter.COMPUTE_MAXS);
        this.loader = loader != null ? loader : ClassLoader.getSystemClassLoader();
    }

    @Override
    protected ClassLoader getClassLoader() {
        return loader;
    }

    /**
     * Override to prevent NoClassDefFoundError / crashes when Mixin-injected types
     * (CallbackInfo, etc.) can't be resolved. Returning Object is always safe here
     * because we only use COMPUTE_MAXS (frames are not recomputed).
     */
    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable t) {
            return "java/lang/Object";
        }
    }
}
