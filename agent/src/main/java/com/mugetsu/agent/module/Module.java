package com.mugetsu.agent.module;

import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.common.module.IModule;

public abstract class Module implements IModule {

    protected final ResolvedNames names;
    private final String name;
    private final String category;
    private volatile boolean enabled = false;
    private int keybind;

    protected Module(String name, String category, int keybind, ResolvedNames names) {
        this.name = name;
        this.category = category;
        this.keybind = keybind;
        this.names = names;
    }

    @Override public String getName()     { return name; }
    @Override public String getCategory() { return category; }
    @Override public boolean isEnabled()  { return enabled; }
    @Override public int getKeybind()     { return keybind; }
    public void setKeybind(int key)       { this.keybind = key; }

    public void toggle() {
        if (enabled) {
            enabled = false;
            onDisable();
        } else {
            enabled = true;
            onEnable();
        }
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}

    // Convenience: access MC instance through cached field
    protected Object mc() { return names.mcInstance; }

    protected Object getPlayer() {
        if (names.playerField == null || mc() == null) return null;
        try { return names.playerField.get(mc()); } catch (Throwable e) { return null; }
    }

    protected Object getOptions() {
        if (names.optionsField == null || mc() == null) return null;
        try { return names.optionsField.get(mc()); } catch (Throwable e) { return null; }
    }

    protected Object getGameMode() {
        if (names.gameModeField == null || mc() == null) return null;
        try { return names.gameModeField.get(mc()); } catch (Throwable e) { return null; }
    }
}
