package com.exosite.demo;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.SimpleCursorAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.exosite.onepv1.OneException;
import com.exosite.onepv1.OnePlatformRPC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class SelectDeviceActivity extends ListActivity {

    JSONArray mPortalList;
    ListView mDevices;
    SimpleCursorAdapter mAdapter;
    DatabaseHelper mDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        displayList();

        new LoadDevicesTask().execute();

    }

    private void displayList() {
        mDB = new DatabaseHelper(this);
        mDB.RecreateTable();

        Cursor cursor = mDB.GetAllData();
        String from[] = new String[]{ mDB.colName, mDB.colPortalName };
        int to[] = new int[] { android.R.id.text1, android.R.id.text2 };

        mAdapter = new SimpleCursorAdapter(
                this, android.R.layout.two_line_list_item, cursor, from, to, 0);
        //mDB.close();
        setListAdapter(mAdapter);
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
                        mDB.InsertDevice(
                                rid,
                                info.getJSONObject("description").getString("name"),
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
                Log.e(TAG, "JSONException in ReadPortals.doInBackground" + e.toString());
                return null;
            } catch (OneException e) {
                Log.e(TAG, "OneException: " + e.toString());
                return null;
            }
            return null;
        }

        // this is executed on UI thread when doInBackground
        // returns a result
        protected void onPostExecute(JSONObject portals) {
        }
    }
}
