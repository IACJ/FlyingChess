package com.scut.flyingchess.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.scut.flyingchess.Global;
import com.scut.flyingchess.R;
import com.scut.flyingchess.manager.SoundManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class GameEndActivity extends BaseActivity {
    Button con;
    TextView winner;
    SimpleAdapter playerListAdapter;
    ListView playerListView;
    LinkedList<HashMap<String, String>> playerListData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_end);
        //init
        con = (Button) findViewById(R.id.con);
        winner = (TextView) findViewById(R.id.winner);
        playerListData = new LinkedList<>();
        playerListAdapter = new SimpleAdapter(getApplicationContext(), playerListData, R.layout.content_game_end_player_list_item, new String[]{"name", "state", "action", "score"}, new int[]{R.id.name, R.id.state, R.id.action, R.id.score});
        playerListView = (ListView) findViewById(R.id.listView);
        //trigget
        con.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //settings
        if (Integer.valueOf(Global.dataManager.getLastWinner()) < 0)
            winner.setText("AI 赢得胜利~");
        ArrayList<String> msgs = getIntent().getExtras().getStringArrayList("msgs");
        for (int i = 0; i < msgs.size(); i += 4) {
            HashMap<String, String> map = new HashMap<>();
            map.put("name", msgs.get(i + 1));
            if (Global.dataManager.getLastWinner().compareTo(msgs.get(i)) == 0) {
                map.put("state", "胜利！");
                map.put("action", "+10");
                winner.setText(msgs.get(i + 1) + " win the game~");
            } else {
                map.put("state", "失败");
                map.put("action", "-5");
            }
            if (Global.playersData.get(msgs.get(i)).offline) {
                map.put("state", "掉线");
                map.put("action", "0");
            }
            map.put("score", msgs.get(i + 2));
            playerListData.addLast(map);
        }
        playerListView.setAdapter(playerListAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Global.soundManager.resumeMusic(SoundManager.BACKGROUND);
    }
}
