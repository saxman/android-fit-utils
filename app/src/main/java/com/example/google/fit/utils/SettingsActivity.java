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

        mClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ)) // required by FitPedometerService
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ)) // required by FitPedometerService
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        // Reset the status if orientation change or activity re-start, to check the current state
        // of the Fit connection.
        initialFitConnectionCheck = true;

        super.onStart();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "Connected to Google Fit.");

        if (initialFitConnectionCheck) {
            fitSwitch.setChecked(true);
            initialFitConnectionCheck = false;
            return;
        }

        Toast.makeText(this, "Connected to Google Fit.", Toast.LENGTH_LONG).show();

        // TODO if necessary, add additional code here for accessing Google Fit here.
        Intent intent = new Intent(this, PedometerService.class);
        intent.putExtra(ACCOUNT_NAME_EXTRA_KEY, accountName);
        startService(intent);
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

        Toast.makeText(this, "Connection to Google Fit cancelled.", Toast.LENGTH_LONG).show();

        // TODO figure out how to allow the user to select a different account if they cancel at Fit authorization dialog. use mClient.clearDefaultAccountAndReconnect()?
    }

    public void onFitSwitchClicked(View view) {
        // TODO disable switch until connection completed or fails.

        if (((CompoundButton) view).isChecked()) {
            mClient.connect();
        } else {
            PendingResult<Status> pendingResult = Fitness.ConfigApi.disableFit(mClient);
            pendingResult.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Toast.makeText(SettingsActivity.this, "Disconnected from Google Fit.", Toast.LENGTH_LONG).show();

                        // TODO if necessary, add Fit disconnect code here
                        stopService(new Intent(SettingsActivity.this, PedometerService.class));
                    } else {
                        Log.e(LOG_TAG, "Unable to disconnect from Google Fit. " + status.toString());

                        // Re-set the switch since auth failed.
                        fitSwitch.setChecked(true);

                        Toast.makeText(SettingsActivity.this, "Unable to disconnect from Google Fit. See logcat for details.", Toast.LENGTH_LONG).show();
                    }
                }
            }, 5, TimeUnit.SECONDS);
        }
    }
}