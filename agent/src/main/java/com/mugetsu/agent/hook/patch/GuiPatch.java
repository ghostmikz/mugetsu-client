package com.mugetsu.agent.hook.patch;

import com.mugetsu.agent.mapping.ResolvedNames;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/** Injects 2D render hook at exit of InGameHud.render(). */
public class GuiPatch extends ClassVisitor {

    private static final String REGISTRY = "com/mugetsu/agent/hook/CallbackRegistry";
    private final ResolvedNames names;

    public GuiPatch(ClassVisitor cv, ResolvedNames names) {
        super(Opcodes.ASM9, cv);
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
        if (names.hudRenderMethodName != null
                && name.equals(names.hudRenderMethodName)
                && desc.equals(names.hudRenderMethodDesc)) {
            return new GuiAdapter(mv, access, name, desc);
        }
        return mv;
    }

    private static class GuiAdapter extends AdviceAdapter {
        GuiAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM9, mv, access, name, desc);
        }
        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == Opcodes.ATHROW) return;
            // Push placeholder screen dimensions — modules read from Minecraft instance anyway
            visitIntInsn(Opcodes.SIPUSH, 1920);
            visitIntInsn(Opcodes.SIPUSH, 1080);
            visitMethodInsn(Opcodes.INVOKESTATIC, REGISTRY,
                "onRender2D", "(II)V", false);
        }
    }
}
