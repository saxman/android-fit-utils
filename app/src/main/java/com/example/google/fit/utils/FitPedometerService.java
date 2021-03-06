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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;

import java.util.concurrent.TimeUnit;

public class FitPedometerService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnDataPointListener {

    private static final String LOG_TAG = FitPedometerService.class.getSimpleName();

    private static final int PEDOMETER_SAMPLE_RATE_MS = 100;

    protected GoogleApiClient mClient = null;

    public FitPedometerService() {
        super(FitPedometerService.class.getSimpleName());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        mClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Fitness.SENSORS_API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mClient.isConnected()) {
            mClient.disconnect();
        }
    }

    @Override
    public void onHandleIntent(Intent intent) {
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "Connection to Google Fit succeeded. Registering sensor listener.");

        SensorRequest sensorRequest = new SensorRequest.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setSamplingRate(PEDOMETER_SAMPLE_RATE_MS, TimeUnit.MILLISECONDS)
                .build();

        PendingResult<Status> pendingResult = Fitness.SensorsApi.add(mClient, sensorRequest, this);

        pendingResult.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(LOG_TAG, "Google Fit steps listener registered.");
                } else {
                    Log.d(LOG_TAG, "Google Fit steps listener not registered. Cause: " + status.toString());

                    if (status.hasResolution()) {
                        try {
                            Log.d(LOG_TAG, "Google Fit steps listener not registered. Attempting to resolve.");
                            status.getResolution().send();
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(LOG_TAG, "Exception while attempting to resolve failed attempt to register Google Fit step listener.", e);
                        }
                    } else {
                        Log.e(LOG_TAG, "No resolution to failed attempt to register sensor listener with Google Fit.");
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(LOG_TAG, "Connection to Google Fit failed. Cause: " + result.toString());

        if (result.hasResolution()) {
            Log.e(LOG_TAG, "Attempting to resolve failed connection to Google Fit.");

            try {
                result.getResolution().send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(LOG_TAG, "Exception while attempting to resolve failed connection to Google Fit.");
            }
        } else {
            Log.w(LOG_TAG, "Could not resolve failed connection to Google Fit. Shutting down.");
            stopSelf();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(LOG_TAG, "Connection to Google Fit suspended.");
    }

    @Override
    public void onDataPoint(DataPoint dataPoint) {
        Log.i(LOG_TAG, "Steps delta: " + dataPoint.getValue(Field.FIELD_STEPS) + " at " + dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
    }
}
