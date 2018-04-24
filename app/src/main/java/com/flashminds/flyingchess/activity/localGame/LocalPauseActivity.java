package com.flashminds.flyingchess.activity.localGame;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.flashminds.flyingchess.R;
import com.flashminds.flyingchess.activity.ChooseModeActivity;
import com.flashminds.flyingchess.Global;
import com.flashminds.flyingchess.util.BaseActivity;

/**
 * Created by IACJ on 2018/4/9.
 */
public class LocalPauseActivity extends BaseActivity {
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
        resume.setTypeface(Global.getFont());
        robot.setTypeface(Global.getFont());
        exit.setTypeface(Global.getFont());

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
            Global.localGameManager.gameOver();
            Global.replayManager.closeRecord();
            Global.replayManager.clearRecord();
            startActivity(new Intent(getApplicationContext(), ChooseModeActivity.class));
            Global.dataManager.giveUp(false);
            }
        });
        if (Global.dataManager.isGiveUp()) {
            robot.setText("取消托管");
        }
    }
}
