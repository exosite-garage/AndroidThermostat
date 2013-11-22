package com.exosite.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.exosite.onepv1.HttpRPCRequestException;
import com.exosite.onepv1.HttpRPCResponseException;
import com.exosite.onepv1.OnePlatformException;
import com.exosite.onepv1.OnePlatformRPC;
import com.exosite.onepv1.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    // TI device CIK
    static String mCIK;
    // whether to show colors for action
    static boolean mShowActionColor;
    // url where alternative logo may be found
    static String mLogoUrl = "";

    // Device model
    static Device mDevice = new Device();
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    public static final String LOGO_FILENAME = "logo.png";

    private void deleteLogoFile() {
        File dir = getFilesDir();
        File file = new File(dir, LOGO_FILENAME);
        boolean deleted = file.delete();
    }

    private void updateFromSettings(boolean reDownloadLogo) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        mCIK = sharedPreferences.getString(SettingsActivity.KEY_PREF_DEVICE_CIK, "DEFAULT CIK");
        mShowActionColor = sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_ACTION_COLOR, true);
        String url = sharedPreferences.getString(SettingsActivity.KEY_PREF_LOGO_URL, "");
        // if the logo URL is updated, remove the saved bitmap.
        // this will cause a redownload
        if ((reDownloadLogo && url != mLogoUrl) || url.length() == 0) {
            deleteLogoFile();
        }
        mLogoUrl = url;
        PlaceholderFragment fragment = (PlaceholderFragment)getSupportFragmentManager()
                .findFragmentByTag(PlaceholderFragment.FRAGMENT_TAG);
        if (fragment != null) {
            fragment.handleSettingsUpdate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            PlaceholderFragment frag = new PlaceholderFragment();
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            trans.add(R.id.container, frag, PlaceholderFragment.FRAGMENT_TAG).commit();
        } else if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        // set up preferences/settings
        updateFromSettings(false);
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                Log.d(TAG, "called onSharedPreferenceChanged()");
                updateFromSettings(true);
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);

        // remove Exosite from title
        setTitle("");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mDevice.getSetpoint() != null) {
            savedInstanceState.putDouble("setpoint", mDevice.getSetpoint());
        }
        if (mDevice.getTemperature() != null) {
            savedInstanceState.putDouble("temp", mDevice.getTemperature());
        }
    }
    private void restoreInstanceState(Bundle savedInstanceState) {
        mDevice.setSetpoint(savedInstanceState.getDouble("setpoint"));
        mDevice.setTemperature(savedInstanceState.getDouble("temp"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                int RESULT_SETTINGS = 1;
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private static final String TAG = "PlaceholderFragment";
        public static final String FRAGMENT_TAG = "PLACEHOLDER_FRAGMENT";
        ImageView mLogo;
        TextView mTemperature;
        //Spinner mLed;
        SeekBar mSetpoint;
        CompoundButton mSwitch;
        TextView mSetpointText;
        // heat and cool when temp is + or - TOLERANCE from setpoint
        double TOLERANCE = 1.0;
        Handler mReadHandler = new Handler();
        Runnable mReadRunnable;
        String lastToast;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            setRetainInstance(true);
            mLogo = (ImageView)rootView.findViewById(R.id.logo);
            mTemperature = (TextView)rootView.findViewById(R.id.temperature);
            mSetpoint = (SeekBar)rootView.findViewById(R.id.setpoint);
            mSetpoint.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                    if (mDevice.getSetpoint() != null) {
                        mDevice.setSetpointFromPercent(i);
                    }
                    updateWidgets();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mDevice.setWriteInProgress(true);
                    new WriteTask().execute(ALIAS_SETPOINT, String.valueOf(mDevice.getSetpoint()));
                }
            });
            mSetpointText = (TextView)rootView.findViewById(R.id.setpointText);

            mSwitch = (CompoundButton)rootView.findViewById(R.id.switch_control);
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    String value = b ? "1" : "0";
                    mDevice.setWriteInProgress(true);
                    new WriteTask().execute(ALIAS_SWITCH, value);
                }
            });

            // configure to update widgets from platform periodically
            mReadRunnable = new Runnable() {
                @Override
                public void run() {
                    new ReadTask().execute();
                }
            };

            // sync widgets with model
            updateWidgets();

            // load logo from internal storage
            loadLogo(rootView.getContext());

            // start worker thread for reading from OneP
            new ReadTask().execute();
            return rootView;
        }

        void displayError() {
            // show a brief message if it hasn't already been shown
            String err = mDevice.getError();
            if (err != lastToast) {
                if (err.length() > 0) {
                    Context ctx = getView().getContext();
                    Toast.makeText(ctx, err, Toast.LENGTH_LONG).show();
                }
                lastToast = err;
            }
        }

        void updateWidgets() {
            if (getActivity() == null || mTemperature == null || mSetpoint == null) return;
            Double temp = mDevice.getTemperature();
            Double setpoint = mDevice.getSetpoint();
            if (temp == null) {
                mTemperature.setText("--°");
            } else {
                mTemperature.setText(temp + "°");
            }
            if (mShowActionColor && temp != null && setpoint != null) {
                if (temp < setpoint - TOLERANCE) {
                    // too cold
                    mTemperature.setBackgroundColor(getResources().getColor(R.color.warming));
                } else if (setpoint + TOLERANCE < temp) {
                    // too hot
                    mTemperature.setBackgroundColor(getResources().getColor(R.color.cooling));
                } else {
                    // just right
                    mTemperature.setBackgroundColor(getResources().getColor(R.color.off));
                }
            } else {
                mTemperature.setBackgroundColor(getResources().getColor(R.color.appBlue));
            }
            if (setpoint == null) {
                // if setpoint hasn't been set, leave setpoint slider as it is
                mSetpoint.setEnabled(false);
                mSetpointText.setText("");
            } else {
                mSetpoint.setEnabled(true);
                mSetpoint.setProgress(mDevice.getSetpointAsPercent().intValue());
                mSetpointText.setText(String.format("%.0f", setpoint) + "°");
            }

            Integer switchState = mDevice.getSwitch();
            if (switchState == null) {
                mSwitch.setChecked(false);
            } else {
                mSwitch.setChecked(switchState != 0);
            }

        }

        private boolean loadLogo(Context ctx) {
            Bitmap bitmap = null;
            try {
                Log.d(TAG, "File location: " + ctx.getFileStreamPath(LOGO_FILENAME));
                final FileInputStream fis = ctx.openFileInput(LOGO_FILENAME);
                bitmap = BitmapFactory.decodeStream(fis);
                fis.close();
            } catch (FileNotFoundException e) {
                Toast.makeText(ctx, "Logo file not found", Toast.LENGTH_SHORT);
            } catch (IOException e) {
                Toast.makeText(ctx, "IO Exception loading logo", Toast.LENGTH_SHORT);
            }
            if (bitmap != null) {
                mLogo.setImageBitmap(bitmap);
                return true;
            } else {
                return false;
            }
        }

        protected void handleSettingsUpdate() {
            View v = getView();
            if (v != null) {
                Context ctx = v.getContext();

                if (!loadLogo(ctx)) {
                    if (mLogoUrl != null && mLogoUrl.trim().length() != 0) {
                        new DownloadTask(ctx).execute(mLogoUrl);
                    }
                }
            }
        }

        private final String ALIAS_TEMP = "temp";
        private final String ALIAS_SETPOINT = "setpoint";
        private final String ALIAS_SWITCH = "switch";

        class ReadTask extends AsyncTask<Void, Integer, ArrayList<Result>> {
            private static final String TAG = "ReadTask";
            private final String[] aliases = {ALIAS_TEMP, ALIAS_SETPOINT, ALIAS_SWITCH};
            private Exception exception;
            protected ArrayList<Result> doInBackground(Void... params) {
                exception = null;
                // call to OneP
                OnePlatformRPC rpc = new OnePlatformRPC();
                String responseBody = null;
                try {
                    String requestBody = "{\"auth\":{\"cik\":\"" + mCIK
                            + "\"},\"calls\":[";
                    for (String alias: aliases) {
                        requestBody += "{\"id\":\"" + alias + "\",\"procedure\":\"read\","
                            + "\"arguments\":[{\"alias\":\"" + alias + "\"},"
                            + "{\"limit\":1,\"sort\":\"desc\"}]}";
                        if (alias != aliases[aliases.length - 1]) {
                            requestBody += ',';
                        }
                    }
                    requestBody += "]}";
                    Log.v(TAG, requestBody);
                    // do this just to check for JSON parse errors on client side
                    // while debugging. it can be removed for production.
                    JSONObject jo = new JSONObject(requestBody);
                    responseBody = rpc.callRPC(requestBody);

                    Log.v(TAG, responseBody);
                } catch (JSONException e) {
                    this.exception = e;
                    Log.e(TAG, "Caught JSONException before sending request. Message:" + e.getMessage());
                } catch (HttpRPCRequestException e) {
                    this.exception = e;
                    Log.e(TAG, "Caught HttpRPCRequestException " + e.getMessage());
                } catch (HttpRPCResponseException e) {
                    this.exception = e;
                    Log.e(TAG, "Caught HttpRPCResponseException " + e.getMessage());
                }

                if (responseBody != null) {
                    try {
                        ArrayList<Result> results = rpc.parseResponses(responseBody);
                        return results;
                    } catch (OnePlatformException e) {
                        this.exception = e;
                        Log.e(TAG, "Caught OnePlatformException " + e.getMessage());
                    } catch (JSONException e) {
                        this.exception = e;
                        Log.e(TAG, "Caught JSONException " + e.getMessage());
                    }
                }
                return null;
            }

            // this is executed on UI thread when doInBackground
            // returns a result
            protected void onPostExecute(ArrayList<Result> results) {
                boolean hasError = false;
                if (results != null) {
                    for(int i = 0; i < results.size(); i++) {
                        Result result = results.get(i);
                        String alias = aliases[i];
                        if (result.getResult() instanceof JSONArray) {
                            try {
                                JSONArray points = ((JSONArray)result.getResult());
                                if (points.length() > 0) {
                                    JSONArray point = points.getJSONArray(0);
                                    // this will break if results are out of order.
                                    // need to fix OnePlatformRPC.java
                                    if (alias == ALIAS_TEMP) {
                                        mDevice.setTemperature(point.getDouble(1));
                                    } else if (alias == ALIAS_SETPOINT) {
                                        // only set the setpoint once from a read
                                        if (mDevice.getSetpoint() == null) {
                                            mDevice.setSetpoint(point.getDouble(1));
                                        }
                                    } else if (alias == ALIAS_SWITCH) {
                                        mDevice.setSwitchFromCloud(point.getInt(1));
                                    }
                                } else {
                                    hasError = true;
                                    if (alias == ALIAS_TEMP) {
                                        mDevice.setTemperature(null);
                                        mDevice.setError("No temperature values.");
                                    } else if (alias == ALIAS_SETPOINT) {
                                        mDevice.setSetpoint(null);
                                        mDevice.setError("No setpoint value");
                                    } else if (alias == ALIAS_SWITCH) {
                                        mDevice.setSwitchFromCloud(null);
                                        mDevice.setError("No switch value");
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSONException getting the result: " + e.getMessage());
                            }
                        } else {
                            Log.e(TAG, result.getStatus() + ' ' + result.getResult().toString());
                        }
                    }
                    updateWidgets();

                } else {
                    Log.e(TAG, "null result in ReadTask.onPostExecute()");
                    if (this.exception instanceof OnePlatformException) {
                        mDevice.setError("Received error from platform");
                    } else {
                        mDevice.setError("Unable to connect to platform");
                    }
                    hasError = true;
                }
                if (!hasError) {
                    mDevice.setError("");
                } else {
                    displayError();
                }
                mReadHandler.postDelayed(mReadRunnable, 2000);
            }
        }

        class WriteTask extends AsyncTask<String, Integer, ArrayList<Result>> {
            private static final String TAG = "WriteTask";
            private Exception exception = null;
            // pass two values per alias to write -- alias followed by value to write
            // for example "foo", "1", "bar", "2"
            protected ArrayList<Result> doInBackground(String... values) {
                assert(values.length % 2 == 0);
                OnePlatformRPC rpc = new OnePlatformRPC();
                String responseBody = null;
                try {
                    String requestBody = "{\"auth\":{\"cik\":\"" + mCIK
                            + "\"},\"calls\":[";
                    for (int i = 0; i < values.length; i += 2) {
                        String alias = values[i];
                        requestBody += "{\"id\":\"" + alias + "\",\"procedure\":\"write\","
                                + "\"arguments\":[{\"alias\":\"" + alias + "\"},"
                                + "\"" + values[i + 1] + "\"]}";
                        // are we pointing to the last alias?
                        if (i != values.length - 2) {
                            requestBody += ',';
                        }
                    }
                    requestBody += "]}";
                    Log.d(TAG, requestBody);
                    // do this just to check for JSON parse errors on client side
                    // while debugging. it can be removed for production.
                    JSONObject jo = new JSONObject(requestBody);
                    responseBody = rpc.callRPC(requestBody);

                    Log.d(TAG, responseBody);
                } catch (JSONException e) {
                    this.exception = e;
                    Log.e(TAG, "Caught JSONException before sending request. Message:" + e.getMessage());
                } catch (HttpRPCRequestException e) {
                    this.exception = e;
                    Log.e(TAG, "Caught HttpRPCRequestException " + e.getMessage());
                } catch (HttpRPCResponseException e) {
                    this.exception = e;
                    Log.e(TAG, "Caught HttpRPCResponseException " + e.getMessage());
                }

                if (responseBody != null) {
                    try {
                        ArrayList<Result> results = rpc.parseResponses(responseBody);
                        return results;
                    } catch (OnePlatformException e) {
                        this.exception = e;
                        Log.e(TAG, "Caught OnePlatformException " + e.getMessage());
                    } catch (JSONException e) {
                        this.exception = e;
                        Log.e(TAG, "Caught JSONException " + e.getMessage());
                    }
                }
                return null;
            }

            // this is executed on UI thread when doInBackground
            // returns a result
            protected void onPostExecute(ArrayList<Result> results) {
                mDevice.setWriteInProgress(false);
            }
        }

        class DownloadTask extends AsyncTask<String, Void, Bitmap> {
            String errorMessage;
            Context ctx;
            public DownloadTask(Context ctx) {
                this.ctx = ctx;
            }
            // pass one value per entry in aliases above
            protected Bitmap doInBackground(String... urls) {
                String logoUrl = urls[0];
                Log.d(TAG, "logoUrl " + logoUrl);
                Bitmap bitmap = null;
                errorMessage = "";
                try {
                    URL url = new URL(logoUrl.trim());
                    URLConnection urlConnection = url.openConnection();
                    InputStream inputStream = urlConnection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                } catch (MalformedURLException e) {
                    errorMessage = "Invalid image URL";
                } catch (IOException e) {
                    errorMessage = "IO Exception";
                }
                if (errorMessage.length() > 0) {
                    if (mLogoUrl.trim().length() > 0) {
                        Log.e(TAG, errorMessage);
                    }
                }
                return bitmap;
            }
            // this is executed on UI thread when doInBackground
            // returns a result
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null || errorMessage != "") {
                    Toast.makeText(ctx, "Error downloading logo bitmap:" + errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    try {
                        // save file locally
                        final FileOutputStream fos = ctx.openFileOutput(LOGO_FILENAME, Context.MODE_PRIVATE);
                        Log.d(TAG, "File location: " + ctx.getFileStreamPath(LOGO_FILENAME));
                        if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)) {
                            Toast.makeText(ctx, "Logo saved successfully", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ctx, "Failed to save logo", Toast.LENGTH_SHORT).show();
                        }
                        loadLogo(ctx);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(ctx, "File not found when saving logo file", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

}
