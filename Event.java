package dev.hh00.event;

/**
 * Base class for every event posted on the {@link EventBus}.
 */
public abstract class Event {

    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
