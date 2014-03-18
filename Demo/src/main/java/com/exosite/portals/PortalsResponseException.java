package com.exosite.portals;

public class PortalsResponseException extends Exception {
    // HTTP response code
    public int getResponseCode() {
        return mResponseCode;
    }

    public void setResponseCode(int responseCode) {
        mResponseCode = responseCode;
    }
    int mResponseCode;

    // Content of HTTP response body
    public String getResponseBody() {
        return mResponseBody;
    }

    public void setResponseBody(String responseBody) {
        mResponseBody = responseBody;
    }
    String mResponseBody;

    public PortalsResponseException(final String message,
                                    int responseCode,
                                    String responseBody) {
        super(message);
        mResponseCode = responseCode;
        mResponseBody = responseBody;
    }
}
