package com.mugetsu.common.event;

public class EventEntityAABB {
    public final Object entity;
    public Object box; // mutable — modules may replace this

    public EventEntityAABB(Object entity, Object box) {
        this.entity = entity;
        this.box = box;
    }
}
