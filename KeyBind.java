package dev.hh00.core;

import dev.hh00.util.KeyUtil;

/**
 * GLFW key code wrapper. {@link #NONE} (0) means "not bound".
 */
public final class KeyBind {

    public static final int NONE = 0;

    private int key;

    public KeyBind(int key) {
        this.key = key;
    }

    public static KeyBind unbound() {
        return new KeyBind(NONE);
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isBound() {
        return key != NONE;
    }

    public String getName() {
        return KeyUtil.getName(key);
    }
}
