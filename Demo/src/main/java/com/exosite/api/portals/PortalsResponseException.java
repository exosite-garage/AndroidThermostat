package com.exosite.api.portals;

import com.exosite.api.ExoException;

public class PortalsResponseException extends ExoException {
    /**
     * @return HTTP response code
     */
    public int getResponseCode() {
        return mResponseCode;
    }

    /**
     * @param responseCode HTTP response code
     */
    public void setResponseCode(int responseCode) {
        mResponseCode = responseCode;
    }
    int mResponseCode;

    /**
     * @return content of HTTP response body
     */
    public String getResponseBody() {
        return mResponseBody;
    }

    /**
     * @param responseBody content of HTTP response body
     */
    public void setResponseBody(String responseBody) {
        mResponseBody = responseBody;
    }
    String mResponseBody;

    /**
     * Create response exception.
     *
     * @param message HTTP response message
     * @param responseCode HTTP response code
     * @param responseBody HTTP response body
     */
    public PortalsResponseException(final String message,
                                    int responseCode,
                                    String responseBody) {
        super(message);
        mResponseCode = responseCode;
        mResponseBody = responseBody;
    }
}
