package com.flashminds.flyingchess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.flashminds.flyingchess.R;
import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.dataPack.Target;
import com.flashminds.flyingchess.entity.ChessBoard;
import com.flashminds.flyingchess.entity.Game;
import com.flashminds.flyingchess.entity.Role;
import com.flashminds.flyingchess.manager.DataManager;
import com.flashminds.flyingchess.manager.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by IACJ on 2018/4/4.
 *
 * 开始 Local 游戏前的房间设定
 */
public class LocalRoomActivity extends AppCompatActivity {
    Button startButton, backButton;
    Button[] site = new Button[4];
    Button[] addRobotButton = new Button[4];
    int[] siteState = new int[4];// -1 none   0 robot    1 people
    ListView idlePlayerView;
    LinkedList<HashMap<String, String>> idlePlayerListData;
    SimpleAdapter idlePlayerListAdapter;
    TextView title;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 全局管理
        Game.activityManager.add(this);
        Game.soundManager.playMusic(SoundManager.BACKGROUND);

        // 绑定view
        startButton = (Button) findViewById(R.id.start);
        backButton = (Button) findViewById(R.id.back);
        site[0] = (Button) findViewById(R.id.R);
        site[1] = (Button) findViewById(R.id.G);
        site[2] = (Button) findViewById(R.id.B);
        site[3] = (Button) findViewById(R.id.Y);
        addRobotButton[0] = (Button) findViewById(R.id.jr);
        addRobotButton[1] = (Button) findViewById(R.id.jg);
        addRobotButton[2] = (Button) findViewById(R.id.jb);
        addRobotButton[3] = (Button) findViewById(R.id.jy);
        idlePlayerView = (ListView) findViewById(R.id.playerInRoom);
        idlePlayerListData = new LinkedList<>();
        idlePlayerListAdapter = new SimpleAdapter(getApplicationContext(), idlePlayerListData, R.layout.content_player_list_item, new String[]{"name", "score"}, new int[]{R.id.nameInRoom, R.id.scoreInRoom});
        idlePlayerView.setAdapter(idlePlayerListAdapter);
        title = (TextView) findViewById(R.id.title);

        // 字体设置
        title.setTypeface(Game.getFont());
        for (int i = 0; i < 4; i++) {
            site[i].setTypeface(Game.getFont());
            addRobotButton[i].setTypeface(Game.getFont());
        }
        startButton.setTypeface(Game.getFont());

        // 按钮事件
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.soundManager.playSound(SoundManager.BUTTON);
                if (idlePlayerListData.size() == 1){
                    Game.replayManager.startRecord();
                    Intent intent = new Intent(getApplicationContext(), LocalGamingActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "陈独秀请你找个位置坐下~", Toast.LENGTH_SHORT).show();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.soundManager.playSound(SoundManager.BUTTON);
                startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
            }
        });


        site[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.soundManager.playSound(SoundManager.BUTTON);
                chooseSite(0);
            }
        });

        site[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.soundManager.playSound(SoundManager.BUTTON);
                chooseSite(1);
            }
        });

        site[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.soundManager.playSound(SoundManager.BUTTON);
                chooseSite(2);
            }
        });

        site[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.soundManager.playSound(SoundManager.BUTTON);
                chooseSite(3);
            }
        });

        addRobotButton[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRobot(0);
            }
        });

        addRobotButton[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRobot(1);
            }
        });

        addRobotButton[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRobot(2);
            }
        });

        addRobotButton[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRobot(3);
            }
        });

        // 初始化数据
        siteState[0] = -1;
        siteState[1] = -1;
        siteState[2] = -1;
        siteState[3] = -1;

        HashMap<String, String> map = new HashMap<>();
        map.put("name", "Name");
        map.put("score", "Score");
        idlePlayerListData.addLast(map);
        HashMap<String, String> map2 = new HashMap<>();
        map2.put("name", "Me");
        map2.put("score", String.valueOf(Game.dataManager.getScore()));
        idlePlayerListData.addLast(map2);
        idlePlayerListAdapter.notifyDataSetChanged();

        //添加玩家数据
        Game.dataManager.setGameMode(DataManager.GM_LOCAL);//set game mode
        Game.dataManager.setMyId("0");
        Game.dataManager.setHostId("0");
        Game.playersData.clear();
        Game.playersData.put("0", new Role("0", "ME", String.valueOf(Game.dataManager.getScore()) ,-1, Role.PLAYER, true));
        Game.playersData.get(Game.dataManager.getMyId()).type = Role.ME;
    }

    private void chooseSite(int color) {
        if (siteState[color] == -1) {
            if (Game.playersData.get(Game.dataManager.getMyId()).color == ChessBoard.COLOR_Z) {
                idlePlayerListData.removeLast();
                idlePlayerListAdapter.notifyDataSetChanged();
            } else {
                site[Game.playersData.get(Game.dataManager.getMyId()).color].setText("JOIN");
                siteState[Game.playersData.get(Game.dataManager.getMyId()).color] = -1;
            }
            Game.playersData.get(Game.dataManager.getMyId()).color = color;
            site[color].setText("ME");
            siteState[color] = 1;
        } else {
            Toast.makeText(getApplicationContext(), "座位已被占!", Toast.LENGTH_SHORT).show();
        }
    }

    private void addRobot(int color) {
        if (siteState[color] == 1) {
            Toast.makeText(getApplicationContext(), "添加AI失败!", Toast.LENGTH_SHORT).show();
        } else if (siteState[color] == -1) {
            site[color].setText("AI");
            siteState[color] = 0;
            addRobotButton[color].setText("-");
            Game.playersData.put(String.format("%d", -color - 1), new Role(String.format("%d", -color - 1), "AI", "0", color, Role.ROBOT, false));
        } else if (siteState[color] == -0) {
            site[color].setText("JOIN");
            siteState[color] = -1;
            addRobotButton[color].setText("+");
            Game.playersData.remove(String.format("%d", -color - 1));
        }
    }
    ///////////////////////// 常规操作 /////////////////////////
    @Override
    public void onStart() {
        super.onStart();
        Game.soundManager.resumeMusic(SoundManager.BACKGROUND);
    }
    @Override
    public void onStop() {
        super.onStop();
        Game.soundManager.pauseMusic();
    }
}
