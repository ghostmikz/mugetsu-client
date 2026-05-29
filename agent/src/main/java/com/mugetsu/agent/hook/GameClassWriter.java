package com.mugetsu.agent.hook;

import org.objectweb.asm.ClassWriter;

/**
 * ClassWriter that uses the game's classloader for type hierarchy resolution.
 * Without this override, COMPUTE_FRAMES fails on net.minecraft types that are
 * not visible to the agent's own classloader.
 */
public class GameClassWriter extends ClassWriter {

    private final ClassLoader gameLoader;

    public GameClassWriter(int flags, ClassLoader gameLoader) {
        super(flags);
        this.gameLoader = gameLoader != null ? gameLoader : ClassLoader.getSystemClassLoader();
    }

    @Override
    protected ClassLoader getClassLoader() {
        return gameLoader;
    }
}
