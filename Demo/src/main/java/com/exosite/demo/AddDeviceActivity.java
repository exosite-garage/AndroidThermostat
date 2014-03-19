package com.exosite.demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.exosite.portals.Portals;
import com.exosite.portals.PortalsRequestException;
import com.exosite.portals.PortalsResponseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddDeviceActivity extends ActionBarActivity {
    static JSONArray mPortalList;
    static final String TAG = "AddDeviceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(AddDeviceActivity.this);

        try {
            mPortalList = new JSONArray(sharedPreferences.getString("portal_list", "[]"));
        } catch (JSONException e) {
            Log.e(TAG, "portal_list shared preference was not set.");
        }

        this.setTitle("Add Device");

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.add_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        static AddDeviceTask mAddDeviceTask;
        EditText mNameEditText;
        EditText mSerialNumberEditText;
        Spinner mPortalSpinner;
        Button mAddDeviceButton;

        public PlaceholderFragment() {
        }

        void showProgress(boolean enabled) {
            // TODO: show progress spinner
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_add_device, container, false);

            mNameEditText = (EditText)rootView.findViewById(R.id.device_name);
            mNameEditText.setText("Device Name");
            mSerialNumberEditText = (EditText)rootView.findViewById(R.id.device_serial_number);
            mSerialNumberEditText.setText(String.format("123123%06x", new Random().nextInt(0xffffff)));

            // populate spinner
            mPortalSpinner = (Spinner)rootView.findViewById(R.id.device_portal);
            List<String> SpinnerArray = new ArrayList<String>();
            try {
                for (int i = 0; i < mPortalList.length(); i++) {
                    JSONObject portal = mPortalList.getJSONObject(i);
                    SpinnerArray.add(String.format("%s (CIK: %s...)",
                            portal.getString("name"),
                            portal.getString("key").substring(0,8)));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Exception while populating spinner: " + e.toString());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_spinner_item,
                    SpinnerArray);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPortalSpinner.setAdapter(adapter);

            mAddDeviceButton = (Button)rootView.findViewById(R.id.add_device_button);
            mAddDeviceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        String portalRID = mPortalList.getJSONObject(mPortalSpinner.getSelectedItemPosition()).getString("rid");
                        String vendor = MainActivity.VENDOR;
                        String model = MainActivity.DEVICE_MODEL;
                        String sn = mSerialNumberEditText.getText().toString();
                        String name = mNameEditText.getText().toString();

                        mAddDeviceTask = new AddDeviceTask(
                                (AddDeviceActivity)getActivity(),
                                getActivity().getApplicationContext());
                        mAddDeviceTask.execute(portalRID, vendor, model, sn, name);

                    } catch (JSONException e) {
                        Log.e(TAG, "Exception while handling add device click: " + e.toString());
                    }
                    //
                }
            });

            return rootView;
        }

        /**
         * Represents an asynchronous task to add a device.
         */
        public class AddDeviceTask extends AsyncTask<String, Void, Boolean> {
            Exception exception;
            JSONObject mNewDevice;

            AddDeviceActivity mActivity;
            Context mCtx;
            public AddDeviceTask(AddDeviceActivity activity, Context ctx) {
                mCtx = ctx;
                mActivity = activity;
            }

            @Override
            protected Boolean doInBackground(String... params) {
                String portalRID = params[0];
                String vendor = params[1];
                String model = params[2];
                String sn = params[3];
                String name = params[4];

                mNewDevice = null;
                exception = null;
                Portals p = new Portals();
                p.setDomain(MainActivity.PORTALS_DOMAIN);
                p.setTimeoutSeconds(15);
                try {
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(mActivity.getApplicationContext());
                    String email = sharedPreferences.getString("email", null);
                    String password = sharedPreferences.getString("password", null);

                    mNewDevice = p.addDevice(
                            portalRID,
                            vendor,
                            model,
                            sn,
                            name,
                            email,
                            password);

                } catch (PortalsRequestException e) {
                    exception = e;
                    return false;
                } catch (PortalsResponseException e) {
                    exception = e;
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(final Boolean success) {
                mAddDeviceTask = null;
                showProgress(false);

                if (success) {
                    try {
                        Toast.makeText(mCtx,
                                String.format("Device created with CIK %s...",
                                    mNewDevice.getString("cik").substring(0, 8)),
                                Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(mActivity, SelectDeviceActivity.class);
                        startActivity(intent);
                        mActivity.finish();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error getting cik: " + e.toString());
                    }
                } else {
                    if (exception instanceof PortalsResponseException) {
                        PortalsResponseException pre = (PortalsResponseException)exception;
                        JSONObject errObj;
                        try {
                            errObj = new JSONObject(pre.getResponseBody());
                            if (errObj != null) {
                                Toast.makeText(mCtx,
                                        String.format("Device not created. Reason: %s", errObj.getJSONArray("errors").getString(0)),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            int code = pre.getResponseCode();
                            Toast.makeText(mCtx,
                                    String.format("Error: %s (%d)",pre.getMessage(), code), Toast.LENGTH_LONG).show();
                        }


                    } else {
                        Toast.makeText(mCtx,
                                String.format("Unexpected error: %s",exception.getMessage()), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            protected void onCancelled() {
                mAddDeviceTask = null;
                showProgress(false);
            }
        }
    }

}
