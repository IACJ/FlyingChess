package com.flashminds.flyingchess.activity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flashminds.flyingchess.entity.ChessBoard;
import com.flashminds.flyingchess.entity.Global;
import com.flashminds.flyingchess.manager.DataManager;
import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.R;
import com.flashminds.flyingchess.manager.SoundManager;

import java.util.ArrayList;

/**
 * Edited by IACJ on 2018/4/1.
 */
public class ChessBoardActivity extends AppCompatActivity {
    Button pauseButton;
    Button throwDiceButton;
    Button[][] plane;
    int boardWidth;
    public Handler handler;
    ImageView map;
    float dx;
    int n;
    TextView xt[], xname[], xscore[];

    SensorManager manager;
    ShakeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ui setting
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess_board);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);//Activity切换动画
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Global.activityManager.add(this);
        Global.soundManager.playMusic(SoundManager.GAME);
        //init
        pauseButton = (Button) findViewById(R.id.pause);
        throwDiceButton = (Button) findViewById(R.id.dice);
        plane = new Button[4][4];
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

        handler = new MyHandler(this);
        map = (ImageView) findViewById(R.id.map);

        xt = new TextView[4];
        xname = new TextView[4];
        xscore = new TextView[4];
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

        //set data
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        boardWidth = dm.heightPixels;

        n = 36;
        dx = boardWidth / n;

        map.setImageBitmap(Global.getBitmap(R.raw.map_min));
        //trigger
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), PauseActivity.class));
            }
        });
        throwDiceButton.setOnClickListener(new View.OnClickListener() {//throw dice
            @Override
            public void onClick(View v) {
                Global.playersData.get(Global.dataManager.getMyId()).setDiceValid(0);
            }
        });

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        listener = new ShakeListener();
        manager.registerListener(listener,
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                manager.SENSOR_DELAY_NORMAL);
        /////////////////
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                plane[i][j].setOnClickListener(new myOnClickListener(i, j));
                plane[i][j].setVisibility(View.INVISIBLE);
            }
        }
        ///setting

        // 初始化飞机view
        for (int i=0;i<4;i++){
            for (int j=0;j<4;j++){
                moveTo(plane[i][j], ChessBoard.mapStart[i][j][0],ChessBoard.mapStart[i][j][1]);
            }
        }


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
        Global.gameManager.newTurn(this);

        for (int i = 0; i < 4; i++) {
            xname[i].setTypeface(Global.getFont());
            xscore[i].setTypeface(Global.getFont());
        }
        throwDiceButton.setBackground(Global.d[0]);
    }

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

    public void exit() {
        if (Global.dataManager.getGameMode() == DataManager.GM_WLAN) {
            Global.socketManager.send(DataPack.R_GAME_EXIT, Global.dataManager.getMyId(), Global.dataManager.getRoomId());
        }
        Global.gameManager.gameOver();
        if (Global.dataManager.getGameMode() != DataManager.GM_LOCAL) {
            startActivity(new Intent(getApplicationContext(), GameInfoActivity.class));
            if (Global.dataManager.getGameMode() == DataManager.GM_LAN) {
                Global.localServer.stopHost();
            }
        } else {
            startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
        }
        Global.dataManager.giveUp(false);
        Global.soundManager.playMusic(SoundManager.BACKGROUND);
    }

    public void moveTo(Button plane, int x, int y) {
        plane.setX(x * dx);
        plane.setY(y * dx);
    }

    public void animMoveTo(Button plane, int x, int y) {
        plane.animate().setDuration(100);
        plane.animate().translationX(x * dx);
        plane.animate().translationY(y * dx);
    }
}


class MyHandler extends Handler {
    ChessBoardActivity parent;

    public MyHandler(ChessBoardActivity parent) {
        this.parent = parent;
    }

