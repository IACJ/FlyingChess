package com.scut.flyingchess.activity.lanGame;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.scut.flyingchess.R;
import com.scut.flyingchess.dataPack.DataPack;
import com.scut.flyingchess.dataPack.Target;
import com.scut.flyingchess.Global;
import com.scut.flyingchess.manager.DataManager;
import com.scut.flyingchess.manager.SoundManager;
import com.scut.flyingchess.activity.BaseActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Create by IACJ on 2018/4/22
 */

public class LanHallActivity extends BaseActivity implements Target {
    Button createButton, btnRefresh, backButton;
    TextView title;
    ListView roomListView;
    SimpleAdapter roomListAdapter;
    LinkedList<HashMap<String, String>> roomListData;
    Worker worker = new Worker();
    String roomId  = "";
    int roomIndex = -1;

    LinkedList<String> roomUUID = new LinkedList<>();

    private static final String TAG = "LanHallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ui setting
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hall);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);//Activity切换动画
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Global.activityManager.add(this);
        Global.soundManager.playMusic(SoundManager.BACKGROUND);
        Global.dataManager.setGameMode(DataManager.GM_LAN);
        Global.dataManager.setMyName(new Build().MODEL);

        // 查找 View
        createButton = (Button) findViewById(R.id.create);
        backButton = (Button) findViewById(R.id.back);
        btnRefresh = (Button) findViewById(R.id.refresh);

        title = (TextView) findViewById(R.id.room_title);

        roomListView = (ListView) findViewById(R.id.roomList);

        roomListData = new LinkedList<>();
        String[] t = {"room", "id", "player", "state"};
        int[] t2 = {R.id.roomName, R.id.roomId, R.id.player, R.id.roomState};
        roomListAdapter = new SimpleAdapter(getApplicationContext(), roomListData, R.layout.content_room_list_item, t, t2);
        roomListView.setAdapter(roomListAdapter);


        // 点击事件
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);

                new Thread() {
                    @Override
                    public void run() {
                        try{
                            Global.localServer.startHost();
                            Global.socketManager.connectToLocalServer();
                            Global.socketManager.send(DataPack.R_LOGIN, new Build().MODEL);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }.start();


            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                goBack();
            }
        });
        roomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (roomUUID.size() == 0){
                    Toast.makeText(LanHallActivity.this,"请选择可进入的房间",Toast.LENGTH_SHORT).show();
                    return;
                }
                view.setSelected(true);
                String roomId = roomUUID.get(position);
                Global.socketManager.connectLanServer(Global.localServer.getRoomIp(roomId));
                Global.delay(500);
                Global.socketManager.send(DataPack.R_LOGIN, new Build().MODEL);
            }
        });
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.soundManager.playSound(SoundManager.BUTTON);
                Global.localServer.updateRoomListImmediately();

            }
        });

        // 设置字体
        title.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/comici.ttf"));
        btnRefresh.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/comici.ttf"));
        createButton.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/comici.ttf"));
    }

    @Override
    public void onStart() {
        super.onStart();
        Global.soundManager.resumeMusic(SoundManager.BACKGROUND);

        Global.socketManager.registerActivity(DataPack.A_ROOM_LOOKUP, this);
        Global.socketManager.registerActivity(DataPack.A_ROOM_CREATE, this);
        Global.socketManager.registerActivity(DataPack.A_ROOM_ENTER, this);

        Global.localServer.startListen();
        Global.localServer.registerMsg(this);

        new Thread(worker).start();
    }

    @Override
    public void onStop() {
        super.onStop();
        Global.soundManager.pauseMusic();

        worker.stop();
        Global.localServer.stopListen();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {//返回按钮
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                goBack();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void goBack() {
        finish();
    }

    @Override
    public void processDataPack(DataPack dataPack) {
        switch (dataPack.getCommand()){
            case DataPack.A_ROOM_LOOKUP : {
                synchronized (roomListData) {
                    roomListData.clear();
                    roomUUID.clear();
                    for (int i = 0; i < dataPack.getMessageList().size(); ) {
                        HashMap<String, String> data = new HashMap<>();
                        String str = dataPack.getMessage(i);
                        roomUUID.addLast(str);
                        data.put("room", dataPack.getMessage(i + 1));
                        if (str.length() > 3) {
                            data.put("id", str.substring(str.length() - 3));
                        } else {
                            data.put("id", str);
                        }
                        data.put("player", dataPack.getMessage(i + 2));
                        if (dataPack.getMessage(i + 3).compareTo("0") == 0) {
                            data.put("state", "等待开始");
                        } else {
                            data.put("state", "游戏中");
                        }
                        roomListData.addLast(data);
                        i += 4;
                    }
                    roomListView.post(new Runnable() {
                        @Override
                        public void run() {
                            roomListAdapter.notifyDataSetChanged();
                        }
                    });
                }
                break;
            }
            case DataPack.A_ROOM_ENTER :{

                if (dataPack.isSuccessful()) {
                    Global.dataManager.setMyId(dataPack.getMessage(0));
                    Global.dataManager.setRoomId(roomId);
                    Intent intent = new Intent(getApplicationContext(), LanRoomActivity.class);
                    ArrayList<String> msgs = new ArrayList<>(dataPack.getMessageList());
                    msgs.remove(0);
                    intent.putStringArrayListExtra("msgs", msgs);
                    startActivity(intent);
                } else {
                    createButton.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "加入房间失败!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            }
            default:{
                createButton.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "未知错误！", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
    class Worker implements Runnable {
        private boolean running;

        @Override
        public void run() {
            running = true;
            while (running){
                try {
                    Global.localServer.updateRoomListImmediately();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stop(){
            running = false;
        }
    }
}

