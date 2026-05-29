package com.mugetsu.agent.hook.patch;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Injects hooks into Entity.travel() and Entity.causeFallDamage().
 */
public class EntityPatch extends ClassVisitor {

    private static final String REGISTRY = "com/mugetsu/agent/hook/CallbackRegistry";
    private final ResolvedNames names;

    public EntityPatch(ClassVisitor cv, ResolvedNames names) {
        super(Opcodes.ASM9, cv);
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);

        if (names.travelMethodName != null
                && name.equals(names.travelMethodName)
                && desc.equals(names.travelMethodDesc)) {
            return new TravelAdapter(mv, access, name, desc);
        }

        if (names.causeFallDamageMethodName != null
                && name.equals(names.causeFallDamageMethodName)
                && desc.equals(names.causeFallDamageMethodDesc)) {
            return new NoFallAdapter(mv, access, name, desc);
        }

        if (names.getBoundingBoxMethodName != null
                && name.equals(names.getBoundingBoxMethodName)
                && desc.equals(names.getBoundingBoxMethodDesc)) {
            return new BoundingBoxAdapter(mv, access, name, desc);
        }

        return mv;
    }

    // Injects: CallbackRegistry.onTravel(this, movementInput)
    private static class TravelAdapter extends AdviceAdapter {
        TravelAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }
        @Override
        protected void onMethodEnter() {
            loadThis();
            loadArg(0);
            visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                "onTravel", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
        }
    }

    // Injects: if (CallbackRegistry.shouldCancelFall(this)) return;
    private static class NoFallAdapter extends AdviceAdapter {
        NoFallAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }
        @Override
        protected void onMethodEnter() {
            loadThis();
            visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                "shouldCancelFall", "(Ljava/lang/Object;)Z", false);
            Label skip = new Label();
            visitJumpInsn(Opcodes.IFEQ, skip);
            // Figure out return type from descriptor and return appropriately
            returnValue();
            visitLabel(skip);
        }
    }

    // Wraps return value: box = CallbackRegistry.onGetBoundingBox(this, box)
    private class BoundingBoxAdapter extends AdviceAdapter {
        private final String aabbInternal;

        BoundingBoxAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
            // Determine AABB internal name for CHECKCAST
            aabbInternal = (names.aabbClass != null)
                ? names.aabbClass.getName().replace('.', '/') : null;
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == Opcodes.ATHROW) return;
            // Store the AABB return value in a new local slot
            int slot = newLocal(Type.getType(Object.class));
            storeLocal(slot);
            // Push: this, originalBox
            loadThis();
            loadLocal(slot);
            visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                "onGetBoundingBox", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            // Cast result back to AABB type (required by bytecode verifier)
            if (aabbInternal != null) {
                visitTypeInsn(Opcodes.CHECKCAST, aabbInternal);
            }
        }
    }
}
