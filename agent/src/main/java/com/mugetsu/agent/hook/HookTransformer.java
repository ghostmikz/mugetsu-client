package com.mugetsu.agent.hook;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

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
            ClassWriter writer = new GameClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS,
                names.gameLoader
            );
            ClassVisitor chain = PatchRegistry.INSTANCE.applyPatches(className, writer, names);
            reader.accept(chain, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (Throwable t) {
            System.err.println("[Mugetsu] Transform failed for " + className + ": " + t);
            return null; // return null = leave original bytecode intact
        }
    }
}
