package com.exosite.portals;

public abstract class PortalsCallback<T> {
    public abstract void done(T result, PortalsException e);
}

