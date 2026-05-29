package com.mugetsu.common.event;

public class EventUpdate {
    private final Object player;
    public double x, y, z;
    public float yaw, pitch;
    public boolean onGround;

    public EventUpdate(Object player, double x, double y, double z,
                       float yaw, float pitch, boolean onGround) {
        this.player = player;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround;
    }

    public Object getPlayer() { return player; }
}
