package com.mugetsu.agent.hook;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class HookTransformer implements ClassFileTransformer {

    private final ResolvedNames names;
    public static boolean verbose = false;

    public HookTransformer(ResolvedNames names) {
        this.names = names;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        if (className == null || classfileBuffer == null) return null;
        if (!PatchRegistry.INSTANCE.hasPatches(className)) return null;

        try {
            if (verbose) System.out.println("[Mugetsu] Patching " + className);

            ClassReader reader = new ClassReader(classfileBuffer);
            // Use the class's own loader (Fabric's KnotClassLoader), not names.gameLoader.
            // GameClassWriter uses COMPUTE_MAXS only — safe on Mixin-transformed classes.
            GameClassWriter writer = new GameClassWriter(loader);
            ClassVisitor chain = PatchRegistry.INSTANCE.applyPatches(className, writer, names);
            // SKIP_FRAMES: preserve original frames; combined with COMPUTE_MAXS this is
            // the safest option for already-Mixin-transformed bytecode.
            reader.accept(chain, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (Throwable t) {
            System.err.println("[Mugetsu] Transform failed for " + className + ": " + t);
            return null; // null = leave original bytecode untouched
        }
    }
}
