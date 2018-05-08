package com.scut.flyingchess.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.scut.flyingchess.activity.lanGame.LanHallActivity;
import com.scut.flyingchess.activity.localGame.LocalRoomActivity;
import com.scut.flyingchess.Global;
import com.scut.flyingchess.activity.wanGame.WanLoginActivity;
import com.scut.flyingchess.manager.DataManager;
import com.scut.flyingchess.dataPack.DataPack;
import com.scut.flyingchess.R;
import com.scut.flyingchess.manager.SoundManager;
import com.scut.flyingchess.dataPack.Target;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Edited by IACJ on 2018/4/8.
 *
 * 主界面，选择Local、Lan、Wan模式。
 */
public class ChooseModeActivity extends BaseActivity implements Target {
    Button btnLocal, btnLan, btnWan, btnRecord;
    boolean exit= false;
    Timer closeTimer;
    ImageView bk2;
    ImageView waitImage;
    Button waitBackground;

    Button confirmName;//确认昵称按钮
    EditText userName; //用户输入框
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    View popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ui setting
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_mode);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);//Activity切换动画
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Global.activityManager.add(this);
        // 查找view
        btnLocal = (Button) findViewById(R.id.btn_local);
        btnLan = (Button) findViewById(R.id.btn_lan);
        btnWan = (Button) findViewById(R.id.btn_wan);
        closeTimer = new Timer();
        bk2 = (ImageView) findViewById(R.id.backgroud2);
        waitImage = (ImageView) findViewById(R.id.wait);
        waitBackground = (Button) findViewById(R.id.waitbackground);
        btnRecord = (Button) findViewById(R.id.records);

        pref = getSharedPreferences("data",MODE_PRIVATE);
        editor = pref.edit();

        // 按钮事件
        btnLocal.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                startActivity(new Intent(ChooseModeActivity.this,LocalRoomActivity.class));
            }
        } );

        btnLan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                //设置弹窗
                popupWindow = LayoutInflater.from(getBaseContext()).inflate(R.layout.popwindow,null);
                PopupWindow popWindow = new PopupWindow(popupWindow,900,500);
                popWindow.setFocusable(true);
                popWindow.setOutsideTouchable(false);
                //设置弹窗位于lan button上方
                popWindow.showAsDropDown(btnLan,(btnLan.getWidth() - 900)/2,-btnLan.getHeight()-500);
                confirmName = (Button) popupWindow.findViewById(R.id.confirmName) ;
                userName = (EditText) popupWindow.findViewById(R.id.userName);
                confirmName.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        String text = String.valueOf(userName.getText());

                        if(text.length() > 10){
                            Toast.makeText(ChooseModeActivity.this,"请输入10个以内的昵称",Toast.LENGTH_SHORT).show();
                        }else{
                            if(text.length() == 0){
                                Toast.makeText(ChooseModeActivity.this,"请输入您的昵称",Toast.LENGTH_SHORT).show();
                            }else{
                                Global.dataManager.setLanName(text);
                                startActivity( new Intent(getApplicationContext(), LanHallActivity.class));
                                ChooseModeActivity.this.popupWindow.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                });
            }
        });

        btnWan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                Global.socketManager.connectToRemoteServer();
                btnWan.setVisibility(View.INVISIBLE);
                btnRecord.setVisibility(View.INVISIBLE);
                waitImage.setVisibility(View.VISIBLE);
                btnLocal.setVisibility(View.INVISIBLE);
                btnLan.setVisibility(View.INVISIBLE);
                waitBackground.setVisibility(View.VISIBLE);
                Global.startWaitAnimation(waitImage);
                clean();
            }
        });
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), RecordsActivity.class));
            }
        });



        // 动态设置V iew UI
        waitImage.setVisibility(View.INVISIBLE);
        waitBackground.setVisibility(View.INVISIBLE);
        bk2.setImageBitmap(Global.getBitmap(R.raw.cloud));
        btnWan.setTypeface(Global.getFont());

        //注册响应远程事件
        Global.socketManager.registerActivity(DataPack.CONNECTED, this);
    }
    @Override
    public void processDataPack(DataPack dataPack) {
        if (dataPack.getCommand() == DataPack.CONNECTED) {
            if (dataPack.isSuccessful()) {
                Global.dataManager.setGameMode(DataManager.GM_WLAN);
                Intent intent = new Intent(getApplicationContext(), WanLoginActivity.class);
                startActivity(intent);
            } else {
                btnWan.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "连接服务器失败！", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            btnWan.post(new Runnable() {
                @Override
                public void run() {
                    btnLocal.setVisibility(View.VISIBLE);
                    btnLan.setVisibility(View.VISIBLE);
                    btnWan.setVisibility(View.VISIBLE);
                    btnRecord.setVisibility(View.VISIBLE);
                    waitBackground.setVisibility(View.INVISIBLE);
                    waitImage.setVisibility(View.INVISIBLE);
                }
            });
            Global.stopWaitAnimation();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Global.soundManager.playMusic(SoundManager.BACKGROUND);
    }

    @Override
    public void onStop() {
        super.onStop();
        Global.soundManager.pauseMusic();
    }

    public void clean() {
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {//返回按钮
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (exit) {
                    Global.activityManager.closeAll();
                    System.exit(0);
                } else {
                    exit = true;
                    Toast.makeText(getApplicationContext(), "再点一次，退出游戏。", Toast.LENGTH_SHORT).show();
                    closeTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            exit = false;
                        }
                    }, 1200);
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
