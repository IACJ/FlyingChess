package com.flashminds.flyingchess.activity;

import android.content.Intent;
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

import com.flashminds.flyingchess.R;
import com.flashminds.flyingchess.entity.Game;
import com.flashminds.flyingchess.manager.DataManager;
import com.flashminds.flyingchess.manager.LocalGameManager;
import com.flashminds.flyingchess.manager.SoundManager;

import java.util.ArrayList;

/**
 * Created by IACJ on 2018/4/8.
 */
public class LocalGamingActivity extends AppCompatActivity {
    Button pauseButton;
    Button throwDiceButton;
    Button[][] plane = new Button[4][4];
    TextView[] xt= new TextView[4];
    TextView[] xname = new TextView[4];
    TextView[] xscore = new TextView[4];
    ImageView map;

    int boardWidth;
    public Handler handler;
    float dx;
    int n;

    class LocalGamingOnClickListener implements View.OnClickListener {
        int color, which;
        public LocalGamingOnClickListener(int color, int which) {
            this.color = color;
            this.which = which;
        }
        @Override
        public void onClick(View v) {
            if (Game.playersData.get(Game.dataManager.getMyId()).color == color)
                Game.playersData.get(Game.dataManager.getMyId()).setPlaneValid(which);
        }
    }
    class LocalGamingHandler extends Handler {
        LocalGamingActivity parent;

        public LocalGamingHandler(LocalGamingActivity parent) {
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {//事件回调
            switch (msg.what) {
                case 1: { //飞机

                    int color = msg.getData().getInt("color");
                    int whichPlane = msg.getData().getInt("whichPlane");
                    int pos = msg.getData().getInt("pos");
                    parent.animMoveTo(parent.plane[color][whichPlane], Game.chessBoard.map[color][pos][0], Game.chessBoard.map[color][pos][1]);
                }
                break;
                case 2: { //骰子
                    parent.throwDiceButton.setBackground(Game.d[msg.getData().getInt("dice") - 1]);
                }
                    break;
                case 3: { //显示消息
                    Toast.makeText(parent.getApplicationContext(), msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                }
                    break;
                case 4:  { // crash
                    int color = msg.getData().getInt("color");
                    int whichPlane = msg.getData().getInt("whichPlane");
                    parent.animMoveTo(parent.plane[color][whichPlane], Game.chessBoard.mapStart[color][whichPlane][0], Game.chessBoard.mapStart[color][whichPlane][1]);
                }
                break;
                case 5:  { //finished
                    Intent intent = new Intent(parent.getApplicationContext(), RoomActivity.class);

                    if (Game.dataManager.getLastWinner().compareTo(Game.dataManager.getMyId()) == 0) {//更新分数
                        Game.dataManager.setScore(Game.dataManager.getScore() + 10);
                        Game.soundManager.playSound(SoundManager.WIN);
                    } else {
                        Game.dataManager.setScore(Game.dataManager.getScore() - 5);
                        Game.soundManager.playSound(SoundManager.LOSE);
                    }
                    Game.dataManager.saveData();

                    ArrayList<String> msgs = new ArrayList<>();
                    msgs.add(Game.dataManager.getMyId());
                    msgs.add(Game.playersData.get(Game.dataManager.getMyId()).name);
                    msgs.add(String.valueOf(Game.dataManager.getScore()));
                    msgs.add("-1");
                    intent.putStringArrayListExtra("msgs", msgs);
                    parent.startActivity(intent);

                    Intent intent2 = new Intent(parent.getApplicationContext(), GameEndActivity.class);
                    intent2.putStringArrayListExtra("msgs", msgs);
                    parent.startActivity(intent2);
                    Game.localGameManager.gameOver();

                    Game.dataManager.giveUp(false);
                    Game.replayManager.closeRecord();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess_board);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);//Activity切换动画
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Game.activityManager.add(this);
        Game.soundManager.playMusic(SoundManager.GAME);

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

        handler = new LocalGamingHandler(this);


        //  绘制棋盘
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        boardWidth = dm.heightPixels;
        n = 19;
        dx = boardWidth / n + 0.8f;
        map.setImageBitmap(Game.getBitmap(R.raw.map_min));

        // 按钮事件
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), LocalPauseActivity.class));
            }
        });
        throwDiceButton.setOnClickListener(new View.OnClickListener() {//throw dice
            @Override
            public void onClick(View v) {
                Game.playersData.get(Game.dataManager.getMyId()).setDiceValid(0);
            }
        });

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                plane[i][j].setOnClickListener(new LocalGamingOnClickListener(i, j));
                plane[i][j].setVisibility(View.INVISIBLE);
            }
        }
        // 初始化飞机view
        movePlaneTo(plane[0][0], 1, n - 4);
        movePlaneTo(plane[0][1], 3, n - 4);
        movePlaneTo(plane[0][2], 1, n - 2);
        movePlaneTo(plane[0][3], 3, n - 2);

        movePlaneTo(plane[1][0], n - 4, n - 4);
        movePlaneTo(plane[1][1], n - 2, n - 4);
        movePlaneTo(plane[1][2], n - 4, n - 2);
        movePlaneTo(plane[1][3], n - 2, n - 2);

        movePlaneTo(plane[2][0], n - 4, 1);
        movePlaneTo(plane[2][1], n - 2, 1);
        movePlaneTo(plane[2][2], n - 4, 3);
        movePlaneTo(plane[2][3], n - 2, 3);

        movePlaneTo(plane[3][0], 1, 1);
        movePlaneTo(plane[3][1], 3, 1);
        movePlaneTo(plane[3][2], 1, 3);
        movePlaneTo(plane[3][3], 3, 3);

        Game.replayManager.savePlayerNum(Game.playersData.size());
        for (String key : Game.playersData.keySet()) {
            Game.replayManager.saveRoleKey(key);
            Game.replayManager.saveRoleInfo(Game.playersData.get(key));
            plane[Game.playersData.get(key).color][0].setVisibility(View.VISIBLE);
            plane[Game.playersData.get(key).color][1].setVisibility(View.VISIBLE);
            plane[Game.playersData.get(key).color][2].setVisibility(View.VISIBLE);
            plane[Game.playersData.get(key).color][3].setVisibility(View.VISIBLE);
            xt[Game.playersData.get(key).color].setText("");
            xname[Game.playersData.get(key).color].setText(Game.playersData.get(key).name);
            xscore[Game.playersData.get(key).color].setText(Game.playersData.get(key).score);
        }

        // 设置字体及背景图
        for (int i = 0; i < 4; i++) {
            xname[i].setTypeface(Game.getFont());
            xscore[i].setTypeface(Game.getFont());
        }
        throwDiceButton.setBackground(Game.d[0]);

        Game.localGameManager = new LocalGameManager();
        Game.localGameManager.startGame(this);
    }

    public void exit() {
        Game.localGameManager.gameOver();
        startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
    }

    public void movePlaneTo(Button plane, int x, int y) {
        plane.setX(x * dx);
        plane.setY(y * dx);
    }

    public void animMoveTo(Button plane, int x, int y) {
        plane.animate().setDuration(100);
        plane.animate().translationX(x * dx);
        plane.animate().translationY(y * dx);
    }
    ///////////////////////// 常规操作 /////////////////////////
    @Override
    public void onStart() {
        super.onStart();
        Game.soundManager.resumeMusic(SoundManager.GAME);
    }
    @Override
    public void onStop() {
        super.onStop();
        Game.soundManager.pauseMusic();
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {//返回按钮
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (Game.replayManager.isReplay == false)
                    Game.replayManager.clearRecord();
                exit();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}




