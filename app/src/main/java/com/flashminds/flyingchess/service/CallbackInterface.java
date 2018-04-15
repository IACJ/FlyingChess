package com.flashminds.flyingchess.service;

import android.os.Environment;

import com.flashminds.flyingchess.Handler.CrashHandler;
import com.tencent.mars.app.AppLogic;

/**
 * Created by Wenpzhou on 2018/4/10 0010.
 */

public class CallbackInterface implements AppLogic.ICallBack {
    @Override
    public String getAppFilePath() {
        final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String logPath = SDCARD + "/marssample/data";
        return logPath;
    }

    @Override
    public AppLogic.AccountInfo getAccountInfo() {
        AppLogic.AccountInfo accountInfo = new AppLogic.AccountInfo(0x10,"test");
        return accountInfo;
    }

    @Override
    public int getClientVersion() {
        int versionCode = CrashHandler.SystemProperty.getAPKVersionCode();
        return versionCode;
    }

    @Override
    public AppLogic.DeviceInfo getDeviceType() {
        AppLogic.DeviceInfo deviceInfo = new AppLogic.DeviceInfo("xiaomi","phone");
        return deviceInfo;
    }
}
