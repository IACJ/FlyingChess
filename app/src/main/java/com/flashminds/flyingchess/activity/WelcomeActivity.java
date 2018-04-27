package com.flashminds.flyingchess.activity;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.flashminds.flyingchess.Global;
import com.flashminds.flyingchess.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Edited by IACJ on 2018/4/4.
 *
 * 播放欢迎动画，然后跳转到ChooseModeActivity。
 */
public class WelcomeActivity extends BaseActivity {
    SurfaceView sv;
    MediaPlayer mediaPlayer;

    Boolean jump = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //UI setting
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //initialization
        sv = (SurfaceView) findViewById(R.id.surfaceView);
        ///////////
        Global.init(this);
        Global.activityManager.add(this);
        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mediaPlayer = new MediaPlayer();
                    AssetFileDescriptor fileDescriptor = getAssets().openFd("movie/start.mp4");
                    mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                    mediaPlayer.setDisplay(sv.getHolder());
                    mediaPlayer.setLooping(false);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
            }
        });
        sv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                jump = true;
                mediaPlayer = null;
                sv = null;
                Intent intent = new Intent(getApplicationContext(), ChooseModeActivity.class);
                startActivity(intent);//switch activity
                WelcomeActivity.this.finish();
            }
        });


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (jump == false){
                    mediaPlayer = null;
                    sv = null;
                    Intent intent = new Intent(getApplicationContext(), ChooseModeActivity.class);
                    startActivity(intent);//switch activity
                    WelcomeActivity.this.finish();
                }
            }
        }, 11500);
    }
}
