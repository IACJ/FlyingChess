package com.flashminds.flyingchess.manager;


import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.appcompat.BuildConfig;

import android.widget.Toast;

import com.flashminds.flyingchess.Handler.CrashHandler;
import com.flashminds.flyingchess.service.CallbackInterface;
import com.flashminds.flyingchess.service.GameServiceProfile;
import com.flashminds.flyingchess.service.NetworkServiceProxy;
import com.google.gson.JsonObject;
import com.tencent.mars.BaseEvent;
import com.tencent.mars.Mars;
import com.tencent.mars.app.AppLogic;
import com.tencent.mars.sample.wrapper.remote.JsonMarsTaskWrapper;
import com.tencent.mars.sample.wrapper.remote.MarsServiceProxy;
import com.tencent.mars.sample.wrapper.service.DebugMarsServiceProfile;
import com.tencent.mars.sample.wrapper.service.MarsServiceProfile;
import com.tencent.mars.sample.wrapper.service.MarsServiceProfileFactory;
import com.tencent.mars.sample.wrapper.service.MarsServiceStub;
import com.tencent.mars.sdt.SdtLogic;
import com.tencent.mars.stn.StnLogic;
import com.tencent.mars.xlog.Log;
import com.tencent.mars.xlog.Xlog;

import com.google.gson.JsonObject;

/**
 * Created by Wenpzhou on 2018/4/10 0010.
 */

public class ApplicationManager extends Application {
    private static Context context;
    private GameServiceProfile profile=new GameServiceProfile();
    private MarsServiceStub stub;
    private static MarsServiceProfileFactory gFactory = new MarsServiceProfileFactory() {
        @Override
        public MarsServiceProfile createMarsServiceProfile() {
            return new DebugMarsServiceProfile();
        }
    };

    public static void setProfileFactory(MarsServiceProfileFactory factory) {
        gFactory = factory;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        context=getApplicationContext();
        CrashCaptureInit();
        logInit();
        //Log.d("TAG","onCreate");
        //stnInit();
    }

    public void logInit(){
        System.loadLibrary("stlport_shared");
        System.loadLibrary("marsxlog");

        final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String logPath = SDCARD + "/marssample/log";

// this is necessary, or may cash for SIGBUS
        final String cachePath = this.getFilesDir() + "/xlog";

//init xlog
        if (!BuildConfig.DEBUG) {
            Xlog.appenderOpen(Xlog.LEVEL_DEBUG, Xlog.AppednerModeAsync, cachePath, logPath, "MarsSample", "");
            Xlog.setConsoleLogOpen(true);

            //Toast.makeText(context, SDCARD+"\n"+logPath+"\n"+cachePath+"\n", Toast.LENGTH_LONG).show();
        } else {

            Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath, "MarsSample", "");
            Xlog.setConsoleLogOpen(false);
        }

        Log.setLogImp(new Xlog());

    }
    public void stnInit(){


        final MarsServiceProfile profile = gFactory.createMarsServiceProfile();
        stub = new MarsServiceStub(this, profile);


        // set callback
        AppLogic.setCallBack(stub);
        StnLogic.setCallBack(stub);
        SdtLogic.setCallBack(stub);

// Initialize the Mars PlatformComm
        Mars.init(getApplicationContext(), new Handler(Looper.getMainLooper()));

// Initialize the Mars
        StnLogic.setLonglinkSvrAddr(profile.longLinkHost(), profile.longLinkPorts());
        StnLogic.setShortlinkSvrAddr(profile.shortLinkPort());
        StnLogic.setClientVersion(profile.productID());
        Mars.onCreate(true);

        BaseEvent.onForeground(true);
        StnLogic.makesureLongLinkConnected();

        NetworkServiceProxy.init(this, getMainLooper(), null);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Mars.onDestroy();
        Log.appenderClose();
    }
    public void CrashCaptureInit(){
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }

    public static Context getContext(){
        return context;
    }

}
