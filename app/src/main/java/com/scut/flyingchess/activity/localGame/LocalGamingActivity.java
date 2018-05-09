package com.scut.flyingchess.activity.localGame;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.scut.flyingchess.R;
import com.scut.flyingchess.activity.ChooseModeActivity;
import com.scut.flyingchess.activity.GameEndActivity;
import com.scut.flyingchess.entity.ChessBoard;
import com.scut.flyingchess.Global;
import com.scut.flyingchess.manager.LocalGameManager;
import com.scut.flyingchess.manager.SoundManager;
import com.scut.flyingchess.activity.BaseActivity;

import java.util.ArrayList;

/**
 * Created by IACJ on 2018/4/8.
 */
public class LocalGamingActivity extends BaseActivity {
    Button pauseButton;
    Button throwDiceButton;
    Button[][] plane = new Button[4][4];
    TextView[] xt= new TextView[4];
    TextView[] xname = new TextView[4];
    TextView[] xscore = new TextView[4];
    TextView message;
    ImageView map;

    SensorManager manager;
    ShakeListener listener;

    int boardWidth;
    public Handler handler;
    float dx;
    int n;

    private static final String TAG = "LocalGamingActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ui setting
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gaming);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);//Activity切换动画
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Global.activityManager.add(this);
        Global.soundManager.playMusic(SoundManager.GAME);

        // 查找View
        pauseButton = (Button) findViewById(R.id.pause);
        throwDiceButton = (Button) findViewById(R.id.dice);
        map = (ImageView) findViewById(R.id.map);

        plane[0][0] = (Button) findViewById(R.id.R1);
        plane[0][1] = (Button) findViewById(R.id.R2);
        plane[0][2] = (Button) findViewById(R.id.R3);
        plane[0][3] = (Button) findViewById(R.id.R4);

        plane[1][0] = (Button) findViewById(R.id.G1);
        plane[1][1] = (Button) findViewById(R.id.G2);
        plane[1][2] = (Button) findViewById(R.id.G3);
        plane[1][3] = (Button) findViewById(R.id.G4);

        plane[2][0] = (Button) findViewById(R.id.B1);
        plane[2][1] = (Button) findViewById(R.id.B2);
        plane[2][2] = (Button) findViewById(R.id.B3);
        plane[2][3] = (Button) findViewById(R.id.B4);

        plane[3][0] = (Button) findViewById(R.id.Y1);
        plane[3][1] = (Button) findViewById(R.id.Y2);
        plane[3][2] = (Button) findViewById(R.id.Y3);
        plane[3][3] = (Button) findViewById(R.id.Y4);

        xt[0] = (TextView) findViewById(R.id.rt);
        xt[1] = (TextView) findViewById(R.id.gt);
        xt[2] = (TextView) findViewById(R.id.bt);
        xt[3] = (TextView) findViewById(R.id.yt);

        xname[0] = (TextView) findViewById(R.id.rname);
        xname[1] = (TextView) findViewById(R.id.gname);
        xname[2] = (TextView) findViewById(R.id.bname);
        xname[3] = (TextView) findViewById(R.id.yname);

        xscore[0] = (TextView) findViewById(R.id.rscore);
        xscore[1] = (TextView) findViewById(R.id.gscore);
        xscore[2] = (TextView) findViewById(R.id.bscore);
        xscore[3] = (TextView) findViewById(R.id.yscore);

        message = (TextView) findViewById(R.id.message);

        handler = new LocalGamingHandler(this);

        // 适配屏幕
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        boardWidth = dm.heightPixels;
        n = 36;
        dx = (float) boardWidth / n;
        Log.v(TAG, "onCreate: 屏幕适配 dx ="+dx);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) plane[i][j].getLayoutParams();
                params.height=(int) (2*dx);
                params.width=(int) (2*dx);
                plane[i][j].setLayoutParams(params);
            }
        }
        map.setImageBitmap(Global.getBitmap(R.raw.map_min));

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        listener = new ShakeListener();
        manager.registerListener(listener,
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                manager.SENSOR_DELAY_NORMAL);

        // 按钮事件
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), LocalSettingActivity.class));
            }
        });
        throwDiceButton.setOnClickListener(new View.OnClickListener() {//throw dice
            @Override
            public void onClick(View v) {
                Global.playersData.get(Global.dataManager.getMyId()).setDiceValid(0);
            }
        });

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                plane[i][j].setOnClickListener(new LocalGamingOnClickListener(i, j));
                plane[i][j].setVisibility(View.INVISIBLE);
            }
        }
        // 初始化飞机view
        for (int i=0;i<4;i++){
            for (int j=0;j<4;j++){
                movePlaneTo(plane[i][j], ChessBoard.mapStart[i][j][0],ChessBoard.mapStart[i][j][1]);
            }
        }

        Global.replayManager.startRecord();
        Global.replayManager.savePlayerNum(Global.playersData.size());
        for (String key : Global.playersData.keySet()) {
            Global.replayManager.saveRoleKey(key);
            Global.replayManager.saveRoleInfo(Global.playersData.get(key));
            plane[Global.playersData.get(key).color][0].setVisibility(View.VISIBLE);
            plane[Global.playersData.get(key).color][1].setVisibility(View.VISIBLE);
            plane[Global.playersData.get(key).color][2].setVisibility(View.VISIBLE);
            plane[Global.playersData.get(key).color][3].setVisibility(View.VISIBLE);
            xt[Global.playersData.get(key).color].setText("");
            xname[Global.playersData.get(key).color].setText(Global.playersData.get(key).name);
            xscore[Global.playersData.get(key).color].setText(Global.playersData.get(key).score);
        }

        // 设置字体及背景图
        for (int i = 0; i < 4; i++) {
            xname[i].setTypeface(Global.getFont());
            xscore[i].setTypeface(Global.getFont());
        }
        throwDiceButton.setBackground(Global.d[0]);

        Global.dataManager.giveUp(false);
        Global.localGameManager = new LocalGameManager();
        Global.localGameManager.startGame(this);
    }

    public void exit() {
        Global.localGameManager.gameOver();
        startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
    }

    public void movePlaneTo(Button plane, int x, int y) {
        plane.setX(x * dx);
        plane.setY(y * dx);
    }

    public void animMoveTo(Button plane, int x, int y) {
        plane.animate().setDuration(200);
        plane.animate().translationX(x * dx);
        plane.animate().translationY(y * dx);
    }
    ///////////////////////// 常规操作 /////////////////////////
    @Override
    public void onStart() {
        super.onStart();
        Global.soundManager.resumeMusic(SoundManager.GAME);
    }
    @Override
    public void onStop() {
        super.onStop();
        Global.soundManager.pauseMusic();
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {//返回按钮
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (Global.replayManager.isReplay == false)
                    Global.replayManager.clearRecord();
                exit();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    class ShakeListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            //获取传感器类型
            int sensorType = sensorEvent.sensor.getType();
            //values[0]:X轴，values[1]:Y轴，values[2]:Z轴
            float[] values = sensorEvent.values;
            //如果传感器类型为加速段传感器，则判断是否为摇一摇
            if(sensorType == Sensor.TYPE_ACCELEROMETER) {
                if((Math.abs(values[0]) > 17 || Math.abs(values[1]) > 17 ||
                        Math.abs(values[2]) > 17)) {
                    Global.playersData.get(Global.dataManager.getMyId()).setDiceValid(0);

                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class LocalGamingHandler extends Handler {
        LocalGamingActivity parent;

        LocalGamingHandler(LocalGamingActivity parent) {
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {//事件回调
            switch (msg.what) {
                case 1: { //飞机

                    int color = msg.getData().getInt("color");
                    int whichPlane = msg.getData().getInt("whichPlane");
                    int pos = msg.getData().getInt("pos");

                    switch(color){
                        case 0:
                            message.setText("红色飞机移动中" );
                            break;
                        case 1:
                            message.setText("绿色飞机移动中" );
                            break;
                        case 2:
                            message.setText("蓝色飞机移动中" );
                            break;
                        case 3:
                            message.setText("黄色飞机移动中" );
                            break;
                    }
                    parent.animMoveTo(parent.plane[color][whichPlane], Global.chessBoard.map[color][pos][0], Global.chessBoard.map[color][pos][1]);
                }
                break;
                case 2: { //骰子
                    int currentDice = msg.getData().getInt("dice");
                    message.setText("骰子数是:" + currentDice );
                    Global.soundManager.playSound(currentDice+100);
                    parent.throwDiceButton.setBackground(Global.d[msg.getData().getInt("dice") - 1]);
                }
                break;
                case 3: { //显示消息
                    Toast.makeText(parent.getApplicationContext(), msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                }
                break;
                case 4:  { // crash
                    int color = msg.getData().getInt("color");
                    int whichPlane = msg.getData().getInt("whichPlane");
                    parent.animMoveTo(parent.plane[color][whichPlane], Global.chessBoard.mapStart[color][whichPlane][0], Global.chessBoard.mapStart[color][whichPlane][1]);
                }
                break;
                case 5:  { //finished
                    Intent intent = new Intent(parent.getApplicationContext(), LocalRoomActivity.class);

                    if (Global.dataManager.getLastWinner().compareTo(Global.dataManager.getMyId()) == 0) {
                        Global.soundManager.playSound(SoundManager.WIN);
                    } else {
                        Global.soundManager.playSound(SoundManager.LOSE);
                    }


                    ArrayList<String> msgs = new ArrayList<>();
                    msgs.add(Global.dataManager.getMyId());
                    msgs.add(Global.playersData.get(Global.dataManager.getMyId()).name);
                    msgs.add(String.valueOf(Global.dataManager.getScore()));
                    msgs.add("-1");
                    intent.putStringArrayListExtra("msgs", msgs);
                    parent.startActivity(intent);

                    Intent intent2 = new Intent(parent.getApplicationContext(), GameEndActivity.class);
                    intent2.putStringArrayListExtra("msgs", msgs);
                    parent.startActivity(intent2);
                    Global.localGameManager.gameOver();


                    Global.replayManager.saveRecord();
                    break;
                }
                case 6: {//turn to
                    for (int i = 0; i < 4; i++) {
                        parent.xt[i].setText(" ");
                    }
                    parent.xt[msg.getData().getInt("color")].setText(">");
                    switch(msg.getData().getInt("color")){
                        case 0:
                            message.setText("请红方掷骰子");
                            break;
                        case 1:
                            message.setText("请绿方掷骰子");
                            break;
                        case 2:
                            message.setText("请蓝方掷骰子");
                            break;
                        case 3:
                            message.setText("请黄方掷骰子" );
                            break;
                    }
                    break;
                }
                case 7: { //旋转的骰子
                    message.setText("掷骰子中……");
                    parent.throwDiceButton.setBackground(Global.d[msg.getData().getInt("dice") - 1]);
                    break;
                }

                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class LocalGamingOnClickListener implements View.OnClickListener {
        int color, which;
        LocalGamingOnClickListener(int color, int which) {
            this.color = color;
            this.which = which;
        }
        @Override
        public void onClick(View v) {
            if (Global.playersData.get(Global.dataManager.getMyId()).color == color)
                Global.playersData.get(Global.dataManager.getMyId()).setPlaneValid(which);
        }
    }



}




