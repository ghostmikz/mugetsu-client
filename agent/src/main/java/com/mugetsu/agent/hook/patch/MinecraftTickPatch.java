package com.mugetsu.agent.hook.patch;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Hooks Minecraft.tick() — runs on the game/render thread every game tick.
 * Safe to call glfwGetKey from here, unlike background threads.
 * This replaces KeyboardHandlerPatch which couldn't find the private onKey method.
 */
public class MinecraftTickPatch extends ClassVisitor {

    private static final String REGISTRY = "com/mugetsu/agent/hook/CallbackRegistry";
    private final ResolvedNames names;

    public MinecraftTickPatch(ClassVisitor cv, ResolvedNames names) {
        super(Opcodes.ASM9, cv);
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String sig, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
        if (names.minecraftTickMethodName != null
                && name.equals(names.minecraftTickMethodName)
                && desc.equals(names.minecraftTickMethodDesc)) {
            return new AdviceAdapter(Opcodes.ASM9, mv, access, name, desc) {
                @Override
                protected void onMethodEnter() {
                    visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY, "onTick", "()V", false);
                }
            };
        }
        return mv;
    }
}
