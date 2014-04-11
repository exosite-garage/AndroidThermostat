package com.exosite.portals;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class JSONUtil {

    public static JSONArray sort(JSONArray array, Comparator c) {
        List asList = new ArrayList(array.length());
        for (int i=0; i<array.length(); i++){
            asList.add(array.opt(i));
        }
        Collections.sort(asList, c);
        JSONArray res = new JSONArray();
        for (Object o : asList){
            res.put(o);
        }
        return res;
    }

    public static JSONArray sortByTwoProperties(
            JSONArray array, final String property1, final String property2, final String fallback) {
        return JSONUtil.sort(array, new Comparator() {
            public int compare(Object a, Object b){
                JSONObject ja = (JSONObject)a;
                JSONObject jb = (JSONObject)b;
                int comp = ja.optString(property1, fallback).toLowerCase().compareTo(
                           jb.optString(property1, fallback).toLowerCase());
                if (comp == 0) {
                    comp = ja.optString(property2, fallback).toLowerCase().compareTo(
                           jb.optString(property2, fallback).toLowerCase());
                }
                return comp;
            }
        });
    }

    public static JSONArray sortByProperty(JSONArray array, final String property, final String fallback) {
        return JSONUtil.sort(array, new Comparator() {
            public int compare(Object a, Object b){
                JSONObject ja = (JSONObject)a;
                JSONObject jb = (JSONObject)b;
                return ja.optString(property, fallback).toLowerCase().compareTo(
                       jb.optString(property, fallback).toLowerCase());
            }
        });
    }

}
