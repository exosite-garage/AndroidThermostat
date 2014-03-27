package com.exosite.api;

public abstract class ExoCallback<T> {
    public abstract void done(T result, ExoException e);
}
