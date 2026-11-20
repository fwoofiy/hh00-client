package dev.hh00.event.events;

import dev.hh00.event.Event;
import org.lwjgl.glfw.GLFW;

public class KeyEvent extends Event {

    private final int key;
    private final int scancode;
    private final int action;
    private final int modifiers;

    public KeyEvent(int key, int scancode, int action, int modifiers) {
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKey() {
        return key;
    }

    public int getScancode() {
        return scancode;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isPressed() {
        return action == GLFW.GLFW_PRESS;
    }

    public boolean isReleased() {
        return action == GLFW.GLFW_RELEASE;
    }
}
