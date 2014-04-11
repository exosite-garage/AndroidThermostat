package com.exosite.portals;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Cache {
    private static final String TAG = "Cache";
    private static final String CACHE_PREFERENCE_KEY_SUFFIX = "_by_domain_cache";

    static void ClearAllCache(Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(activity);
        sharedPreferences.edit()
                .remove("device" + CACHE_PREFERENCE_KEY_SUFFIX)
                .remove("portal" + CACHE_PREFERENCE_KEY_SUFFIX).commit();
    }

    private static void CacheByDomain(Activity activity, String domain, String thingName, Object thing) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(activity);
        JSONObject domainThingLists = null;
        String domainThingJSON = sharedPreferences.getString(
                thingName + CACHE_PREFERENCE_KEY_SUFFIX, null);
        try {
            if (domainThingJSON != null) {
                domainThingLists = new JSONObject(domainThingJSON);
            } else {
                domainThingLists = new JSONObject();
            }
            domainThingLists.put(domain, thing);
            sharedPreferences.edit().putString(
                    thingName + CACHE_PREFERENCE_KEY_SUFFIX,
                    domainThingLists.toString()).commit();
        } catch (JSONException je) {
            Log.e(TAG, je.toString());
        }
    }

    private static Object RestoreFromCacheByDomain(Activity activity, String domain, String thingName) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(activity);
        String domainThingJSON = sharedPreferences.getString(
                thingName + CACHE_PREFERENCE_KEY_SUFFIX, null);
        if (domainThingJSON != null) {
            try {
                JSONObject domainThingLists = new JSONObject(domainThingJSON);
                if (domainThingLists.has(domain)) {
                    return domainThingLists.getJSONArray(domain);
                }
            } catch (JSONException je) {
                Log.e(TAG, je.toString());
            }
        }
        return null;
    }

    static void CacheDeviceList(Activity activity, String domain, JSONArray deviceList) {
        CacheByDomain(activity, domain, "device", deviceList);
    }

    static JSONArray RestoreDeviceListFromCache(Activity activity, String domain) {
        return (JSONArray)RestoreFromCacheByDomain(activity, domain, "device");
    }

    static void CachePortalList(Activity activity, String domain, JSONArray deviceList) {
        CacheByDomain(activity, domain, "portal", deviceList);
    }

    static JSONArray RestorePortalListFromCache(Activity activity, String domain) {
        return (JSONArray)RestoreFromCacheByDomain(activity, domain, "portal");
    }

}
