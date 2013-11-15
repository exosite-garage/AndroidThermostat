package com.exosite.onepv1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class OnePlatformRPC {
    String mURL;
    int mTimeout;

    /** This class represents a binding library of One Platform API.
     *
     * @param url			The URL of Exosite OnePlatform JSON-RPC service.
     *                      e.g. http://m2.exosite.com/api:v1/rpc/process
     * @param timeout   	HTTP timeout for request, in seconds
     */
    public OnePlatformRPC(String url, int timeout) {
        mURL = url;
        mTimeout = timeout;
    }

    public OnePlatformRPC() {
        mURL = "http://m2.exosite.com/api:v1/rpc/process";
        mTimeout = 5;
    }

    public String callRPC(String request)
            throws HttpRPCRequestException, HttpRPCResponseException {
        URL url = null;
        HttpURLConnection conn = null;
        OutputStreamWriter writer = null;
        StringBuffer response = new StringBuffer();
        try {
            url = new URL(this.mURL);
        } catch (MalformedURLException ex) {
            throw new HttpRPCRequestException("Malformed URL.");
        }
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type",
                    "application/json; charset=utf-8");
            conn.setRequestProperty("Content-Length", "" + request.length());
            conn.setConnectTimeout(this.mTimeout * 1000);
            try {
                writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(request);
                writer.flush();
            } catch (IOException e) {
                throw new HttpRPCRequestException(
                        "Failed to make http request.");
            } finally {
                if (null != writer)
                    writer.close();
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
                throw new HttpRPCResponseException(
                        "Failed to get http response.");
            } finally {
                if (null != reader)
                    reader.close();
            }
        } catch (IOException e) {
            throw new HttpRPCRequestException(
                    "Failed to open/close url connection.");
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return response.toString();

    }

    public static ArrayList<Result> parseResponses(String response) throws OnePlatformException, JSONException {
        JSONArray callRespArray = null;
        try {
            callRespArray = new JSONArray(response);
        } catch (Exception e) {
            JSONObject callError = new JSONObject(response);
            String errormsg = callError.getString("error");
            throw new OnePlatformException(errormsg);
        }
        ArrayList<Result> results = new ArrayList<Result>();
        for (int i = 0; i < callRespArray.length(); i++) {
            JSONObject callRespObj = (JSONObject) callRespArray.get(i);
            if (callRespObj.has("error")) {
                String errorMsg = callRespObj.get("error").toString();
                throw new OnePlatformException(errorMsg);
            }
            String status = callRespObj.get("status").toString();
            if (Result.OK.equals(status)) {
                if (callRespObj.has("result"))
                    results.add(new Result(Result.OK, callRespObj.get("result")));
                else
                    results.add(new Result(Result.OK, null));
            } else {
                results.add(new Result(Result.FAIL, status));
            }
        }
        return results;


    }
}
