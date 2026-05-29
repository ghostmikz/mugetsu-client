package com.mugetsu.common.event;

public class EventRender {
    public final float partialTicks;
    public final Object matrixStack;

    public EventRender(float partialTicks, Object matrixStack) {
        this.partialTicks = partialTicks;
        this.matrixStack = matrixStack;
    }
}
