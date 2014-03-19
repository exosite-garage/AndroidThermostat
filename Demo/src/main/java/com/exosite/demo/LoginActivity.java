package com.exosite.demo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.exosite.portals.Portals;
import com.exosite.portals.PortalsRequestException;
import com.exosite.portals.PortalsResponseException;

import org.json.JSONArray;

import java.util.Random;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {

    enum LoginTask {
        SignIn,
        SignUp
    }

    /**
     * Keep track of the login and password recovery tasks so we can cancel them if requested.
     */
    private UserLoginTask mAuthTask = null;
    private ResetPasswordTask mResetPasswordTask = null;

    // Values for email and password at the time of the login attempt.
    private String mEmail;
    private String mPassword;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmail = "";
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(mEmail); // String.format("danweaver+ti_%d@exosite.com", new Random().nextInt(100000)));

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

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
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
                showProgress(true);
                // Store value at the time of the reset attempt.
                mEmail = mEmailView.getText().toString();
                mResetPasswordTask = new ResetPasswordTask();
                mResetPasswordTask.execute((Void) null);

                return true;
            case R.id.action_sign_up:
                // Show a progress spinner
                mLoginStatusMessageView.setText(R.string.login_progress_signing_up);
                showProgress(true);
                // Store value at the time of the reset attempt.
                mEmail = mEmailView.getText().toString();
                mPassword = mPasswordView.getText().toString();

                mAuthTask = new UserLoginTask();
                mAuthTask.execute(LoginTask.SignUp);

                return true;
        }
        return false;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
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
            mAuthTask = new UserLoginTask();
            mAuthTask.execute(LoginTask.SignIn);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<LoginTask, Void, Boolean> {
        JSONArray mPortalList = null;
        Exception mException;
        LoginTask mTask;

        @Override
        protected Boolean doInBackground(LoginTask... loginTask) {
            mPortalList = null;
            mException = null;
            mTask = loginTask[0];

            Portals p = new Portals();
            p.setDomain(MainActivity.PORTALS_DOMAIN);
            p.setTimeoutSeconds(15);

            try {
                switch(loginTask[0]) {
                    case SignUp:
                        p.SignUp(mEmail, mPassword, MainActivity.PLAN_ID);
                        return true;
                    case SignIn:
                        mPortalList = p.ListPortals(mEmail, mPassword);
                        return true;
                }
            } catch (PortalsRequestException e) {
                mException = e;
                return false;
            } catch (PortalsResponseException e) {
                mException = e;
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(LoginActivity.this);
                sharedPreferences.edit().putString("email", mEmail).commit();
                sharedPreferences.edit().putString("password", mPassword).commit();
                if (mTask == LoginTask.SignIn) {

                    sharedPreferences.edit().putString("portal_list", mPortalList.toString()).commit();

                    Intent intent = new Intent(LoginActivity.this, SelectDeviceActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(getApplicationContext(),
                            String.format("Sent confirmation email to %s", mEmail), Toast.LENGTH_LONG).show();
                }

            } else {
                if (mException instanceof PortalsResponseException) {
                    PortalsResponseException pre = (PortalsResponseException) mException;
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
                            String.format("Unexpected error: %s", mException.getMessage()), Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous task used to request password recovery email.
     */
    public class ResetPasswordTask extends AsyncTask<Void, Void, Boolean> {
        Exception exception;

        @Override
        protected Boolean doInBackground(Void... params) {
            exception = null;
            Portals p = new Portals();
            p.setDomain(MainActivity.PORTALS_DOMAIN);
            p.setTimeoutSeconds(15);
            try {
                p.ResetPassword(mEmail);
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
            mResetPasswordTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(getApplicationContext(),
                        String.format("Password recovery email sent to %s", mEmail), Toast.LENGTH_LONG).show();
                finish();
            } else {
                if (exception instanceof PortalsResponseException) {
                    PortalsResponseException pre = (PortalsResponseException)exception;
                    int code = pre.getResponseCode();
                    Toast.makeText(getApplicationContext(),
                            String.format("Error: %s (%d)",pre.getMessage(), code), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            String.format("Unexpected error: %s",exception.getMessage()), Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mResetPasswordTask = null;
            showProgress(false);
        }
    }
}
