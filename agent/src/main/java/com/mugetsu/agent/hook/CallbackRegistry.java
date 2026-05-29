package com.mugetsu.agent.hook;

import com.mugetsu.common.event.*;

/**
 * Static bridge called from ASM-injected bytecode running inside game classes.
 * Must stay on the bootstrap classpath (Boot-Class-Path: agent.jar) so the game's
 * classloader can resolve it.  Keep every method as thin as possible.
 */
public final class CallbackRegistry {

    // Set by MugetsuClient after init
    public static volatile EventBus eventBus;
    public static volatile boolean noFallEnabled = false;
    public static volatile boolean reachEnabled = false;
    public static volatile double reachDistance = 4.5;
    public static volatile boolean hitboxExpandEnabled = false;
    public static volatile double hitboxExpand = 0.5;
    public static volatile Object localPlayerRef = null;

    // Called at Entity.travel() entry
    public static void onTravel(Object entity, Object movementInput) {
        if (eventBus == null) return;
        eventBus.post(new EventMotion(0, 0, 0, 0, 0, false));
    }

    // Called at Entity.causeFallDamage() entry — returns true to cancel the method
    public static boolean shouldCancelFall(Object entity) {
        return noFallEnabled;
    }

    // Called at Entity.getBoundingBox() return — may replace the box
    public static Object onGetBoundingBox(Object entity, Object originalBox) {
        if (!hitboxExpandEnabled) return originalBox;
        // Don't expand local player's own box
        if (entity == localPlayerRef) return originalBox;
        EventEntityAABB event = new EventEntityAABB(entity, originalBox);
        if (eventBus != null) eventBus.post(event);
        return event.box;
    }

    // Called at GameRenderer.renderLevel() entry
    public static void onRenderWorld(float partialTicks, Object matrixStack) {
        if (eventBus == null) return;
        eventBus.post(new EventRender(partialTicks, matrixStack));
    }

    // Called at InGameHud.render() exit
    public static void onRender2D(int width, int height) {
        if (eventBus == null) return;
        eventBus.post(new EventRender2D(width, height));
    }

    // Called at InteractionManager.getPickRange() return — may return larger value
    public static double onGetReachDistance(double original) {
        if (!reachEnabled) return original;
        EventGetReach event = new EventGetReach(original);
        if (eventBus != null) eventBus.post(event);
        return event.reachDistance;
    }

    // Called at InteractionManager.attack() entry — returns true to cancel
    public static boolean onAttack(Object target) {
        if (eventBus == null) return false;
        EventAttack event = new EventAttack(target);
        eventBus.post(event);
        return event.isCancelled();
    }

    private CallbackRegistry() {}
}
