package com.mugetsu.common.event;

public class EventKey {
    public final int key;
    public final int action; // GLFW_PRESS=1, GLFW_RELEASE=0, GLFW_REPEAT=2

    public EventKey(int key, int action) {
        this.key = key;
        this.action = action;
    }
}
