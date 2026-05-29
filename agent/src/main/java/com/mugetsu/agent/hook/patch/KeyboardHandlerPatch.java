package com.mugetsu.agent.hook.patch;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Hooks KeyboardHandler.onKey() — called by GLFW on the render/main thread.
 * Safe replacement for calling glfwGetKey from a background thread.
 * Descriptor: (JIIII)V  — window(long), key, scancode, action, mods (ints)
 */
public class KeyboardHandlerPatch extends ClassVisitor {

    private static final String REGISTRY = "com/mugetsu/agent/hook/CallbackRegistry";
    private final ResolvedNames names;

    public KeyboardHandlerPatch(ClassVisitor cv, ResolvedNames names) {
        super(Opcodes.ASM9, cv);
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String sig, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
        if (names.onKeyMethodName != null
                && name.equals(names.onKeyMethodName)
                && desc.equals(names.onKeyMethodDesc)) {
            return new AdviceAdapter(Opcodes.ASM9, mv, access, name, desc) {
                @Override
                protected void onMethodEnter() {
                    // args: (long window, int key, int scancode, int action, int mods)
                    loadArg(1); // key
                    loadArg(3); // action
                    visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                        "onKeyEvent", "(II)V", false);
                }
            };
        }
        return mv;
    }
}
