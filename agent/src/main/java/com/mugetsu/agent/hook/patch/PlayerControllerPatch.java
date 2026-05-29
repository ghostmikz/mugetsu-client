package com.mugetsu.agent.hook.patch;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/** Hooks getPickRange() return value and attack() entry for cancellation. */
public class PlayerControllerPatch extends ClassVisitor {

    private static final String REGISTRY = "com/mugetsu/agent/hook/CallbackRegistry";
    private final ResolvedNames names;

    public PlayerControllerPatch(ClassVisitor cv, ResolvedNames names) {
        super(Opcodes.ASM9, cv);
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);

        if (names.getPickRangeMethodName != null
                && name.equals(names.getPickRangeMethodName)
                && desc.equals(names.getPickRangeMethodDesc)) {
            return new ReachAdapter(mv, access, name, desc);
        }

        if (names.attackMethodName != null
                && name.equals(names.attackMethodName)
                && desc.equals(names.attackMethodDesc)) {
            return new AttackAdapter(mv, access, name, desc);
        }

        return mv;
    }

    // Wraps the return value of getPickRange() through CallbackRegistry
    private static class ReachAdapter extends AdviceAdapter {
        private final Type returnType;
        ReachAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
            returnType = Type.getReturnType(desc);
        }
        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == Opcodes.ATHROW) return;
            // Stack: [float or double result]
            if (returnType.equals(Type.FLOAT_TYPE)) {
                visitInsn(Opcodes.F2D);
                visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                    "onGetReachDistance", "(D)D", false);
                visitInsn(Opcodes.D2F);
            } else if (returnType.equals(Type.DOUBLE_TYPE)) {
                visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                    "onGetReachDistance", "(D)D", false);
            }
        }
    }

    // Cancels attack() if CallbackRegistry.onAttack() returns true
    private static class AttackAdapter extends AdviceAdapter {
        AttackAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }
        @Override
        protected void onMethodEnter() {
            // First param is the target entity
            loadArg(0);
            visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                "onAttack", "(Ljava/lang/Object;)Z", false);
            Label proceed = new Label();
            visitJumpInsn(Opcodes.IFEQ, proceed);
            returnValue(); // cancel
            visitLabel(proceed);
        }
    }
}
