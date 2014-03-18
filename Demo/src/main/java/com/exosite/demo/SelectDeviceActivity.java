package com.exosite.demo;

import android.app.ListActivity;
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

import com.exosite.onepv1.OneException;
import com.exosite.onepv1.OnePlatformRPC;

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

        new LoadDevicesTask().execute();

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
        int cikIdx = c.getColumnIndex(DatabaseHelper.colCIK);
        String cik = c.getString(cikIdx);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(SettingsActivity.KEY_PREF_DEVICE_CIK, cik).commit();

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Represents a task that can load information about devices
    // from OneP on a background thread.
    class LoadDevicesTask extends AsyncTask<Void, Integer, JSONObject> {
        private static final String TAG = "ReadTask";
        private Exception exception;
        protected JSONObject doInBackground(Void... params) {
            Bundle bundle = getIntent().getExtras();

            OnePlatformRPC rpc = new OnePlatformRPC();
            exception = null;
            try {
                mPortalList = new JSONArray(bundle.getString("portal_list"));
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
                        Log.e(TAG, String.format("Inserting device %s...", info.getString("key")));
                        mDB.InsertDevice(
                                rid,
                                String.format("%s (%s...)",
                                        info.getJSONObject("description").getString("name"),
                                        info.getString("key").substring(0, 8)),
                                info.getString("key"),
                                portal.getString("rid"),
                                portal.getString("name"),
                                portal.getString("key"));
                    }

                    // TODO: combine results from multiple portals
                    // and return them outside the loop.

                    return clientsInfoListing;
                }
            } catch (JSONException e) {
                exception = e;
                Log.e(TAG, "JSONException in ReadPortals.doInBackground: " + e.toString());
                return null;
            } catch (OneException e) {
                exception = e;
                Log.e(TAG, "OneException: " + e.toString());
                return null;
            }
            return null;
        }

        // this is executed on UI thread when doInBackground
        // returns a result
        protected void onPostExecute(JSONObject portals) {
            if (exception == null) {
                Cursor cursor = mDB.GetAllData();
                mAdapter.changeCursor(cursor);
                mAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getApplicationContext(),
                        String.format("Error fetching devices: %s", exception.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
    }
}
