package dev.hh00.event;

@FunctionalInterface
public interface Listener<T extends Event> {

    void invoke(T event);
}
