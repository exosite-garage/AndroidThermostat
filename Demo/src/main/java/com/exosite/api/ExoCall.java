package com.exosite.api;

public class ExoCall<T> {
    public ExoCall(ExoCallback<T> callback) {
        mCallback = callback;
    }
    private ExoCallback<T> mCallback;
    ExoCallback<T> getCallback() {
        return mCallback;
    }
    public T call() throws ExoException {
        return null;
    }
    public void callInBackground() {
        ExoTask task = new ExoTask();
        task.execute(this);
    }
    protected void handleException(ExoException e) {
        mCallback.done(null, e);
    }
}
