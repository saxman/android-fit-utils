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

public class PedometerService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnDataPointListener {

    private static final String LOG_TAG = PedometerService.class.getSimpleName();

    protected GoogleApiClient mClient = null;

    public PedometerService() {
        super("FitPedometerService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind(Intent)");

        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate()");

        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(LOG_TAG, "onStart()");

        super.onStart(intent, startId);

        String accountName = intent.getStringExtra(GoogleApiClientActivity.ACCOUNT_NAME_EXTRA_KEY);
        mClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .setAccountName(accountName)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO need to unregister the listener?

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

        // TODO best way to get real-time updates?
        SensorRequest sensorRequest = new SensorRequest.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setSamplingRate(100, TimeUnit.MILLISECONDS)
                .build();

        PendingResult<Status> pendingResult = Fitness.SensorsApi.add(mClient, sensorRequest, this);

        pendingResult.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                Log.d(LOG_TAG, "Status from registering sensor listener: " + status);

                if (status.isSuccess()) {
                    Log.d(LOG_TAG, "Listener registered!");
                } else {
                    Log.d(LOG_TAG, "Listener not registered. Cause: " + status.toString());
                    if (status.hasResolution()) {
                        try {
                            Log.d(LOG_TAG, "Resolving...");
                            status.getResolution().send();
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(LOG_TAG, "Exception while attempting to resolve failed connection to Google Fit.");
                        }
                    } else {
                        Log.w(LOG_TAG, "Could not resolve failed connection to Google Fit while registering sensor listener.");
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
            Log.w(LOG_TAG, "Could not resolve failed connection to Google Fit.");
            // TODO shutdown the service?
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(LOG_TAG, "Connection to Google Fit suspended.");
    }

    @Override
    public void onDataPoint(DataPoint dataPoint) {
        Log.i(LOG_TAG, "Steps delta: " + dataPoint.getValue(Field.FIELD_STEPS));
    }
}
