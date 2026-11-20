package dev.hh00.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal, allocation-light event bus. Listeners are matched by exact event class.
 */
public final class EventBus {

    private final Map<Class<?>, List<RegisteredListener>> listeners = new ConcurrentHashMap<>();

    public <T extends Event> void subscribe(Class<T> eventType, Listener<T> listener) {
        subscribe(eventType, Priority.NORMAL, null, listener);
    }

    public <T extends Event> void subscribe(Class<T> eventType, Priority priority, Listener<T> listener) {
        subscribe(eventType, priority, null, listener);
    }

    private <T extends Event> void subscribe(Class<T> eventType, Priority priority, Object owner, Listener<T> listener) {
        List<RegisteredListener> list = listeners.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>());
        list.add(new RegisteredListener(eventType, priority, owner, listener));
        list.sort(Comparator.comparingInt(registered -> registered.priority.ordinal()));
    }

    /**
     * Scans {@code holder} (and its superclasses) for {@link Subscribe} methods.
     */
    public void register(Object holder) {
        if (holder == null) {
            throw new IllegalArgumentException("holder must not be null");
        }

        for (Class<?> type = holder.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                if (annotation == null) continue;

                if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("@Subscribe method must take exactly one Event parameter: " + method);
                }

                method.setAccessible(true);
                @SuppressWarnings("unchecked")
                Class<Event> eventType = (Class<Event>) method.getParameterTypes()[0];

                subscribe(eventType, annotation.value(), holder, event -> invoke(method, holder, event));
            }
        }
    }

    public void unregister(Object holder) {
        if (holder == null) return;

        for (List<RegisteredListener> list : listeners.values()) {
            list.removeIf(registered -> registered.owner == holder);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> T post(T event) {
        List<RegisteredListener> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return event;

        for (RegisteredListener registered : list) {
            ((Listener<T>) registered.listener).invoke(event);
        }
        return event;
    }

    private static void invoke(Method method, Object holder, Event event) {
        try {
            method.invoke(holder, event);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception in event listener " + method, e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access event listener " + method, e);
        }
    }

    private record RegisteredListener(Class<?> eventType, Priority priority, Object owner, Listener<?> listener) {
    }
}
