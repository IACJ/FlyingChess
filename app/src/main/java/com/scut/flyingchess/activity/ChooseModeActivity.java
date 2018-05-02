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
    Button btnLocal,btnLan,local, lan, wlan;
    boolean exit;
    Timer closeTimer;
    ImageView bk2;
    ImageView waitImage;
    Button waitBackground;
    Button records;

    Button confirmName;//确认昵称按钮
    EditText userName; //用户输入框
    Boolean hasInputName = false;
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
        Global.soundManager.playMusic(SoundManager.BACKGROUND);
        Global.activityManager.add(this);
        // 查找view
        btnLocal = (Button) findViewById(R.id.btn_local);
        btnLan = (Button) findViewById(R.id.btn_lan);
        local = (Button) findViewById(R.id.button2);
        lan = (Button) findViewById(R.id.button3);
        wlan = (Button) findViewById(R.id.button4);
        exit = false;
        closeTimer = new Timer();
        exit = false;
        bk2 = (ImageView) findViewById(R.id.backgroud2);
        waitImage = (ImageView) findViewById(R.id.wait);
        waitBackground = (Button) findViewById(R.id.waitbackground);
        records = (Button) findViewById(R.id.records);

        pref = getSharedPreferences("data",MODE_PRIVATE);
        editor = pref.edit();
        hasInputName = pref.getBoolean("hasInputName",false);

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
                hasInputName = pref.getBoolean("hasInputName",false);
                if(!hasInputName){
                    //设置弹窗
                    popupWindow = LayoutInflater.from(getBaseContext()).inflate(R.layout.popwindow,null);
                    PopupWindow popWindow = new PopupWindow(popupWindow,700,400);
                    popWindow.setFocusable(true);
                    popWindow.setOutsideTouchable(false);
                    //设置弹窗位于lan button上方
                    popWindow.showAsDropDown(btnLan,(btnLan.getWidth() - 700)/2,-btnLan.getHeight()-400);
                    confirmName = (Button) popupWindow.findViewById(R.id.confirmName) ;
                    userName = (EditText) popupWindow.findViewById(R.id.userName);
                    confirmName.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v){
                            String text = String.valueOf(userName.getText());
                            Log.v("ssss",String.valueOf(text.length()));
                            if(text.length() > 10){
                                Toast.makeText(ChooseModeActivity.this,"请输入10个以内的昵称",Toast.LENGTH_SHORT).show();
                            }else{
                                if(text.length() == 0){
                                    Toast.makeText(ChooseModeActivity.this,"请输入您的昵称",Toast.LENGTH_SHORT).show();
                                }else{
                                    //保存昵称
                                    editor.putBoolean("hasInputName",true);
                                    editor.commit();
                                    startActivity( new Intent(getApplicationContext(), LanHallActivity.class));
                                    ChooseModeActivity.this.popupWindow.setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                    });
                }
                else{
                    startActivity( new Intent(getApplicationContext(), LanHallActivity.class));
                }
            }
        });


        local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//choose local game
                Global.soundManager.playSound(SoundManager.BUTTON);
                Global.dataManager.setGameMode(DataManager.GM_LOCAL);//set game mode

                Intent intent = new Intent(getApplicationContext(), RoomActivity.class);
                ArrayList<String> msgs = new ArrayList<>();
                Global.dataManager.setMyId("0");
                msgs.add("0");
                msgs.add("ME");
                msgs.add(String.valueOf(Global.dataManager.getScore()));
                msgs.add("-1");
                intent.putStringArrayListExtra("msgs", msgs);
                startActivity(intent);//switch wo chess board activity
                clean();
            }
        });
        lan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);

                Global.dataManager.setGameMode(DataManager.GM_LAN);
                Global.dataManager.setMyName(new Build().MODEL);
                Global.localServer.startListen();
                Intent intent = new Intent(getApplicationContext(), GameInfoActivity.class);
                startActivity(intent);
                clean();
            }
        });
        wlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                Global.socketManager.connectToRemoteServer();
                local.setVisibility(View.INVISIBLE);
                lan.setVisibility(View.INVISIBLE);
                wlan.setVisibility(View.INVISIBLE);
                records.setVisibility(View.INVISIBLE);
                waitImage.setVisibility(View.VISIBLE);
                btnLocal.setVisibility(View.INVISIBLE);
                btnLan.setVisibility(View.INVISIBLE);
                waitBackground.setVisibility(View.VISIBLE);
                Global.startWaitAnimation(waitImage);
                clean();
            }
        });
        records.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), RecordsActivity.class));
            }
        });



        // 动态设置V iew UI
        waitImage.setVisibility(View.INVISIBLE);
        waitBackground.setVisibility(View.INVISIBLE);
        bk2.setImageBitmap(Global.getBitmap(R.raw.cloud));
        lan.setTypeface(Global.getFont());
        wlan.setTypeface(Global.getFont());
        local.setTypeface(Global.getFont());

        //注册响应远程事件
        Global.socketManager.registerActivity(DataPack.CONNECTED, this);
        //        Global.updateManager.checkUpdate();
    }
    @Override
    public void processDataPack(DataPack dataPack) {
        if (dataPack.getCommand() == DataPack.CONNECTED) {
            if (dataPack.isSuccessful()) {
                Global.dataManager.setGameMode(DataManager.GM_WLAN);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);

            } else {
                wlan.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "连接服务器失败！", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            wlan.post(new Runnable() {
                @Override
                public void run() {
                    btnLocal.setVisibility(View.VISIBLE);
                    btnLan.setVisibility(View.VISIBLE);
                    local.setVisibility(View.VISIBLE);
                    lan.setVisibility(View.VISIBLE);
                    wlan.setVisibility(View.VISIBLE);
                    records.setVisibility(View.VISIBLE);
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
        Global.soundManager.resumeMusic(SoundManager.BACKGROUND);
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
                    Toast.makeText(getApplicationContext(), "press again to exit", Toast.LENGTH_SHORT).show();
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