    @Override
    public void handleMessage(Message msg) {//事件回调
        switch (msg.what) {
            case 1://飞机
            {
                int color = msg.getData().getInt("color");
                int whichPlane = msg.getData().getInt("whichPlane");
                int pos = msg.getData().getInt("pos");
                parent.animMoveTo(parent.plane[color][whichPlane], Global.chessBoard.map[color][pos][0], Global.chessBoard.map[color][pos][1]);
            }
            break;
            case 2://骰子
                System.out.println(msg.getData().getInt("dice"));
                parent.throwDiceButton.setBackground(Global.d[msg.getData().getInt("dice") - 1]);
                break;
            case 3://显示消息
                Toast.makeText(parent.getApplicationContext(), msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                break;
            case 4: // crash
            {
                int color = msg.getData().getInt("color");
                int whichPlane = msg.getData().getInt("whichPlane");
                parent.animMoveTo(parent.plane[color][whichPlane], Global.chessBoard.mapStart[color][whichPlane][0], Global.chessBoard.mapStart[color][whichPlane][1]);
            }
            break;
            case 5://finished
            {
                if (Global.replayManager.isReplay == false) {
                    Intent intent = new Intent(parent.getApplicationContext(), RoomActivity.class);
                    ArrayList<String> msgs = new ArrayList<>();
                    if (Global.dataManager.getGameMode() == DataManager.GM_LOCAL) {
                        if (Global.dataManager.getLastWinner().compareTo(Global.dataManager.getMyId()) == 0) {//更新分数
                            Global.dataManager.setScore(Global.dataManager.getScore() + 10);
                            Global.soundManager.playSound(SoundManager.WIN);
                        } else {
                            Global.dataManager.setScore(Global.dataManager.getScore() - 5);
                            //Global.soundManager.playSound(SoundManager.LOSE);
                        }

                        Global.dataManager.saveData();
                        msgs.add(Global.dataManager.getMyId());
                        msgs.add(Global.playersData.get(Global.dataManager.getMyId()).name);
                        msgs.add(String.valueOf(Global.dataManager.getScore()));
                        msgs.add("-1");
                    } else if (Global.dataManager.getGameMode() == DataManager.GM_WLAN) {
                        for (String key : Global.playersData.keySet()) {//更新玩家的分数
                            if (Global.playersData.get(key).offline == false) {
                                if (Global.playersData.get(key).id.compareTo(Global.dataManager.getLastWinner()) == 0) {
                                    Global.playersData.get(key).score = String.valueOf(Integer.valueOf(Global.playersData.get(key).score) + 10);
                                    Global.soundManager.playSound(SoundManager.WIN);
                                } else {
                                    Global.playersData.get(key).score = String.valueOf(Integer.valueOf(Global.playersData.get(key).score) - 5);
                                    //Global.soundManager.playSound(SoundManager.LOSE);
                                }
                            }
                        }

                        Global.dataManager.setOnlineScore(Global.playersData.get(Global.dataManager.getMyId()).score);

                        msgs.add(Global.playersData.get(Global.dataManager.getHostId()).id);
                        msgs.add(Global.playersData.get(Global.dataManager.getHostId()).name);
                        msgs.add(Global.playersData.get(Global.dataManager.getHostId()).score);
                        msgs.add("-1");
                        for (String key : Global.playersData.keySet()) {
                            Global.playersData.get(key).color = -1;
                            if (Global.dataManager.getHostId().compareTo(Global.playersData.get(key).id) != 0 && Integer.valueOf(Global.playersData.get(key).id) >= 0 && Global.playersData.get(key).offline == false) {
                                msgs.add(Global.playersData.get(key).id);
                                msgs.add(Global.playersData.get(key).name);
                                msgs.add(Global.playersData.get(key).score);
                                msgs.add("-1");
                            }
                        }
                    }
                    intent.putStringArrayListExtra("msgs", msgs);
                    parent.startActivity(intent);
                    Intent intent2 = new Intent(parent.getApplicationContext(), GameEndActivity.class);
                    intent2.putStringArrayListExtra("msgs", msgs);
                    parent.startActivity(intent2);
                    Global.dataManager.giveUp(false);
                    Global.gameManager.gameOver();
                    Global.soundManager.playMusic(SoundManager.BACKGROUND);
                    Global.replayManager.closeRecord();
                    Global.replayManager.stopReplay();
                } else {
                    Toast.makeText(parent, "Replay finished!", Toast.LENGTH_SHORT).show();
                    parent.startActivity(new Intent(parent.getApplicationContext(), ChooseModeActivity.class));
                }
                break;
            }
            case 6://turn to
            {
                for (int i = 0; i < 4; i++) {
                    parent.xt[i].setText(" ");
                }
                parent.xt[msg.getData().getInt("color")].setText(">");
            }
            break;
            default:
                super.handleMessage(msg);
        }
    }
}

class myOnClickListener implements View.OnClickListener {
    int color, which;

    public myOnClickListener(int color, int which) {
        this.color = color;
        this.which = which;
    }

    @Override
    public void onClick(View v) {
        if (Global.playersData.get(Global.dataManager.getMyId()).color == color)
            Global.playersData.get(Global.dataManager.getMyId()).setPlaneValid(which);
    }
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