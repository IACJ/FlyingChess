package com.scut.flyingchess.activity.localGame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.scut.flyingchess.R;
import com.scut.flyingchess.activity.ChooseModeActivity;
import com.scut.flyingchess.entity.ChessBoard;
import com.scut.flyingchess.Global;
import com.scut.flyingchess.entity.Role;
import com.scut.flyingchess.manager.DataManager;
import com.scut.flyingchess.manager.SoundManager;
import com.scut.flyingchess.activity.BaseActivity;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by IACJ on 2018/4/4.
 *
 * 开始 Local 游戏前的房间设定
 */
public class LocalRoomActivity extends BaseActivity {
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
        Global.activityManager.add(this);
        Global.soundManager.playMusic(SoundManager.BACKGROUND);

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
        title.setTypeface(Global.getFont());
        for (int i = 0; i < 4; i++) {
            site[i].setTypeface(Global.getFont());
            addRobotButton[i].setTypeface(Global.getFont());
        }
        startButton.setTypeface(Global.getFont());

        // 按钮事件
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                if (idlePlayerListData.size() == 1){
                    Global.replayManager.startRecord();
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
                Global.soundManager.playSound(SoundManager.BUTTON);
                startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
            }
        });


        site[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                chooseSite(0);
            }
        });

        site[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                chooseSite(1);
            }
        });

        site[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                chooseSite(2);
            }
        });

        site[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
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
        map2.put("score", String.valueOf(Global.dataManager.getScore()));
        idlePlayerListData.addLast(map2);
        idlePlayerListAdapter.notifyDataSetChanged();

        //添加玩家数据
        Global.dataManager.setGameMode(DataManager.GM_LOCAL);//set game mode
        Global.dataManager.setMyId("0");
        Global.dataManager.setHostId("0");
        Global.playersData.clear();
        Global.playersData.put("0", new Role("0", "ME", String.valueOf(Global.dataManager.getScore()) ,-1, Role.PLAYER, true));
        Global.playersData.get(Global.dataManager.getMyId()).type = Role.ME;
    }

    private void chooseSite(int color) {
        if (siteState[color] == -1) {
            if (Global.playersData.get(Global.dataManager.getMyId()).color == ChessBoard.COLOR_Z) {
                idlePlayerListData.removeLast();
                idlePlayerListAdapter.notifyDataSetChanged();
            } else {
                site[Global.playersData.get(Global.dataManager.getMyId()).color].setText("JOIN");
                siteState[Global.playersData.get(Global.dataManager.getMyId()).color] = -1;
            }
            Global.playersData.get(Global.dataManager.getMyId()).color = color;
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
            Global.playersData.put(String.format("%d", -color - 1), new Role(String.format("%d", -color - 1), "AI", "0", color, Role.ROBOT, false));
        } else if (siteState[color] == -0) {
            site[color].setText("JOIN");
            siteState[color] = -1;
            addRobotButton[color].setText("+");
            Global.playersData.remove(String.format("%d", -color - 1));
        }
    }
    ///////////////////////// 常规操作 /////////////////////////
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
}
