package com.mugetsu.common.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    public static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, CopyOnWriteArrayList<Subscriber>> subs = new ConcurrentHashMap<>();

    public void register(Object listener) {
        for (Method method : listener.getClass().getMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            if (method.getParameterCount() != 1) continue;
            Class<?> eventType = method.getParameterTypes()[0];
            int priority = method.getAnnotation(EventHandler.class).priority();
            method.setAccessible(true);
            Subscriber sub = new Subscriber(method, listener, priority);
            CopyOnWriteArrayList<Subscriber> list =
                subs.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
            list.add(sub);
            // Rebuild sorted snapshot — CopyOnWriteArrayList sort is atomic
            List<Subscriber> sorted = new ArrayList<>(list);
            sorted.sort(Comparator.comparingInt(Subscriber::priority).reversed());
            subs.put(eventType, new CopyOnWriteArrayList<>(sorted));
        }
    }

    public void unregister(Object listener) {
        subs.values().forEach(list -> list.removeIf(s -> s.listener() == listener));
    }

    public void post(Object event) {
        List<Subscriber> list = subs.get(event.getClass());
        if (list == null) return;
        for (Subscriber sub : list) {
            if (event instanceof Cancellable c && c.isCancelled()) break;
            sub.invoke(event);
        }
    }

    private record Subscriber(Method method, Object listener, int priority) {
        void invoke(Object event) {
            try {
                method.invoke(listener, event);
            } catch (Throwable ignored) {}
        }
    }
}
