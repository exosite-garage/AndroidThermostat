package com.exosite.demo;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.exosite.api.onep.OneException;
import com.exosite.api.onep.OnePlatformRPC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class SelectDeviceActivity extends ListActivity {

    JSONArray mPortalList;
    SimpleCursorAdapter mAdapter;
    DatabaseHelper mDB;
    Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        mDB = new DatabaseHelper(this);
        mDB.RecreateTable();
        setupAdapter();

        new LoadDevicesTask(getListView().getContext()).execute();
    }

    private void setupAdapter() {
        Cursor mCursor = mDB.GetAllData();
        String from[] = new String[]{ mDB.colName, mDB.colPortalName };
        int to[] = new int[] { android.R.id.text1, android.R.id.text2 };
        mAdapter = new SimpleCursorAdapter(
                this, android.R.layout.two_line_list_item, mCursor, from, to, 0);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        Object o = this.getListAdapter().getItem(position);
        SQLiteCursor c = (SQLiteCursor)o;
        String cik = c.getString(c.getColumnIndex(DatabaseHelper.colCIK));
        String name = c.getString(c.getColumnIndex(DatabaseHelper.colName));

        // select a device to use in the Thermostat demo
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(SettingsActivity.KEY_PREF_DEVICE_CIK, cik).commit();
        sharedPreferences.edit().putString(SettingsActivity.KEY_PREF_DEVICE_NAME, name).commit();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_device) {
            Intent intent = new Intent(this, AddDeviceActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Represents a task that loads information about devices
    // from OneP on a background thread.
    class LoadDevicesTask extends AsyncTask<Void, Integer, JSONObject> {
        private static final String TAG = "LoadDevicesTask";
        private Exception exception;
        Context mCtx;
        public LoadDevicesTask(Context ctx) {
            mCtx = ctx;
        }

        protected JSONObject doInBackground(Void... params) {
            Bundle bundle = getIntent().getExtras();
            // maps portals RIDs to info listing for each portal
            JSONObject response = new JSONObject();
            OnePlatformRPC rpc = new OnePlatformRPC();
            exception = null;
            try {
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(SelectDeviceActivity.this);

                mPortalList = new JSONArray(sharedPreferences.getString("portal_list", "[]"));
                JSONObject infoOptions = new JSONObject();
                infoOptions.put("description", true);
                infoOptions.put("key", true);
                for (int i = 0; i < mPortalList.length(); i++) {
                    JSONObject portal = (JSONObject)mPortalList.get(i);
                    String cik = portal.getString("key");
                    JSONArray types = new JSONArray();
                    types.put("client");
                    JSONObject infoListing = rpc.infoListing(cik, types, infoOptions);

                    JSONObject clientsInfoListing = infoListing.getJSONObject("client");

                    Iterator<String> iter = clientsInfoListing.keys();
                    while (iter.hasNext()) {
                        String rid = iter.next();
                        JSONObject info = clientsInfoListing.getJSONObject(rid);
                        mDB.InsertDevice(
                                rid,
                                String.format("%s (%s...)",
                                        info.getJSONObject("description").getString("name"),
                                        info.getString("key").substring(0, 8)),
                                info.getString("key"),
                                portal.getString("rid"),
                                String.format("Portal: %s", portal.getString("name")),
                                portal.getString("key"));
                    }
                    response.put(portal.getString("rid"), clientsInfoListing);
                }
                return response;

            } catch (JSONException e) {
                exception = e;
                Log.e(TAG, "JSONException in ReadPortals.doInBackground: " + e.toString());
            } catch (OneException e) {
                exception = e;
                Log.e(TAG, "OneException: " + e.toString());
            }
            return null;
        }

        // this is executed on UI thread when doInBackground
        // returns a result
        protected void onPostExecute(JSONObject infoListing) {
            if (exception == null) {
                Cursor cursor = mDB.GetAllData();
                mAdapter.changeCursor(cursor);
                mAdapter.notifyDataSetChanged();

                // cache the info listing so that we can display the list when offline
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(mCtx);
                sharedPreferences.edit().putString("info_listing", infoListing.toString()).commit();

            } else {
                Toast.makeText(getApplicationContext(),
                        String.format("Error fetching devices: %s", exception.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
    }


}
