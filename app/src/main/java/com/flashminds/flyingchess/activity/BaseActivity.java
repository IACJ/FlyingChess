package com.flashminds.flyingchess.activity;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by IACJ on 2018/4/24.
 */

public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "进入活动："+getClass().getSimpleName());
    }
}
