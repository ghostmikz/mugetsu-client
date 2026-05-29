package com.mugetsu.agent.module.combat;

import com.mugetsu.agent.hook.CallbackRegistry;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventGetReach;
import com.mugetsu.common.event.EventHandler;

/**
 * Reach — expands the player's interaction/attack range.
 * Hooks into InteractionManager.getPickRange() via ASM + CallbackRegistry.
 * Keybind: none (ClickGUI)
 */
public class ReachModule extends Module {

    public double reach = 6.0;

    public ReachModule(ResolvedNames names) {
        super("Reach", "Combat", 0, names);
    }

    @Override public void onEnable()  { CallbackRegistry.reachEnabled = true;  sync(); }
    @Override public void onDisable() { CallbackRegistry.reachEnabled = false; }

    @EventHandler
    public void onGetReach(EventGetReach event) {
        if (!isEnabled()) return;
        event.reachDistance = reach;
    }

    private void sync() { CallbackRegistry.reachDistance = reach; }

    public void setReach(double r) { this.reach = r; CallbackRegistry.reachDistance = r; }
}
