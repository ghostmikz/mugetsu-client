package com.mugetsu.agent;

import com.mugetsu.agent.gui.GlHelper;
import com.mugetsu.agent.hook.CallbackRegistry;
import com.mugetsu.agent.hook.HookTransformer;
import com.mugetsu.agent.hook.PatchRegistry;
import com.mugetsu.agent.mapping.MappingResolver;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.ModuleManager;
import com.mugetsu.common.event.EventBus;
import com.mugetsu.common.event.EventUpdate;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MugetsuClient {

    public static ResolvedNames resolved;

    private static volatile Thread          tickThread;
    private static volatile HookTransformer transformer;
    private static volatile Instrumentation instrumentation;

    private final Instrumentation inst;

    public MugetsuClient(Instrumentation inst) {
        this.inst = inst;
    }

    public void start() {
        instrumentation = inst;
        System.out.println("[Mugetsu] Resolving mappings...");
        resolved = MappingResolver.resolve(inst);

        if (!resolved.isCriticalResolved()) {
            System.err.println("[Mugetsu] Critical class resolution failed. " +
                "MC=" + resolved.minecraftClass +
                " Entity=" + resolved.entityClass +
                " GameRenderer=" + resolved.gameRendererClass);
            return;
        }

        System.out.println("[Mugetsu] Initialising modules...");
        EventBus bus = EventBus.INSTANCE;
        ModuleManager.INSTANCE.init(resolved, bus);

        CallbackRegistry.eventBus       = bus;
        CallbackRegistry.localPlayerRef = null;
        CallbackRegistry.glfwGetKeyRef  = resolved.glfwGetKey;
        CallbackRegistry.glfwWindowRef  = resolved.windowHandle;

        if (resolved.gameLoader != null) GlHelper.init(resolved.gameLoader);

        PatchRegistry.INSTANCE.init(resolved);
        transformer = new HookTransformer(resolved);
        inst.addTransformer(transformer, true);

        // Retransform one class at a time — if one fails (e.g. Mixin conflict) the rest still apply
        int ok = 0;
        for (Class<?> c : resolved.getTargetClasses()) {
            if (c == null) continue;
            try {
                inst.retransformClasses(c);
                ok++;
            } catch (Throwable t) {
                System.err.println("[Mugetsu] Retransform skipped " + c.getName() + ": " + t);
            }
        }
        System.out.println("[Mugetsu] Retransformed " + ok + " classes.");

        startTickThread(resolved, bus);
        System.out.println("[Mugetsu] Ready. Press RShift for ClickGUI.");
    }

    /** Detaches the client: disables all modules, restores original bytecode, stops threads. */
    public static void shutdown() {
        // 1. Disable all modules
        try { ModuleManager.INSTANCE.getAll().forEach(m -> { if (m.isEnabled()) m.toggle(); }); }
        catch (Throwable ignored) {}

        // 2. Remove transformer so retransform restores original bytecode
        if (instrumentation != null && transformer != null) {
            try { instrumentation.removeTransformer(transformer); } catch (Throwable ignored) {}
        }

        // 3. Retransform without the transformer in place — classes revert to original
        if (instrumentation != null && resolved != null) {
            for (Class<?> c : resolved.getTargetClasses()) {
                if (c == null) continue;
                try { instrumentation.retransformClasses(c); } catch (Throwable ignored) {}
            }
        }

        // 4. Kill event bus and tick thread
        CallbackRegistry.eventBus = null;
        Thread tt = tickThread;
        if (tt != null) tt.interrupt();

        System.out.println("[Mugetsu] Uninjected.");
    }

    private static void startTickThread(ResolvedNames r, EventBus bus) {
        // Find Minecraft.execute(Runnable) — posts a task to the game thread (GLFW-safe)
        Method executeMethod = null;
        try { executeMethod = r.minecraftClass.getMethod("execute", Runnable.class); }
        catch (Throwable ignored) {}
        final Method exec = executeMethod;

        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }

                // Post player-position EventUpdate (no GLFW, safe from any thread)
                Object player = getField(r.playerField, r.mcInstance);
                if (player != null) {
                    CallbackRegistry.localPlayerRef = player;
                    double x = 0, y = 0, z = 0;
                    try {
                        Method gx = findMethod(player, new String[]{"getX","method_23264"}, 0, double.class);
                        if (gx != null) {
                            x = (double) gx.invoke(player);
                            Method gy = findMethod(player, new String[]{"getY","method_23263"}, 0, double.class);
                            Method gz = findMethod(player, new String[]{"getZ","method_23265"}, 0, double.class);
                            if (gy != null) y = (double) gy.invoke(player);
                            if (gz != null) z = (double) gz.invoke(player);
                        }
                    } catch (Throwable ignored) {}
                    bus.post(new EventUpdate(player, x, y, z, 0, 0, false));
                }

                // Delegate GLFW key polling to the game thread via Minecraft.execute()
                // glfwGetKey must only be called on the thread that owns the GLFW window
                if (exec != null && r.mcInstance != null) {
                    try { exec.invoke(r.mcInstance, (Runnable) CallbackRegistry::onTick); }
                    catch (Throwable ignored) {}
                }
            }
        }, "Mugetsu-Tick");
        t.setDaemon(true);
        t.start();
        tickThread = t;
    }

    private static Object getField(Field f, Object obj) {
        if (f == null || obj == null) return null;
        try { return f.get(obj); } catch (Throwable e) { return null; }
    }

    private static Method findMethod(Object obj, String[] names, int paramCount, Class<?> ret) {
        for (String name : names)
            for (Method m : obj.getClass().getMethods())
                if (m.getName().equals(name) && m.getParameterCount() == paramCount
                        && (ret == null || m.getReturnType() == ret))
                    return m;
        return null;
    }
}
