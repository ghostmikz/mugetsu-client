package com.mugetsu.agent.hook.patch;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/** Injects onRenderWorld() hook into GameRenderer.renderLevel(). */
public class GameRendererPatch extends ClassVisitor {

    private static final String REGISTRY = "com/mugetsu/agent/hook/CallbackRegistry";
    private final ResolvedNames names;

    public GameRendererPatch(ClassVisitor cv, ResolvedNames names) {
        super(Opcodes.ASM9, cv);
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);

        if (names.renderLevelMethodName != null
                && name.equals(names.renderLevelMethodName)
                && desc.equals(names.renderLevelMethodDesc)) {
            return new RenderAdapter(mv, access, name, desc);
        }

        return mv;
    }

    // Injects: CallbackRegistry.onRenderWorld(partialTicks, matrixStack)
    // Works for both signatures: (F JLPoseStack;)V and (F J)V etc.
    private static class RenderAdapter extends AdviceAdapter {
        RenderAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }
        @Override
        protected void onMethodEnter() {
            // First arg is float partialTicks, second may be long or PoseStack
            // Load partialTicks (arg 0 = float)
            loadArg(0);
            // Load second arg as Object (matrixStack or null)
            Type[] argTypes = Type.getArgumentTypes(methodDesc);
            if (argTypes.length >= 2 && !argTypes[1].equals(Type.LONG_TYPE)
                    && !argTypes[1].equals(Type.getType(String.class))) {
                // Could be PoseStack or similar — skip long nanoTime param if present
                // Try to find the Object-type arg (PoseStack)
                Object matStack = null;
                for (int i = 1; i < argTypes.length; i++) {
                    if (argTypes[i].getSort() == Type.OBJECT) {
                        loadArg(i);
                        visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                            "onRenderWorld", "(FLjava/lang/Object;)V", false);
                        return;
                    }
                }
            }
            // No PoseStack arg found — pass null
            visitInsn(Opcodes.ACONST_NULL);
            visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                "onRenderWorld", "(FLjava/lang/Object;)V", false);
        }
    }
}
