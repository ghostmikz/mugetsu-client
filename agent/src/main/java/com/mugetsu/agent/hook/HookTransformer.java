package com.mugetsu.agent.hook;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class HookTransformer implements ClassFileTransformer {

    // ASM 9.7.1 supports up to class version 68 (Java 24).
    // For newer versions (Java 25+ = 69+), we temporarily downgrade for reading
    // and restore the original version in the output so the JVM still accepts it.
    private static final int ASM_MAX_VERSION = 68;

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

            // Read original class version (bytes 6-7 in .class header)
            final int originalMajor = ((classfileBuffer[6] & 0xFF) << 8) | (classfileBuffer[7] & 0xFF);

            // Downgrade version if newer than what ASM supports
            byte[] readBuffer = classfileBuffer;
            if (originalMajor > ASM_MAX_VERSION) {
                readBuffer = classfileBuffer.clone();
                readBuffer[6] = 0;
                readBuffer[7] = (byte) ASM_MAX_VERSION;
            }

            ClassReader reader = new ClassReader(readBuffer);
            // Use the class's own loader (Fabric KnotClassLoader), COMPUTE_FRAMES for correctness.
            GameClassWriter writer = new GameClassWriter(loader);

            // Wrap with a visitor that restores the original version in the output
            ClassVisitor versionRestorer = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public void visit(int version, int access, String name,
                                  String signature, String superName, String[] interfaces) {
                    super.visit(originalMajor, access, name, signature, superName, interfaces);
                }
            };

            ClassVisitor chain = PatchRegistry.INSTANCE.applyPatches(className, versionRestorer, names);
            reader.accept(chain, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (Throwable t) {
            System.err.println("[Mugetsu] Transform failed for " + className + ": " + t);
            return null;
        }
    }
}
