package com.mugetsu.common.module;

public interface IModule {
    String getName();
    String getCategory();
    boolean isEnabled();
    void onEnable();
    void onDisable();
    int getKeybind();
}
