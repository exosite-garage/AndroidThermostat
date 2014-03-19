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
import org.json.JSONObject;

/**
 * Provides access to the Portals API
 */
public class Portals {
    String mDomain = "portals.exosite.com";
    int mTimeoutSeconds = 15;

    /**
     * @return Portals domain configured for communication
     */
    public String getDomain() {
        return mDomain;
    }

    /**
     * @param domain Portals domain configured for API communication
     */
    public void setDomain(String domain) {
        mDomain = domain;
    }

    /**
     * @return HTTP timeout for calls to Portals API
     */
    public int getTimeoutSeconds() {
        return mTimeoutSeconds;
    }

    /**
     * @param timeoutSeconds HTTP timeout for calls to Portals API
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        mTimeoutSeconds = timeoutSeconds;
    }

    public Portals() {
    }

    /**
     * List a user's portals
     *
     * @param email    - email address of user to authenticate
     * @param password - portals password of user
     * @return JSONArray of  JSONObjects containing information about user's
     * managed and owned portals.
     * @throws PortalsRequestException
     * @throws PortalsResponseException
     */
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

    /**
     * Generate an email to a Portals user to reset their password.
     *
     * @param email - email address to reset
     * @throws PortalsRequestException
     * @throws PortalsResponseException
     */
    public void ResetPassword(String email)
            throws PortalsRequestException, PortalsResponseException {
        HTTPResult r = call(mDomain, "user/password",
                String.format("{\"email\":\"%s\",\"action\":\"reset\"}", email));
    }

    /**
     * Sign up a new Portals user on the configured domain. Since first
     * and last name are not specified, Portals will set them to "New"
     * and "User", respectively.
     *
     * @param email    - email address of user. This may not already
     *                   exist as a user in Portals.
     * @param password - password for user. UIs may want to confirm password.
     * @param plan     - Portals plan identifier for new user.
     * @throws PortalsRequestException
     * @throws PortalsResponseException
     */
    public void SignUp(String email, String password, String plan)
            throws PortalsRequestException, PortalsResponseException {
        SignUp(email, password, plan, "", "");
    }

    /**
     * Sign up a new Portals user on the configured domain.
     *
     * @param email     - email address of new user. This may not already
     *                    be registered in Portals.
     * @param password  - password for user. UIs may want to confirm password.
     * @param plan      - Portals plan identifier for new user.
     * @param firstName - first name of new user
     * @param lastName  - last name of new user
     * @throws PortalsRequestException
     * @throws PortalsResponseException
     */
    public void SignUp(String email, String password, String plan, String firstName, String lastName)
            throws PortalsRequestException, PortalsResponseException {
        HTTPResult r = call(mDomain, "user",
                String.format("{\"email\":\"%s\",\"password\":\"%s\",\"plan\":\"%s\",\"first_name\":\"%s\",\"last_name\":\"%s\"}",
                        email, password, plan, firstName, lastName));
    }

    /**
     * Add a new device to a user's portal.
     *
     * @param portalRID - identifier of portal where device should be added
     * @param vendor    - vendor identifier
     * @param model     - model identifier
     * @param sn        - serial number. This must match one of the sets of valid
     *                    serial numbers configured for the model.
     * @param name      - name of device
     * @param email     - email address of user to authenticate
     * @param password  - portals password of user
     * @return JSONObject containing "rid" - RID of the new device and "cik" - client key for new device
     * @throws PortalsRequestException
     * @throws PortalsResponseException
     */
    public JSONObject AddDevice(String portalRID, String vendor, String model, String sn, String name, String email, String password)
            throws PortalsRequestException, PortalsResponseException {
        HTTPResult r = call(mDomain, "device",
                String.format("{\"portal_rid\":\"%s\",\"vendor\":\"%s\",\"model\":\"%s\",\"serialnumber\":\"%s\",\"name\":\"%s\"}",
                        portalRID, vendor, model, sn, name), email, password);
        JSONObject response;
        String responseBody = r.responseBody;
        try {
            response = new JSONObject(responseBody);
        } catch (JSONException e) {
            throw new PortalsRequestException("Invalid JSON in response. Response was: " + responseBody);
        }
        return response;
    }

    public class HTTPResult {
        public int responseCode;
        public String responseBody;
    }

    /**
     * Makes a call to the the Portals API with no authentication.
     * @param domain - e.g. portals.exosite.com
     * @param path - e.g. user/password
     * @param body - e.g. {"action":"reset", "email":"johndoe@gmail555.com"}
     * @return result of HTTP call
     * @throws PortalsRequestException if an exception was thrown while making the request
     * @throws PortalsResponseException if the response status was >=300 or an exception was
     *         thrown while reading the response
     */
    public HTTPResult call(String domain, String path, String body)
            throws PortalsRequestException, PortalsResponseException {
        return call(domain, path, body, null, null);
    }

    /**
     * Makes a call to the the Portals API.
     * @param domain   - e.g. portals.exosite.com
     * @param path     - e.g. user/password
     * @param body     - e.g. {"action":"reset", "email":"johndoe@gmail555.com"}
     * @param email    - email to use for authentication, or null for no authentication
     * @param password - password to use for authentication, or null for no authentication
     * @return result of HTTP call
     * @throws PortalsRequestException if an exception was thrown while making the request
     * @throws PortalsResponseException if the response status was >=300 or an exception was
     *         thrown while reading the response
     */
    public HTTPResult call(String domain, String path, String body, String email, String password)
            throws PortalsRequestException, PortalsResponseException {
        URL url = null;
        HttpURLConnection conn = null;
        OutputStreamWriter writer = null;
        HTTPResult result = new HTTPResult();
        StringBuffer response = new StringBuffer();
        try {
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
            InputStreamReader isr;
            if (result.responseCode >= 300) {
                isr = new InputStreamReader(conn.getErrorStream());
            } else {
                isr = new InputStreamReader(conn.getInputStream());
            }
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(isr);
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
                result.responseBody = response.toString();
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
