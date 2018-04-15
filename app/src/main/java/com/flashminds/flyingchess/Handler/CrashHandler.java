package com.flashminds.flyingchess.Handler;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import com.flashminds.flyingchess.manager.ApplicationManager;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Wenpzhou on 2018/4/14 0014.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private static final boolean DEBUG = true;

    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/CrashInfo/log/";
    private static final String FILE_NAME = "crash";
    private static final String FILE_NAME_SUFFIX = ".trace";

    private String FILE_POSITION = "";

    private static CrashHandler sInstance = new CrashHandler();
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;
    private Context mContext;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return sInstance;
    }

    public void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context.getApplicationContext();
    }

    /**
     * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法
     * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            //导出异常信息到SD卡中
            dumpExceptionToSDCard(ex);
            //这里可以通过网络上传异常信息到服务器，便于开发人员分析日志从而解决bug
            uploadExceptionToServer();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ex.printStackTrace();
        //处理完之后结束程序
        //如果系统提供了默认的异常处理器，则交给系统去结束我们的程序，否则就由我们自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
        }

    }

    private void dumpExceptionToSDCard(Throwable ex) throws IOException {
        //如果SD卡不存在或无法使用，则无法把异常信息写入SD卡
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (DEBUG) {
                Log.w(TAG, "sdcard unmounted,skip dump exception");
                return;
            }
        }

        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(current));
        FILE_POSITION = PATH + FILE_NAME + time + FILE_NAME_SUFFIX;
        File file = new File(FILE_POSITION);

        System.out.println(FILE_POSITION);

        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            ex.printStackTrace(pw);
            pw.close();
            System.out.println("写入成功");
        } catch (Exception e) {
            Log.e(TAG, "dump crash info failed" + e.getMessage());
        }
    }

    private void dumpPhoneInfo(PrintWriter pw) throws PackageManager.NameNotFoundException {
        //应用版本名
        String verName=SystemProperty.getAPKVerName();
        if (verName!=""){
            pw.print("App Version: ");
            pw.print(verName);
            pw.print('_');
        }
        //应用版本号
        int versionCode=SystemProperty.getAPKVersionCode();
        if(versionCode!=0){
            pw.println(versionCode);
        }


        //android版本号
        pw.print("OS Version: ");
        pw.print(SystemProperty.getSystemVersion());
        pw.print("_");
        pw.println(SystemProperty.getSdkVersion());

        //手机制造商
        pw.print("Vendor: ");
        pw.println(SystemProperty.getDeviceMANUFACTURER());

        //手机型号
        pw.print("Model: ");
        pw.println(SystemProperty.getSystemModel());

        //cpu架构
        pw.print("CPU ABI: ");
        pw.println(SystemProperty.getSystemCPUABI());

    }

    private void uploadExceptionToServer() {
        //TODO Upload Exception Message To Your Web Server
        /*HttpTask httpTask = new HttpTask() {

            @Override
            public void callback(String apiUrl, JSONObject jsonObject) {
            }

            @Override
            public void progressCallback(int progress) {

            }
        };
        httpTask.url(Model.DOMAIN + Model.CRASHUPLOAD)
                .addParams("crash_file", new File(FILE_POSITION))
                .sendRequest();*/
    }
    public static class SystemProperty {
        /**
         * 获取当前手机系统语言。
         *
         * @return 返回当前系统语言。例如：当前设置的是“中文-中国”，则返回“zh-CN”
         */
        public static String getSystemLanguage() {
            return Locale.getDefault().getLanguage();
        }


        /**
         * 获取当前系统上的语言列表(Locale列表)
         *
         * @return  语言列表
         */
        public static Locale[] getSystemLanguageList() {
            return Locale.getAvailableLocales();
        }
        /**
         * 获取当前本地apk的版本
         *
         * @return  apk版本号
         */
        public static int getAPKVersionCode(){
            int versionCode=0;
            Context context=ApplicationManager.getContext();
            PackageManager pm = context.getPackageManager();
            try{
                PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
                versionCode=pi.versionCode;
            }catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
                Log.d(TAG,"can not find package name");
            }
            return versionCode;

        }

        /**
         * 获取版本号名称
         *
         * @return  apk版本名
         */
        public static String getAPKVerName() {
            String verName = "";
            Context context=ApplicationManager.getContext();
            PackageManager pm = context.getPackageManager();
            try{
                PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
                verName = pi.versionName;
            }catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
                Log.d(TAG,"can not find package name");
            }
            return verName;
        }

        /**
         * 获取当前手机系统版本号
         *
         * @return  系统版本号
         */
        public static String getSystemVersion() {
            return Build.VERSION.RELEASE;
        }

        /**
         * 获取sdk版本
         *
         * @return  sdk版本
         */
        public static int getSdkVersion(){
            return Build.VERSION.SDK_INT;
        }

        /**
         * 获取手机型号
         *
         * @return  手机型号
         */
        public static String getSystemModel() {
            return Build.MODEL;
        }

        /**
         * 获取手机厂商
         *
         * @return  手机厂商
         */
        public static String getDeviceMANUFACTURER() {
            return Build.MANUFACTURER;
        }

        /**
         * 获取CPU架构
         *
         * @return  CPU架构，如arm或者x86
         */
        public static String getSystemCPUABI(){
            return Build.CPU_ABI;
        }
    }
}
