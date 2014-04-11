package com.exosite.portals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.exosite.api.portals.Portals;
import com.exosite.api.ExoCallback;
import com.exosite.api.ExoException;
import com.exosite.api.portals.PortalsResponseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddDeviceActivity extends FormActivity {
    static JSONArray mPortalList;
    static final String TAG = "AddDeviceActivity";
    EditText mNameEditText;
    EditText mSerialNumberEditText;
    EditText mVendorEditText;
    EditText mModelEditText;
    Spinner mPortalSpinner;
    Button mAddDeviceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(AddDeviceActivity.this);
        String domain = sharedPreferences.getString("domain", "");
        mPortalList = Cache.RestorePortalListFromCache(this, domain);
        if (mPortalList == null) {
            Log.e(TAG, "no portal list found in cache");
        }

        this.setTitle("Add Device");

        mNameEditText = (EditText)findViewById(R.id.device_name);
        mNameEditText.setText("Device Name");
        mSerialNumberEditText = (EditText)findViewById(R.id.device_serial_number);
        mSerialNumberEditText.setText(String.format("123123%06x", new Random().nextInt(0xffffff)));
        mVendorEditText = (EditText)findViewById(R.id.device_vendor);
        mVendorEditText.setText(MainActivity.VENDOR);
        mModelEditText = (EditText)findViewById(R.id.device_model);
        mModelEditText.setText(MainActivity.DEVICE_MODEL);

        // populate spinner
        mPortalSpinner = (Spinner)findViewById(R.id.device_portal);
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
                AddDeviceActivity.this,
                android.R.layout.simple_spinner_item,
                SpinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPortalSpinner.setAdapter(adapter);

        mAddDeviceButton = (Button)findViewById(R.id.add_device_button);
        mAddDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String portalRID = mPortalList.getJSONObject(mPortalSpinner.getSelectedItemPosition()).getString("rid");
                    String vendor = mVendorEditText.getText().toString();
                    String model = mModelEditText.getText().toString();
                    String sn = mSerialNumberEditText.getText().toString();
                    String name = mNameEditText.getText().toString();
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext());
                    String email = sharedPreferences.getString("email", null);
                    String password = sharedPreferences.getString("password", null);

                    showProgress(true);
                    Portals.addDeviceInBackground(portalRID, vendor, model, sn, name, email, password, new ExoCallback<JSONObject>() {
                        @Override
                        public void done(JSONObject newDevice, ExoException e) {
                            showProgress(false);
                            if (newDevice != null) {
                                try {
                                    Toast.makeText(getApplicationContext(),
                                            String.format("Device created with CIK %s...",
                                                    newDevice.getString("cik").substring(0, 8)),
                                            Toast.LENGTH_LONG).show();

                                    Intent intent = new Intent(AddDeviceActivity.this, DeviceListActivity.class);
                                    startActivity(intent);
                                    finish();
                                } catch (JSONException je) {
                                    Log.e(TAG, "Error getting CIK from created device: " + je.toString());
                                }
                            } else {
                                if (e instanceof PortalsResponseException) {
                                    PortalsResponseException pre = (PortalsResponseException)e;
                                    JSONObject errObj;
                                    try {
                                        errObj = new JSONObject(pre.getResponseBody());
                                        if (errObj != null) {
                                            Toast.makeText(getApplicationContext(),
                                                    String.format("Device not created. Reason: %s", errObj.getJSONArray("errors").getString(0)),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    } catch (JSONException je) {
                                        int code = pre.getResponseCode();
                                        Toast.makeText(getApplicationContext(),
                                                String.format("Error: %s (%d) caused JSONException %s",pre.getMessage(), code, je.getMessage()), Toast.LENGTH_LONG).show();
                                    }


                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            String.format("Unexpected error: %s", e.getMessage()), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "Exception while handling add device click: " + e.toString());
                }
                //
            }
        });

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

}
