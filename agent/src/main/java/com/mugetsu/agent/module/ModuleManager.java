package com.mugetsu.agent.module;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.combat.*;
import com.mugetsu.agent.module.gui.*;
import com.mugetsu.agent.module.movement.*;
import com.mugetsu.agent.module.render.*;
import com.mugetsu.common.event.EventBus;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventKey;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();
    private ResolvedNames names;

    public void init(ResolvedNames names, EventBus bus) {
        this.names = names;

        // Register all modules
        add(new FlyModule(names),           bus);
        add(new SpeedModule(names),         bus);
        add(new NoFallModule(names),        bus);
        add(new ESPModule(names),           bus);
        add(new FullbrightModule(names),    bus);
        add(new XrayModule(names),          bus);
        add(new ReachModule(names),         bus);
        add(new AutoClickerModule(names),   bus);
        add(new TriggerBotModule(names),    bus);
        add(new HitboxExpanderModule(names),bus);
        add(new ClickGUIModule(names),      bus);
        add(new HUDModule(names),           bus);

        // Listen for key events to toggle modules
        bus.register(this);

        System.out.println("[Mugetsu] " + modules.size() + " modules registered.");
    }

    private void add(Module m, EventBus bus) {
        modules.add(m);
        bus.register(m);
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (event.action != 1) return; // only PRESS
        for (Module m : modules) {
            if (m.getKeybind() == event.key) {
                m.toggle();
            }
        }
    }

    public boolean isEnabled(String name) {
        for (Module m : modules) {
            if (m.getName().equals(name)) return m.isEnabled();
        }
        return false;
    }

    public void toggle(String name) {
        for (Module m : modules) {
            if (m.getName().equals(name)) { m.toggle(); return; }
        }
    }

    public List<Module> getAll() { return modules; }

    public List<Module> getEnabled() {
        return modules.stream().filter(Module::isEnabled).collect(Collectors.toList());
    }

    private ModuleManager() {}
}
