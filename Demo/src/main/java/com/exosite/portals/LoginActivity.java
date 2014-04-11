package com.exosite.portals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.exosite.api.portals.Portals;
import com.exosite.api.ExoCallback;
import com.exosite.api.ExoException;
import com.exosite.api.portals.PortalsResponseException;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends FormActivity {
    private static final String TAG = "LoginActivity";

    /**
     * Keep track of the login and password recovery tasks so we can cancel them if requested.
     */

    private boolean mInProgress;

    // Values for email and password at the time of the login attempt.
    private String mEmail;
    private String mPassword;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private TextView mLoginStatusMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(LoginActivity.this);
        mEmail = sharedPreferences.getString("email", "");
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(mEmail);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        if (mEmail.length() > 0) {
            // if email is already entered, put cursor in password field
            mPasswordView.requestFocus();
            int position = mPasswordView.length();
            Editable etext = mPasswordView.getText();
            Selection.setSelection(etext, position);
        }

        // fix password field font
        mPasswordView.setTypeface(Typeface.DEFAULT);
        mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

        setTitle("Portals for Android");
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_forgot_password:
                // Show a progress spinner, and kick off a background task to
                // perform the password recovery email attempt.
                mLoginStatusMessageView.setText(R.string.login_progress_recovering_password);

                // Store value at the time of the reset attempt.
                mEmail = mEmailView.getText().toString();

                showProgress(true);
                Portals.resetPasswordInBackground(mEmail, new ExoCallback<Void>() {
                    @Override
                    public void done(Void result, ExoException e) {
                        mInProgress = false;
                        showProgress(false);
                        if (e == null) {
                            Toast.makeText(getApplicationContext(),
                                    String.format("Password recovery email sent to %s", mEmail), Toast.LENGTH_LONG).show();
                        } else {
                            reportExoException(e);
                        }
                    }
                });

                return true;

            /* TODO: add an API for enumerating available plans
            case R.id.action_sign_up:
                // Show a progress spinner
                mLoginStatusMessageView.setText(R.string.login_progress_signing_up);

                // Store value at the time of the reset attempt.
                mEmail = mEmailView.getText().toString();
                mPassword = mPasswordView.getText().toString();

                showProgress(true);
                Portals.signUpInBackground(mEmail, mPassword, MainActivity.PLAN_ID, new ExoCallback<Void>() {
                    @Override
                    public void done(Void result, ExoException e) {
                        mInProgress = false;
                        showProgress(false);
                        if (result != null) {
                            Toast.makeText(getApplicationContext(),
                                    String.format("Sent confirmation email to %s", mEmail), Toast.LENGTH_LONG).show();
                        } else {
                            reportExoException(e);
                        }
                    }
                });

                showProgress(false);
                return true;
                */
        }
        return false;
    }

    public void reportExoException(Exception e) {
        if (e instanceof PortalsResponseException) {
            PortalsResponseException pre = (PortalsResponseException) e;
            int code = pre.getResponseCode();
            if (code == 401) {
                Toast.makeText(getApplicationContext(),
                        String.format(getString(R.string.error_unauthorized)), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        String.format("Error: %s (%d)",pre.getMessage(), code), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    String.format("Unexpected error: %s", e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mInProgress) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (mPassword.length() < 6) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!mEmail.contains("@")) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mInProgress = true;

            Portals.listDomainsInBackground(mEmail, mPassword, new ExoCallback<JSONArray>() {
                @Override
                public void done(JSONArray result, ExoException e) {
                    mInProgress = false;
                    showProgress(false);

                    if (result != null) {
                        SharedPreferences sharedPreferences = PreferenceManager
                                .getDefaultSharedPreferences(LoginActivity.this);
                        sharedPreferences.edit().putString("email", mEmail).commit();
                        sharedPreferences.edit().putString("password", mPassword).commit();
                        sharedPreferences.edit().putString("domain_list", result.toString()).commit();

                        // select a domain
                        try {
                            // for now just default to the first domain
                            String defaultDomain = result.getJSONObject(0).getString("domain");
                            Intent intent = new Intent(LoginActivity.this, DeviceListActivity.class);
                            Helper.selectDomainAndDoIntent(
                                    defaultDomain,
                                    intent,
                                    LoginActivity.this);
                            finish();

                        } catch (JSONException je) {
                            reportExoException(e);
                        }

                    } else {
                        reportExoException(e);
                    }
                }
            });

        }
    }
}
