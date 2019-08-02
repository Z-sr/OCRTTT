package com.example.ocrttt;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.regions.Region;

public class MyApplication extends Application {
    private final String TAG=this.getClass().getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();
//        AWSConfiguration awsConfiguration = new AWSConfiguration();
//        AWSMobileClient.getInstance()
//                .initialize(getApplicationContext(),awsConfiguration, new Callback<UserStateDetails>() {
//            @Override
//            public void onResult(UserStateDetails userStateDetails) {
//                Log.i(TAG, "AWSMobileClient initialized. User State is " + userStateDetails.getUserState());
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Log.e(TAG, "Initialization error.", e);
//            }
//        });
//
//        startService(new Intent(getApplicationContext(), TransferService.class));

    }
}
