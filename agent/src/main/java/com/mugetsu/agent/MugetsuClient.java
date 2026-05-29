package com.mugetsu.agent;

import com.mugetsu.agent.gui.GlHelper;
import com.mugetsu.agent.hook.CallbackRegistry;
import com.mugetsu.agent.hook.HookTransformer;
import com.mugetsu.agent.hook.PatchRegistry;
import com.mugetsu.agent.mapping.MappingResolver;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.ModuleManager;
import com.mugetsu.common.event.EventBus;
import com.mugetsu.common.event.EventKey;
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

        CallbackRegistry.eventBus = bus;
        CallbackRegistry.localPlayerRef = null;

        if (resolved.gameLoader != null) GlHelper.init(resolved.gameLoader);

        PatchRegistry.INSTANCE.init(resolved);
        transformer = new HookTransformer(resolved);
        inst.addTransformer(transformer, true);

        try {
            Class<?>[] targets = resolved.getTargetClasses();
            java.util.List<Class<?>> valid = new java.util.ArrayList<>();
            for (Class<?> c : targets) if (c != null) valid.add(c);
            if (!valid.isEmpty()) {
                inst.retransformClasses(valid.toArray(new Class[0]));
                System.out.println("[Mugetsu] Retransformed " + valid.size() + " classes.");
            }
        } catch (Throwable t) {
            System.err.println("[Mugetsu] Retransform failed: " + t);
        }

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
            try {
                Class<?>[] targets = resolved.getTargetClasses();
                java.util.List<Class<?>> valid = new java.util.ArrayList<>();
                for (Class<?> c : targets) if (c != null) valid.add(c);
                if (!valid.isEmpty()) instrumentation.retransformClasses(valid.toArray(new Class[0]));
            } catch (Throwable ignored) {}
        }

        // 4. Kill event bus and tick thread
        CallbackRegistry.eventBus = null;
        Thread tt = tickThread;
        if (tt != null) tt.interrupt();

        System.out.println("[Mugetsu] Uninjected.");
    }

    private static void startTickThread(ResolvedNames r, EventBus bus) {
        Thread t = new Thread(() -> {
            boolean[] prevKeys = new boolean[400];
            while (!Thread.currentThread().isInterrupted()) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }

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

                if (r.glfwGetKey != null && r.windowHandle != 0) {
                    for (int key = 32; key < prevKeys.length; key++) {
                        boolean down = false;
                        try { down = (int) r.glfwGetKey.invoke(null, r.windowHandle, key) == 1; }
                        catch (Throwable ignored) {}
                        if (down  && !prevKeys[key]) bus.post(new EventKey(key, 1));
                        if (!down &&  prevKeys[key]) bus.post(new EventKey(key, 0));
                        prevKeys[key] = down;
                    }
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
