package com.exosite.portals;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

/* Activity that shows a form and progress animation the form is submitted. */
public class FormActivity extends Activity {
    protected View mFormView;
    protected View mStatusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    protected void showProgress(final boolean show) {
        if (mFormView == null) {
            mFormView = findViewById(R.id.form);
        }
        if (mStatusView == null) {
            mStatusView = findViewById(R.id.status);
        }
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mStatusView.setVisibility(View.VISIBLE);
            mStatusView.animate()
            .setDuration(shortAnimTime)
            .alpha(show ? 1 : 0)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

            mFormView.setVisibility(View.VISIBLE);
            mFormView.animate()
            .setDuration(shortAnimTime)
            .alpha(show ? 0 : 1)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}
