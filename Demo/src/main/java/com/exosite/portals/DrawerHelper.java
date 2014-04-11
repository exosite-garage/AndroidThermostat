package com.exosite.portals;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DrawerHelper {
    private static final String TAG = "DrawerHelper";

    DrawerLayout mDrawerLayout;
    ListView mDrawerList;
    static JSONArray mDomains;

    void setup(final Activity activity) {
        mDrawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) activity.findViewById(R.id.left_drawer);

        List<String> menuOptions = new ArrayList<String>();
        menuOptions.add("Log out");
        final int numOptionsBeforeDomains = menuOptions.size();

        try {
            // get user's domains
            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(activity);
            mDomains = new JSONArray(sharedPreferences.getString("domain_list", "[]"));
            for (int i = 0; i < mDomains.length(); i++) {
                JSONObject domain = (JSONObject)mDomains.get(i);
                menuOptions.add(domain.getString("domain"));
            }
        } catch (JSONException je) {
            Log.e(TAG, je.toString());
        }

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(activity,
                R.layout.drawer_list_item, menuOptions));
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent;
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(activity);

                switch(i) {
                    case 0:
                        // log out
                        sharedPreferences.edit().remove("password").commit();
                        Cache.ClearAllCache(activity);
                        intent = new Intent(activity.getApplicationContext(), LoginActivity.class);
                        activity.startActivity(intent);
                        break;
                    default:
                        try {
                            String domain = mDomains.getJSONObject(
                                    i - numOptionsBeforeDomains).getString("domain");
                            if (activity instanceof DeviceListActivity) {
                                // select domain
                                Helper.selectDomain(
                                        domain,
                                        activity);
                                mDrawerLayout.closeDrawers();
                                ((DeviceListActivity)activity).populateList();
                            } else {
                                Helper.selectDomainAndDoIntent(
                                        domain,
                                        new Intent(activity, DeviceListActivity.class),
                                        activity);
                            }

                        } catch (JSONException je) {
                            Log.e(TAG, je.toString());
                        }
                }
            }
        });
    }
}
