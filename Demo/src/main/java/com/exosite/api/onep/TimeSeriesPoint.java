package com.exosite.api.onep;

/**
 * Represents a time series point from the One Platform.
 */
public class TimeSeriesPoint {
    public void setTimeStamp(int timeStamp) {
        mTimeStamp = timeStamp;
    }

    public int getTimeStamp() {
        return mTimeStamp;
    }

    int mTimeStamp;

    public void setValue(Object value) {
        mValue = value;
    }

    public Object getValue() {
        return mValue;
    }

    Object mValue;

    public TimeSeriesPoint (Integer timeStamp, Object value) {
        mTimeStamp = timeStamp;
        mValue = value;
    }
}
