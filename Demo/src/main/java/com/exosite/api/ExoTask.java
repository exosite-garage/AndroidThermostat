package com.exosite.api;

import android.os.AsyncTask;

/**
 * Represents an asynchronous task for doing an api call in the background.
 */
class ExoTask<T> extends AsyncTask<Object, Void, Boolean> {
    T mResult = null;
    ExoException mException;
    ExoCall<T> mCall;

    @Override
    protected Boolean doInBackground(Object... calls) {
        mResult = null;
        mException = null;
        mCall = (ExoCall<T>)calls[0];

        try {
            mResult = mCall.call();
        } catch (ExoException e) {
            mException = e;
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        mCall.getCallback().done(mResult, mException);
    }
}
