package com.mugetsu.agent.module.movement;

import com.mugetsu.agent.hook.CallbackRegistry;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;

/**
 * NoFall — cancels fall damage by intercepting Entity.causeFallDamage() via ASM.
 * Keybind: none by default (toggle from ClickGUI).
 */
public class NoFallModule extends Module {

    public NoFallModule(ResolvedNames names) {
        super("NoFall", "Movement", 0, names);
    }

    @Override
    public void onEnable() {
        CallbackRegistry.noFallEnabled = true;
    }

    @Override
    public void onDisable() {
        CallbackRegistry.noFallEnabled = false;
    }
}
