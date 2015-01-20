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

import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;

import java.util.concurrent.TimeUnit;

public class SettingsActivity extends GoogleApiClientActivity {

    public static final String LOG_TAG = SettingsActivity.class.getSimpleName();

    /**
     * Flag to track if this is the initial Fit connection check, to determine the state of the
     * switch when the activity is started, or if the switch state is already know so the
     * connection should be completed (i.e. user authorization, if needed).
     */
    private boolean initialFitConnectionCheck = true;

    private SwitchCompat fitSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fitSwitch = (SwitchCompat) findViewById(R.id.fit_switch);

        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        // Reset the status if orientation change or activity re-start.
        initialFitConnectionCheck = true;
        super.onStart();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (initialFitConnectionCheck) {
            fitSwitch.setChecked(true);
            initialFitConnectionCheck = false;
            return;
        }

        Toast toast = Toast.makeText(getApplicationContext(), "Connected to Google Fit.", Toast.LENGTH_LONG);
        toast.show();

        // TODO If necessary, add additional code here for accessing Google Fit here.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!authInProgress && initialFitConnectionCheck) {
            fitSwitch.setChecked(false);
            initialFitConnectionCheck = false;
        } else {
            handleAuthFailure(result);
        }
    }

    @Override
    protected void handleAuthCancelled() {
        Log.d(LOG_TAG, "Connection to Google Fit cancelled.");

        // Reset the switch since the use cancelled auth.
        fitSwitch.setChecked(false);

        Toast toast = Toast.makeText(getApplicationContext(), "Connection to Google Fit cancelled.", Toast.LENGTH_LONG);
        toast.show();

        // TODO figure out how to allow the user to select a different account if they cancel at Fit authorization dialog
    }

    public void onFitSwitchClicked(View view) {
        if (((CompoundButton) view).isChecked()) {
            mClient.connect();
        } else {
            PendingResult<Status> pendingResult = Fitness.ConfigApi.disableFit(mClient);
            pendingResult.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Disconnected from Google Fit.", Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        Log.e(LOG_TAG, "Unable to disconnect from Google Fit. " + status.toString());

                        // Re-set the switch since auth failed.
                        fitSwitch.setChecked(true);

                        Toast toast = Toast.makeText(getApplicationContext(), "Unable to disconnect from Google Fit. See logcat for details.", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            }, 5, TimeUnit.SECONDS);
        }
    }
}
