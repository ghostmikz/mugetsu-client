package com.mugetsu.agent.module.gui;

import com.mugetsu.agent.gui.HUDOverlay;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventRender2D;

/**
 * HUDModule — draws active module names in the top-right corner.
 * Always enabled by default.
 */
public class HUDModule extends Module {

    private HUDOverlay hud;
    private boolean init = false;

    public HUDModule(ResolvedNames names) {
        super("HUD", "GUI", 0, names);
    }

    @Override
    public void onEnable() {}

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (!isEnabled()) return;
        if (!init) { hud = new HUDOverlay(names); init = true; }
        hud.render(event.screenWidth, event.screenHeight);
    }
}
