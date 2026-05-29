package com.mugetsu.agent.hook;

import com.mugetsu.common.event.*;

import java.lang.reflect.Method;

public final class CallbackRegistry {

    public static volatile EventBus eventBus;
    public static volatile boolean noFallEnabled    = false;
    public static volatile boolean reachEnabled     = false;
    public static volatile double  reachDistance    = 4.5;
    public static volatile boolean hitboxExpandEnabled = false;
    public static volatile double  hitboxExpand     = 0.5;
    public static volatile Object  localPlayerRef   = null;

    // Set by MugetsuClient after init — used by onTick() on the game thread (GLFW-safe)
    public static volatile Method glfwGetKeyRef  = null;
    public static volatile long   glfwWindowRef  = 0;
    private static final boolean[] prevKeys      = new boolean[350];

    // Called from MinecraftTickPatch on the game/render thread — safe to call glfwGetKey here
    public static void onTick() {
        if (eventBus == null || glfwGetKeyRef == null || glfwWindowRef == 0) return;
        for (int key = 32; key < prevKeys.length; key++) {
            boolean down;
            try { down = (int) glfwGetKeyRef.invoke(null, glfwWindowRef, key) == 1; }
            catch (Throwable e) { return; }
            if ( down && !prevKeys[key]) eventBus.post(new EventKey(key, 1));
            if (!down &&  prevKeys[key]) eventBus.post(new EventKey(key, 0));
            prevKeys[key] = down;
        }
    }

    public static void onTravel(Object entity, Object movementInput) {
        if (eventBus == null) return;
        eventBus.post(new EventMotion(0, 0, 0, 0, 0, false));
    }

    public static boolean shouldCancelFall(Object entity) {
        return noFallEnabled;
    }

    public static Object onGetBoundingBox(Object entity, Object originalBox) {
        if (!hitboxExpandEnabled) return originalBox;
        if (entity == localPlayerRef) return originalBox;
        EventEntityAABB event = new EventEntityAABB(entity, originalBox);
        if (eventBus != null) eventBus.post(event);
        return event.box;
    }

    public static void onRenderWorld(float partialTicks, Object matrixStack) {
        if (eventBus == null) return;
        eventBus.post(new EventRender(partialTicks, matrixStack));
    }

    public static void onRender2D(int width, int height) {
        if (eventBus == null) return;
        eventBus.post(new EventRender2D(width, height));
    }

    public static double onGetReachDistance(double original) {
        if (!reachEnabled) return original;
        EventGetReach event = new EventGetReach(original);
        if (eventBus != null) eventBus.post(event);
        return event.reachDistance;
    }

    public static boolean onAttack(Object target) {
        if (eventBus == null) return false;
        EventAttack event = new EventAttack(target);
        eventBus.post(event);
        return event.isCancelled();
    }

    public static void onKeyEvent(int key, int action) {
        if (eventBus == null || action == 2) return;
        eventBus.post(new EventKey(key, action));
    }

    private CallbackRegistry() {}
}
