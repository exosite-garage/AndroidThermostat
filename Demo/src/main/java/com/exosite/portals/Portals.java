package com.exosite.portals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Provides access to the Portals API
 */
public class Portals {
    String mDomain = "portals.exosite.com";
    int mTimeoutSeconds = 15;

    public String getDomain() {
        return mDomain;
    }

    public void setDomain(String domain) {
        mDomain = domain;
    }

    public int getTimeoutSeconds() {
        return mTimeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        mTimeoutSeconds = timeoutSeconds;
    }

    public Portals() {
    }

    public JSONArray ListPortals(String email, String password)
            throws PortalsRequestException, PortalsResponseException {
        JSONArray response = null;
        String responseBody = call(mDomain, "portal/", "", email, password);
        try {
            response = new JSONArray(responseBody);
        } catch (JSONException e) {
            throw new PortalsRequestException("Invalid JSON in response. Response was:" + responseBody);
        }
        return response;
    }

    /*
    Makes a call to the the Portals API.
    domain - e.g. portals.exosite.com
    path - e.g. user/password
    body - e.g. {"action":"reset", "email":"johndoe@gmail555.com"}
     */
    public String call(String domain, String path, String body, String email, String password)
            throws PortalsRequestException, PortalsResponseException {
        URL url = null;
        HttpURLConnection conn = null;
        OutputStreamWriter writer = null;
        StringBuffer response = new StringBuffer();
        try {
            url = new URL("https://" + domain + "/api/portals/v1/" + path);
        } catch (MalformedURLException ex) {
            throw new PortalsRequestException("Malformed URL.");
        }
        try {
            conn = (HttpURLConnection) url.openConnection();
            if (email.length() > 0 && password.length() > 0) {
                String encoded = Base64.encodeToString(
                        (email + ":" + password).getBytes(),
                        Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Length", "" + body.length());
            conn.setConnectTimeout(this.mTimeoutSeconds * 1000);
            if (body.length() > 0) {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type",
                        "application/json; charset=utf-8");
                conn.connect();

                try {
                    writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(body);
                    writer.flush();
                } catch (IOException e) {
                    throw new PortalsRequestException(
                            "IOException writing http request.");
                } finally {
                    if (null != writer)
                        writer.close();
                }
            } else {
                conn.setRequestMethod("GET");
                conn.connect();
            }

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(conn
                        .getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                throw new PortalsResponseException(
                        "IOException when getting http response.");
            } finally {
                if (null != reader)
                    reader.close();
            }
        } catch (IOException e) {
            throw new PortalsRequestException(
                    "IOException when opening/closing url connection.");
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return response.toString();

    }
}
