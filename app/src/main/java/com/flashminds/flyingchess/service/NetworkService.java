package com.flashminds.flyingchess.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class NetworkService extends Service {
    private static final String TAG = "NetworkService";
    public NetworkService() {
    }
    private NetworkBiner mBiner=new NetworkBiner();
    class NetworkBiner extends Binder{

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBiner;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
