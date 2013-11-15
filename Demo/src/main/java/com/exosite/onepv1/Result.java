package com.exosite.onepv1;

/**
 * Represents a result from the OneP RPC
 */

public class Result {
    private String mStatus;
    private Object mResult;

    Result(String status, Object result) {
        this.mStatus = status;
        this.mResult = result;
    }

    public String getStatus() {
        return mStatus;
    }

    public Object getResult() {
        return mResult;
    }

    public final static String OK = "ok";
    public final static String FAIL = "fail";
}
