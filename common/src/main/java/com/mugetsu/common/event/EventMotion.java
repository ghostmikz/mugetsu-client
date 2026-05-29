package com.mugetsu.common.event;

public class EventMotion implements Cancellable {
    public double x, y, z;
    public float yaw, pitch;
    public boolean onGround;
    private boolean cancelled;

    public EventMotion(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround;
    }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean c) { this.cancelled = c; }
}
