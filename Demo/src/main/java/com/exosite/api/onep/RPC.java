package com.exosite.api.onep;

import android.util.Log;
import android.util.Pair;

import com.exosite.api.ExoCall;
import com.exosite.api.ExoCallback;
import com.exosite.api.ExoException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class RPC {
    private static final String TAG = "RPC";
    String mURL;
    int mTimeout;

    /** This class represents a binding library of One Platform API.
     *
     * @param url			The URL of Exosite OnePlatform JSON-RPC service.
     *                      e.g. http://m2.exosite.com/api:v1/rpc/process
     * @param timeout   	HTTP timeout for request, in seconds
     */
    public RPC(String url, int timeout) {
        mURL = url;
        mTimeout = timeout;
    }

    public RPC() {
        mURL = "http://m2.exosite.com/api:v1/rpc/process";
        mTimeout = 5;
    }

    private void putRIDOrAlias(JSONArray args, String ridOrAlias) throws JSONException {
        if (ridOrAlias.matches("[0-9a-fA-F]{40}")) {
            args.put(ridOrAlias);
        } else {
            JSONObject alias = new JSONObject();
            alias.put("alias", ridOrAlias);
            args.put(alias);
        }
    }

    public String call(String request)
            throws RPCRequestException, RPCResponseException {
        URL url = null;
        HttpURLConnection conn = null;
        OutputStreamWriter writer = null;
        StringBuffer response = new StringBuffer();
        try {
            url = new URL(this.mURL);
        } catch (MalformedURLException ex) {
            throw new RPCRequestException("Malformed URL.");
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
                throw new RPCRequestException(
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
                throw new RPCResponseException(
                        "Failed to get http response.");
            } finally {
                if (null != reader)
                    reader.close();
            }
        } catch (IOException e) {
            throw new RPCRequestException(
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

    public JSONObject buildMultiRequest(String cik, JSONArray calls, boolean assignIds) throws JSONException {
        JSONObject req = new JSONObject();

        JSONObject auth = new JSONObject();
        auth.put("cik", cik);

        req.put("auth", auth);

        if (assignIds) {
            for (int i = 0; i < calls.length(); i++) {
                JSONObject call = calls.getJSONObject(i);
                call.put("id", i);
            }
        }
        req.put("calls", calls);

        return req;
    }

    public JSONObject buildSingleRequest(String cik, String procedure, JSONArray arguments) throws JSONException {
        JSONObject call = new JSONObject();
        call.put("procedure", procedure);
        call.put("arguments", arguments);
        call.put("id", 1);

        JSONArray calls = new JSONArray();
        calls.put(call);

        return buildMultiRequest(cik, calls, false);
    }

    public List<Result> callAndRaiseAnyError(JSONObject requestBody)
            throws RPCRequestException, RPCResponseException, OnePlatformException, JSONException {
        ArrayList<Result> results = null;
        Log.v(TAG, requestBody.toString());
        String responseBody = null;
        responseBody = this.call(requestBody.toString());
        Log.v(TAG, responseBody.toString());

        if (responseBody != null) {
            results = parseResponses(responseBody);
            // check the results for non ok results
            for (Result r : results) {
                if (r.getStatus() != "ok") {
                    throw new OnePlatformException("OneP command result not \"ok\": " + r.toString());
                }
            }
        } else {
            throw new RPCResponseException("Response from OneP is empty");
        }

        return results;
    }

    public JSONObject listing(String cik, JSONArray types)
            throws OneException {
        try {
            JSONObject ret = new JSONObject();
            JSONObject options = new JSONObject();

            JSONArray arguments = new JSONArray();
            arguments.put(types);
            arguments.put(options);

            JSONObject requestBody = buildSingleRequest(cik, "listing", arguments);
            List<Result> results = this.callAndRaiseAnyError(requestBody);

            return (JSONObject)results.get(0).getResult();

        } catch (JSONException e) {
            throw new OneException(e.toString());
        }
    }

    public void listingInBackground(final String cik,
                                    final JSONArray types,
                                    final ExoCallback<JSONObject> callback) {
        new ExoCall<JSONObject>(callback) {
            @Override
            public JSONObject call() throws ExoException {
                return listing(cik, types);
            }
        }.callInBackground();
    }


    public ArrayList<JSONObject> info(String cik, List<String> rids, JSONObject infoOptions)
            throws OneException {

        try {
            JSONArray calls = new JSONArray();
            for (String rid: rids) {
                JSONArray args = new JSONArray();
                args.put(rid);
                args.put(infoOptions);

                JSONObject call = new JSONObject();
                call.put("procedure", "info");
                call.put("arguments", args);
                calls.put(call);
            }
            JSONObject requestBody = buildMultiRequest(cik, calls, true);

            List<Result> results = this.callAndRaiseAnyError(requestBody);

            ArrayList<JSONObject> infoList = new ArrayList<JSONObject>();
            for (int i = 0; i < rids.size(); i++) {
                Result r = results.get(i);
                JSONObject info = (JSONObject)r.getResult();
                infoList.add(info);
            }
            return infoList;

        } catch (JSONException e) {
            throw new OneException(e.toString());
        }

    }

    public void infoInBackground(final String cik,
                                 final List<String> rids,
                                 final JSONObject infoOptions,
                                 final ExoCallback<ArrayList<JSONObject>> callback) {
        new ExoCall<ArrayList<JSONObject>>(callback) {
            @Override
            public ArrayList<JSONObject> call() throws ExoException {
                return info(cik, rids, infoOptions);
            }
        }.callInBackground();
    }

    /*
    Returns a JSONObject mapping type strings to JSONObjects that in turn map RIDs to info JSONObjects.
    The content of info JSONObjects depend on infoOptions. See here for details:
    https://github.com/exosite/api/tree/master/rpc#info
    e.g. {"client": {"<rid1>":{"key":"<cik1>"}}}
     */
    public JSONObject infoListing(String cik, JSONArray types, JSONObject infoOptions)
                throws OneException {
        JSONObject infoListing = new JSONObject();

        try {
            // first get listing
            JSONObject listing = listing(cik, types);

            Iterator<String> iter = listing.keys();
                while(iter.hasNext()) {
                    String type = iter.next();
                JSONArray typeRIDs = listing.getJSONArray(type);
                ArrayList<String> rids = new ArrayList<String>();
                for (int i = 0; i < typeRIDs.length(); i++) {
                    rids.add(typeRIDs.getString(i));
                }
                JSONObject typeObj = new JSONObject();
                if (rids.size() > 0) {
                    List<JSONObject> infos = info(cik, rids, infoOptions);

                    for(int i = 0; i < rids.size(); i++) {
                        typeObj.put(rids.get(i), infos.get(i));
                    }
                }
                infoListing.put(type, typeObj);
            }
        } catch (JSONException e) {
            throw new OneException(e.toString());
        }
        return infoListing;
    }

    public void infoListingInBackground(final String cik,
                                        final JSONArray types,
                                        final JSONObject infoOptions,
                                        final ExoCallback<JSONObject> callback) {
        new ExoCall<JSONObject>(callback) {
            @Override
            public JSONObject call() throws ExoException {
                return infoListing(cik, types, infoOptions);
            }
        }.callInBackground();
    }

    public HashMap<String, List<TimeSeriesPoint>> read(
            final String cik,
            final List<String> ridsOrAliases,
            final JSONObject readOptions)
                throws OneException {
        HashMap<String, List<TimeSeriesPoint>> readResults = new HashMap<String, List<TimeSeriesPoint>>();
        try {
            JSONArray calls = new JSONArray();
            for (String ridOrAlias: ridsOrAliases) {
                JSONArray args = new JSONArray();
                putRIDOrAlias(args, ridOrAlias);

                args.put(readOptions);

                JSONObject call = new JSONObject();
                call.put("procedure", "read");
                call.put("arguments", args);
                calls.put(call);
            }
            JSONObject requestBody = buildMultiRequest(cik, calls, true);

            List<Result> results = this.callAndRaiseAnyError(requestBody);

            for (int callIndex = 0; callIndex < ridsOrAliases.size(); callIndex++) {
                Result r = results.get(callIndex);
                ArrayList<TimeSeriesPoint> values = new ArrayList<TimeSeriesPoint>();
                JSONArray a = (JSONArray)r.getResult();
                for (int pointIndex = 0; pointIndex < a.length(); pointIndex++) {
                    JSONArray pt = a.getJSONArray(pointIndex);
                    values.add(new TimeSeriesPoint(pt.getInt(0), pt.get(1)));
                }
                readResults.put(ridsOrAliases.get(callIndex), values);
            }
            return readResults;

        } catch (JSONException e) {
            throw new OneException(e.toString());
        }

    }

    public void readInBackground(
            final String cik,
            final List<String> ridsOrAliases,
            final JSONObject readOptions,
            final ExoCallback<HashMap<String, List<TimeSeriesPoint>>> callback) {

        new ExoCall<HashMap<String, List<TimeSeriesPoint>>>(callback) {
            @Override
            public HashMap<String, List<TimeSeriesPoint>> call() throws ExoException {
                return read(cik, ridsOrAliases, readOptions);
            }
        }.callInBackground();
    }

    public HashMap<String, TimeSeriesPoint> readLatest(
            final String cik,
            final List<String> ridsOrAliases)
                throws OneException {
        HashMap<String, List<TimeSeriesPoint>> results = read(cik, ridsOrAliases, new JSONObject());
        HashMap<String, TimeSeriesPoint> singlePointResults = new HashMap<String, TimeSeriesPoint>();
        for (String key: results.keySet()) {
            List<TimeSeriesPoint> points = results.get(key);
            singlePointResults.put(key, points.size() == 0 ? null : points.get(0));
        }
        return singlePointResults;
    }

    public void readLatestInBackground(
            final String cik,
            final List<String> ridsOrAliases,
            final ExoCallback<HashMap<String, TimeSeriesPoint>> callback) {

        new ExoCall<HashMap<String, TimeSeriesPoint>>(callback) {
            @Override
            public HashMap<String, TimeSeriesPoint> call() throws ExoException {
                return readLatest(cik, ridsOrAliases);
            }
        }.callInBackground();
    }

    public void write(
            final String cik,
            final String ridOrAlias,
            final String value)
                throws OneException {
        try {
            JSONArray calls = new JSONArray();
            JSONArray args = new JSONArray();

            putRIDOrAlias(args, ridOrAlias);
            args.put(value);

            JSONObject call = new JSONObject();
            call.put("procedure", "write");
            call.put("arguments", args);
            calls.put(call);

            JSONObject requestBody = buildMultiRequest(cik, calls, true);
            this.callAndRaiseAnyError(requestBody);

        } catch (JSONException e) {
            throw new OneException(e.toString());
        }
    }

    public void writeInBackground(
            final String cik,
            final String ridOrAlias,
            final String value,
            final ExoCallback<Void> callback) {
        new ExoCall<Void>(callback) {
            @Override
            public Void call() throws ExoException {
                write(cik, ridOrAlias, value);
                return null;
            }
        }.callInBackground();
    }

}
