package com.scut.flyingchess.activity.replay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.scut.flyingchess.R;
import com.scut.flyingchess.activity.ChooseModeActivity;
import com.scut.flyingchess.Global;
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
                    robot.setText("取消托管");
                    Global.dataManager.giveUp(true);
                } else {
                    robot.setText("托管");
                    Global.dataManager.giveUp(false);
                }
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.replayManager.stopReplay();
                Global.replayGameManager.gameOver();
                startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
            }
        });
        if (Global.dataManager.isGiveUp()) {
            robot.setText("取消托管");
        }
        robot.setVisibility(View.INVISIBLE);
        resume.setTypeface(Global.getFont());
        robot.setTypeface(Global.getFont());
        exit.setTypeface(Global.getFont());
    }
}
