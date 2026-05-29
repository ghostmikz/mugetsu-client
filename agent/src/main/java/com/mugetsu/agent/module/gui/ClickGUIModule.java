package com.mugetsu.agent.module.gui;

import com.mugetsu.agent.gui.ClickGUI;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventRender2D;

/**
 * ClickGUIModule — shows/hides the in-game module toggle overlay.
 * Keybind: Right Shift (GLFW 344)
 */
public class ClickGUIModule extends Module {

    private static final int GLFW_KEY_RIGHT_SHIFT = 344;
    private ClickGUI gui;
    private boolean guiInit = false;

    public ClickGUIModule(ResolvedNames names) {
        super("ClickGUI", "GUI", GLFW_KEY_RIGHT_SHIFT, names);
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (!isEnabled()) return;
        if (!guiInit) { gui = new ClickGUI(names); gui.init(); guiInit = true; }
        gui.render(event.screenWidth, event.screenHeight);
    }
}
