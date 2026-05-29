package com.mugetsu.common.event;

public class EventAttack implements Cancellable {
    public final Object target;
    private boolean cancelled;

    public EventAttack(Object target) {
        this.target = target;
    }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean c) { this.cancelled = c; }
}
