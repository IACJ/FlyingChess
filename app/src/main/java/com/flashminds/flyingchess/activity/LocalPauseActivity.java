package com.flashminds.flyingchess.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.flashminds.flyingchess.R;
import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.entity.Game;
import com.flashminds.flyingchess.manager.DataManager;

/**
 * Created by IACJ on 2018/4/9.
 */
public class LocalPauseActivity extends Activity {
    Button resume, robot, exit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pause);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);//Activity切换动画
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 查找view
        resume = (Button) findViewById(R.id.resume);
        robot = (Button) findViewById(R.id.robot);
        exit = (Button) findViewById(R.id.exit);

        // 设置字体
        resume.setTypeface(Game.getFont());
        robot.setTypeface(Game.getFont());
        exit.setTypeface(Game.getFont());

        // 按钮事件
        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        robot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (!Game.dataManager.isGiveUp()) {
                robot.setText("取消托管");
                Game.dataManager.giveUp(true);
            } else {
                robot.setText("托管");
                Game.dataManager.giveUp(false);
            }
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Game.localGameManager.gameOver();
            Game.replayManager.stopReplay();
            startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
            Game.dataManager.giveUp(false);
            }
        });
        if (Game.dataManager.isGiveUp()) {
            robot.setText("取消托管");
        }
    }
}
