package com.mugetsu.agent;

import java.lang.instrument.Instrumentation;

public class AgentMain {

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        Thread hook = new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            new MugetsuClient(inst).start();
        }, "Mugetsu-Init");
        hook.setDaemon(true);
        hook.start();
        System.out.println("[Mugetsu] Agent injected.");
    }
}
