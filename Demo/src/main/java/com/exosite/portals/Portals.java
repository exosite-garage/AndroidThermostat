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
        HTTPResult r = call(mDomain, "portal/", "", email, password);
        String responseBody = r.responseBody;
        try {
            response = new JSONArray(responseBody);
        } catch (JSONException e) {
            throw new PortalsRequestException("Invalid JSON in response. Response was:" + responseBody);
        }
        return response;
    }

    public void ResetPassword(String email)
            throws PortalsRequestException, PortalsResponseException {
        HTTPResult r = call(mDomain, "user/password",
                String.format("{\"email\":\"%s\",\"action\":\"reset\"}", email));
    }

    class HTTPResult {
        public int responseCode;
        public String responseBody;
    }

    /*
    Makes a call to the the Portals API with no authentication.
    domain - e.g. portals.exosite.com
    path   - e.g. user/password
    body   - e.g. {"action":"reset", "email":"johndoe@gmail555.com"}
     */
    public HTTPResult call(String domain, String path, String body)
            throws PortalsRequestException, PortalsResponseException {
        return call(domain, path, body, null, null);
    }

    /*
    Makes a call to the the Portals API.
    domain   - e.g. portals.exosite.com
    path     - e.g. user/password
    body     - e.g. {"action":"reset", "email":"johndoe@gmail555.com"}
    email    - email to use for authentication, or null for no authentication
    password - password to use for authentication, or null for no authentication
     */
    public HTTPResult call(String domain, String path, String body, String email, String password)
            throws PortalsRequestException, PortalsResponseException {
        URL url = null;
        HttpURLConnection conn = null;
        OutputStreamWriter writer = null;
        HTTPResult result = new HTTPResult();
        StringBuffer response = new StringBuffer();
        try {
            // for testing
            // url = new URL("http://192.168.3.142:8001/api/portals/v1/" + path);
            url = new URL("https://" + domain + "/api/portals/v1/" + path);
        } catch (MalformedURLException ex) {
            throw new PortalsRequestException("Malformed URL.");
        }
        try {
            conn = (HttpURLConnection) url.openConnection();
            if (email != null && password != null) {
                String encoded = Base64.encodeToString(
                        (email + ":" + password).getBytes(),
                        Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }
            conn.setUseCaches(false);
            conn.setConnectTimeout(this.mTimeoutSeconds * 1000);
            conn.setRequestProperty("User-Agent", "Android demo app");
            if (body.length() > 0) {
                conn.setRequestMethod("POST");
                //conn.setRequestProperty("Accept", "*/*");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Length", "" + body.length());
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

            result.responseCode = conn.getResponseCode();
            if (result.responseCode == 401)
            {
                throw new PortalsResponseException(
                        "username or password is invalid", conn.getResponseCode(), "");
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
                        "Error reading response", conn.getResponseCode(), "");
            } finally {
                if (null != reader)
                    reader.close();
            }

            if (result.responseCode >= 300) {
                throw new PortalsResponseException(result.responseBody, result.responseCode, response.toString());
            }

        } catch (IOException e) {
            throw new PortalsRequestException(
                    "IOException when opening/closing url connection.");
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        result.responseBody = response.toString();
        return result;
    }
}
