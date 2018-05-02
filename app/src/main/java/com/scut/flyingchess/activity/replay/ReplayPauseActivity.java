package com.scut.flyingchess.activity.replay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.scut.flyingchess.R;
import com.scut.flyingchess.activity.ChooseModeActivity;
import com.scut.flyingchess.activity.GameInfoActivity;
import com.scut.flyingchess.dataPack.DataPack;
import com.scut.flyingchess.Global;
import com.scut.flyingchess.manager.DataManager;
import com.scut.flyingchess.activity.BaseActivity;

public class ReplayPauseActivity extends BaseActivity {
    Button resume, robot, exit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ui setting
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pause);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);//Activity切换动画
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //init
        resume = (Button) findViewById(R.id.resume);
        robot = (Button) findViewById(R.id.robot);
        exit = (Button) findViewById(R.id.exit);
        //trigger
        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        robot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Global.dataManager.isGiveUp()) {
                    robot.setText("Cancel auto");
                    Global.dataManager.giveUp(true);
                } else {
                    robot.setText("Auto");
                    Global.dataManager.giveUp(false);
                }
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Global.dataManager.getGameMode() == DataManager.GM_WLAN) {
                    Global.socketManager.send(DataPack.R_GAME_EXIT, Global.dataManager.getMyId(), Global.dataManager.getRoomId());
                }
                if (Global.replayManager.isReplay == false) {
                    Global.replayManager.closeRecord();
                    Global.replayManager.clearRecord();
                }
                Global.replayGameManager.gameOver();
                Global.replayManager.stopReplay();
                if (Global.dataManager.getGameMode() != DataManager.GM_LOCAL) {
                    startActivity(new Intent(getApplicationContext(), GameInfoActivity.class));
                    if (Global.dataManager.getGameMode() == DataManager.GM_LAN) {
                        Global.localServer.stopHost();
                    }
                } else {
                    startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
                }
                Global.dataManager.giveUp(false);
            }
        });
        if (Global.dataManager.isGiveUp()) {
            robot.setText("Cancel auto");
        }
        resume.setTypeface(Global.getFont());
        robot.setTypeface(Global.getFont());
        exit.setTypeface(Global.getFont());
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
