/*
 * Copyright (C) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.google.fit.utils;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

public abstract class GoogleApiClientActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String LOG_TAG = GoogleApiClientActivity.class.getSimpleName();

    private static final int REQUEST_OAUTH = 1;

    /**
     * Track whether an authorization activity is stacking over the current activity, i.e. when
     * a known auth error is being resolved, such as showing the account chooser or presenting a
     * consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    protected boolean authInProgress = false;

    /**
     * GoogleApiClient which can be used by a subclass to access Google services. This variable
     * must be initialized by the subclass in onCreate.
     */
    protected GoogleApiClient mGoogleApiClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(LOG_TAG, "Connecting to GoogleApiClient.");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            } else {
                // The user cancelled auth. Allow the subclass to handle updating the UI, etc.
                handleAuthCancelled();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    @Override
    public abstract void onConnected(Bundle bundle);

    @Override
    public abstract void onConnectionFailed(ConnectionResult result);

    @Override
    public void onConnectionSuspended(int i) {
        // TODO move to subclass so that we can disable UI components, etc., in the event that the service is inaccessible

        // If your connection to the client gets lost at some point,
        // you'll be able to determine the reason and react to it here.
        if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
            Log.w(LOG_TAG, "GoogleApiClient connection lost. Reason: Network lost.");
        } else if (i == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Log.w(LOG_TAG, "GoogleApiClient connection lost. Reason: Service disconnected");
        }
    }

    protected abstract void handleAuthCancelled();

    protected final void handleAuthFailure(ConnectionResult result) {
        Log.i(LOG_TAG, "GoogleApiClient connection failed. Reason: " + result.toString());

        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }

        // The failure has a resolution, so attempt to resolve it.
        // Called typically when the app is not yet authorized, and an authorization dialog is displayed to the user.
        if (!authInProgress) {
            try {
                Log.i(LOG_TAG, "Attempting to resolve failed GoogleApiClient connection");
                authInProgress = true;
                result.startResolutionForResult(this, REQUEST_OAUTH);
            } catch (IntentSender.SendIntentException e) {
                Log.e(LOG_TAG, "Exception while starting resolution activity", e);
            }
        }
    }
}
