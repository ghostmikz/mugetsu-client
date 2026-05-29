package com.mugetsu.agent.module.render;

import com.mugetsu.agent.gui.GlHelper;
import com.mugetsu.agent.mapping.ResolvedNames;
import com.mugetsu.agent.module.Module;
import com.mugetsu.common.event.EventHandler;
import com.mugetsu.common.event.EventRender;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * ESP — draws colored bounding boxes around all entities through walls.
 * Keybind: Z (GLFW 90)
 */
public class ESPModule extends Module {

    // Camera position — subtracted from entity world coords before GL draw
    private Method getCameraEntity;
    private boolean resolved = false;

    // World/entity list access
    private Method getLevel, getEntities;
    private Field cameraX, cameraY, cameraZ;

    public ESPModule(ResolvedNames names) {
        super("ESP", "Render", 90, names);
    }

    @EventHandler
    public void onRender(EventRender event) {
        if (!isEnabled()) return;
        Object player = getPlayer();
        if (player == null) return;

        List<?> entities = getEntityList(player);
        if (entities == null || entities.isEmpty()) return;

        Object camera = getCamera();
        double cx = camera != null ? getPosComponent(camera, "x", 0) : getPos(player, 0);
        double cy = camera != null ? getPosComponent(camera, "y", 1) : getPos(player, 1);
        double cz = camera != null ? getPosComponent(camera, "z", 2) : getPos(player, 2);

        GlHelper.beginESP();
        for (Object entity : entities) {
            if (entity == player) continue;
            drawEntityBox(entity, cx, cy, cz);
        }
        GlHelper.endESP();
    }

    private void drawEntityBox(Object entity, double cx, double cy, double cz) {
        try {
            Object box = getBoundingBox(entity);
            if (box == null) return;
            double minX = getBoxField(box, "minX", 0) - cx;
            double minY = getBoxField(box, "minY", 1) - cy;
            double minZ = getBoxField(box, "minZ", 2) - cz;
            double maxX = getBoxField(box, "maxX", 3) - cx;
            double maxY = getBoxField(box, "maxY", 4) - cy;
            double maxZ = getBoxField(box, "maxZ", 5) - cz;
            GlHelper.color(1f, 0.3f, 0.3f, 1f);
            GlHelper.drawBox(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (Throwable ignored) {}
    }

    private Object getBoundingBox(Object entity) {
        for (String name : new String[]{"getBoundingBox","getEntityBoundingBox","getBoundingBoxForCulling","method_5829"}) {
            try {
                Method m = entity.getClass().getMethod(name);
                return m.invoke(entity);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private List<?> getEntityList(Object player) {
        // Get the level/world from player
        Object level = null;
        for (String n : new String[]{"level", "world", "field_9243", "field_1735"}) {
            try {
                Field f = player.getClass().getField(n);
                f.setAccessible(true);
                level = f.get(player);
                if (level != null) break;
            } catch (Throwable ignored) {}
        }
        if (level == null) return null;
        // Get entity list from level
        for (Method m : level.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (!List.class.isAssignableFrom(m.getReturnType())
                    && !Collection.class.isAssignableFrom(m.getReturnType())) continue;
            String n = m.getName().toLowerCase();
            if (n.contains("entit")) {
                try {
                    Object result = m.invoke(level);
                    if (result instanceof List) return (List<?>) result;
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    private Object getCamera() {
        // GameRenderer.getMainCamera() or similar
        if (names.gameRendererField == null || mc() == null) return null;
        try {
            Object gr = names.gameRendererField.get(mc());
            if (gr == null) return null;
            for (Method m : gr.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().contains("Camera")) {
                    return m.invoke(gr);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private double getPosComponent(Object camera, String field, int idx) {
        try { return (double) camera.getClass().getField(field).get(camera); } catch (Throwable ignored) {}
        // Try getPosition() method
        for (Method m : camera.getClass().getMethods()) {
            if (m.getName().contains("getPos") && m.getParameterCount() == 0
                    && !m.getReturnType().isPrimitive()) {
                try { return getPos(m.invoke(camera), idx); } catch (Throwable ignored2) {}
            }
        }
        return 0;
    }

    private double getPos(Object obj, int idx) {
        int i = 0;
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (f.getType() == double.class) {
                    if (i == idx) { f.setAccessible(true); return (double) f.get(obj); }
                    i++;
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private double getBoxField(Object box, String name, int idx) {
        try { return (double) box.getClass().getField(name).get(box); } catch (Throwable ignored) {}
        int i = 0;
        try {
            for (Field f : box.getClass().getDeclaredFields()) {
                if (f.getType() == double.class) {
                    if (i == idx) { f.setAccessible(true); return (double) f.get(box); }
                    i++;
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }
}
