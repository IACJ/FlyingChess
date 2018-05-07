package com.scut.flyingchess.manager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.scut.flyingchess.Global;
import com.scut.flyingchess.activity.lanGame.LanGamingActivity;
import com.scut.flyingchess.activity.lanGame.LanHallActivity;
import com.scut.flyingchess.dataPack.DataPack;
import com.scut.flyingchess.dataPack.Target;
import com.scut.flyingchess.entity.Role;

import java.util.LinkedList;

/**
 * Created by IACJ on 2018/4/28.
 */
public class LanGameManager implements Target {//game process control
    private GameWorker gw;//thread
    private LanGamingActivity board;
    private boolean finished;
    private int dice, whichPlane;

    public LanGameManager() {
        Global.socketManager.registerActivity(DataPack.E_GAME_PROCEED_PLANE, this);
        Global.socketManager.registerActivity(DataPack.E_GAME_PROCEED_DICE, this);
        Global.socketManager.registerActivity(DataPack.E_GAME_FINISHED, this);
        Global.socketManager.registerActivity(DataPack.E_GAME_PLAYER_DISCONNECTED, this);
    }

    public void startGame(LanGamingActivity board) {//call by activity when game start
        Global.chessBoard.init();
        this.board = board;
        finished = false;
        gw = new GameWorker();
        new Thread(gw).start();

    }

    public void gameOver() {
        Global.soundManager.stopMusic(SoundManager.GAME);
        gw.stop();
    }

    public void turnTo(int color) {//call by other thread  be careful
        Role role = null;
        for (String key : Global.playersData.keySet()) {
            if (Global.playersData.get(key).color == color) {
                role = Global.playersData.get(key);
            }
        }
        if (role != null) {//此颜色有玩家

            Message msg = new Message();
            Bundle b = new Bundle();
            b.putInt("color", color);
            msg.setData(b);
            msg.what = 6;
            board.handler.sendMessage(msg);

            whichPlane = -1;

            dice = role.roll();
            Global.replayManager.saveDice(dice);


            if ((role.offline || role.type == Role.AI) && Global.dataManager.getHostId().compareTo(Global.dataManager.getMyId()) == 0 || role.type == Role.ME) {
                Global.socketManager.send(DataPack.R_GAME_PROCEED_DICE, role.id, Global.dataManager.getRoomId(), dice);
            }

            Global.soundManager.playSound(SoundManager.DICE);
            diceAnimate(dice);

            boolean canFly = false;
            if (role.canIMove()) {
                do {
                    whichPlane = role.choosePlane();
                } while (!role.move());
                canFly = true;
                Global.replayManager.saveWhichPlane(whichPlane);
            } else if (role.type == Role.ME) {
                toast("没有棋子可以移动，本回合被跳过。");

            }

            if ((role.offline || role.type != Role.PLAYER) && Global.dataManager.getHostId().compareTo(Global.dataManager.getMyId()) == 0 || role.type == Role.ME) {
                Global.socketManager.send(DataPack.R_GAME_PROCEED_PLANE, role.id, Global.dataManager.getRoomId(), whichPlane);
            }

            if (canFly) {
                flyNow(color, whichPlane);
                amIWin(role.id, color);
            }
            Global.delay(200);
        }
    }

    private void amIWin(String id, int color) {
        boolean win = true;
        for (int i = 0; i < 4; i++) {
            if (Global.chessBoard.getAirplane(color).position[i] != -2) {
                win = false;
            }
        }
        if (win) {
            if (Integer.valueOf(id) < 0) {
                String[] s = {"Red", "Green", "Blue", "Yellow"};
                toast(s[color] + " robot is the winner!");
                if (Global.dataManager.getGameMode() == DataManager.GM_WLAN && Global.dataManager.getHostId().compareTo(Global.dataManager.getMyId()) == 0) {//我是房主
                    LinkedList<String> msgs = new LinkedList<>();
                    msgs.addLast(id);
                    msgs.addLast(Global.dataManager.getRoomId());
                    msgs.addLast("AI");
                    Global.socketManager.send(new DataPack(DataPack.R_GAME_FINISHED, msgs));
                }
            } else if (id.compareTo(Global.dataManager.getMyId()) == 0) {//我赢了
                toast("胜利!");
                if (Global.dataManager.getGameMode() == DataManager.GM_WLAN) {
                    LinkedList<String> msgs = new LinkedList<>();
                    msgs.addLast(id);
                    msgs.addLast(Global.dataManager.getRoomId());
                    msgs.addLast(Global.dataManager.getMyName());
                    Global.socketManager.send(new DataPack(DataPack.R_GAME_FINISHED, msgs));
                }
            } else {//player
                toast("player" + Global.playersData.get(id).name + "取得了胜利!");
                if (Global.dataManager.getGameMode() == DataManager.GM_WLAN && Global.playersData.get(id).offline && Global.dataManager.getHostId().compareTo(Global.dataManager.getMyId()) == 0) {//掉线且我是房主
                    LinkedList<String> msgs = new LinkedList<>();
                    msgs.addLast(id);
                    msgs.addLast(id);
                    msgs.addLast(Global.playersData.get(id).name);
                    Global.socketManager.send(new DataPack(DataPack.R_GAME_FINISHED, msgs));
                }
            }
            Global.dataManager.setWinner(id);
            gameOver();
            Message msg = new Message();
            msg.what = 5;
            board.handler.sendMessage(msg);
        }
    }

    private void diceAnimate(int dice) {
        for (int i = 0; i < 6; i++) {
            Message msg = new Message();
            Bundle b = new Bundle();
            b.putInt("dice", Global.chessBoard.getDice().roll());
            msg.setData(b);
            msg.what = 7;
            board.handler.sendMessage(msg);
            Global.delay(150);
        }
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("dice", dice);
        msg.setData(b);
        msg.what = 2;
        board.handler.sendMessage(msg);
        Global.delay(600);
    }

    private void planeAnimate(int color, int pos) {
        Message msg2 = new Message();
        Bundle b2 = new Bundle();
        b2.putInt("color", color);
        b2.putInt("whichPlane", whichPlane);
        b2.putInt("pos", pos);
        msg2.setData(b2);
        msg2.what = 1;
        board.handler.sendMessage(msg2);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void planeCrash(int color, int crashPlane) {
        Global.soundManager.playSound(SoundManager.FLY_CRASH);
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("color", color);
        b.putInt("whichPlane", crashPlane);
        b.putInt("pos", Global.chessBoard.getAirplane(color).position[crashPlane]);
        msg.setData(b);
        //Global.logManager.p("planeCrash:","color:",color,"crashplane:",crashPlane,"position:",Global.chessBoard.getAirplane(color).position[crashPlane]);
        msg.what = 4;
        board.handler.sendMessage(msg);
    }

    private void flyNow(int color, int whichPlane) {
        int toPos = Global.chessBoard.getAirplane(color).position[whichPlane];
        int curPos = Global.chessBoard.getAirplane(color).lastPosition[whichPlane];
        if (curPos + dice == toPos || curPos == -1) {
            for (int pos = curPos + 1; pos <= toPos; pos++) {
                Global.soundManager.playSound(SoundManager.FLY_SHORT);
                planeAnimate(color, pos);
            }
            crash(color, toPos, whichPlane);
        } else if (curPos + dice + 4 == toPos) { // short jump
            for (int pos = curPos + 1; pos <= curPos + dice; pos++) {
                Global.soundManager.playSound(SoundManager.FLY_SHORT);
                planeAnimate(color, pos);
            }
            crash(color, curPos + dice, whichPlane);
            Global.soundManager.playSound(SoundManager.FLY_MID);
            planeAnimate(color, toPos);
            crash(color, toPos, whichPlane);
        } else if (toPos == 30) { // short jump and then long jump
            for (int pos = curPos + 1; pos <= curPos + dice; pos++) {
                Global.soundManager.playSound(SoundManager.FLY_SHORT);
                planeAnimate(color, pos);
            }
            crash(color, curPos + dice, whichPlane);
            Global.soundManager.playSound(SoundManager.FLY_MID);
            planeAnimate(color, 18);
            crash(color, 18, whichPlane);
            Global.soundManager.playSound(SoundManager.FLY_LONG);
            planeAnimate(color, 30);
            crash(color, 30, whichPlane);
        } else if (toPos == 34) { // long jump and then short jump
            for (int pos = curPos + 1; pos <= 18; pos++) {
                Global.soundManager.playSound(SoundManager.FLY_SHORT);
                planeAnimate(color, pos);
            }
            crash(color, 18, whichPlane);
            Global.soundManager.playSound(SoundManager.FLY_LONG);
            planeAnimate(color, 30);
            crash(color, 30, whichPlane);
            Global.soundManager.playSound(SoundManager.FLY_MID);
            planeAnimate(color, 34);
            crash(color, 34, whichPlane);
        } else if (Global.chessBoard.isOverflow()) { // overflow
            for (int pos = curPos + 1; pos <= 56; pos++) {
                Global.soundManager.playSound(SoundManager.FLY_SHORT);
                planeAnimate(color, pos);
            }
            for (int pos = 55; pos >= toPos; pos--) {
                Global.soundManager.playSound(SoundManager.FLY_SHORT);
                planeAnimate(color, pos);
            }
            crash(color, toPos, whichPlane);
            Global.chessBoard.setOverflow(false);
        } else if (toPos == -2) {
            for (int pos = curPos + 1; pos <= 55; pos++) {
                Global.soundManager.playSound(SoundManager.FLY_SHORT);
                planeAnimate(color, pos);
            }
            Global.soundManager.playSound(SoundManager.ARRIVE);
            planeAnimate(color, 56);
        }
    }

    public void crash(int color, int pos, int whichPlane) {

        if (pos >= 50)//不被人撞
            return;
        int crashColor = color;
        int crashPlane = whichPlane;
        int count = 0;
        int curX = Global.chessBoard.map[color][pos][0];
        int curY = Global.chessBoard.map[color][pos][1];
        for (int i = 0; i < 4; i++) {
            if (i != color) {

                for (int j = 0; j < 4; j++) {
                    int crashPos = Global.chessBoard.getAirplane(i).position[j];

                    if (crashPos > 0) {
                        if (Global.chessBoard.map[i][crashPos][0] == curX && Global.chessBoard.map[i][crashPos][1] == curY) {//撞别人

                            crashPlane = j;
                            count++;
                        }
                    }
                }
                if (count == 1) {
                    crashColor = i;
                    break;
                }
                if (count > 1) {
                    crashPlane = whichPlane;
                    break;
                }
            }
        }
        if (count >= 1) {
            planeCrash(crashColor, crashPlane);
            Global.chessBoard.getAirplane(crashColor).position[crashPlane] = -1;
            Global.chessBoard.getAirplane(crashColor).lastPosition[crashPlane] = -1;
        }
    }


    private void toast(String msgs) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("msg", msgs);
        msg.setData(b);
        msg.what = 3;
        board.handler.sendMessage(msg);
    }

    @Override
    public void processDataPack(DataPack dataPack) {
        switch (dataPack.getCommand()) {
            case DataPack.E_GAME_FINISHED:
                finished = true;
                break;
            case DataPack.E_GAME_PROCEED_DICE:
                if ((Integer.valueOf(dataPack.getMessage(0)) < 0 || Global.playersData.get(dataPack.getMessage(0)).offline) && Global.dataManager.getMyId().compareTo(Global.dataManager.getHostId()) != 0 || Global.playersData.get(dataPack.getMessage(0)).type == Role.PLAYER) {//机器人且我不是房主  或者 是玩家且没有掉线 或者 是玩家且掉线当我不是房主
                    Global.playersData.get(dataPack.getMessage(0)).setDiceValid(Integer.valueOf(dataPack.getMessage(2)));
                    //Global.logManager.p("processDataPack:","dice:",dataPack.getMessage(2));
                }
                break;
            case DataPack.E_GAME_PROCEED_PLANE:
                if ((Integer.valueOf(dataPack.getMessage(0)) < 0 || Global.playersData.get(dataPack.getMessage(0)).offline) && Global.dataManager.getMyId().compareTo(Global.dataManager.getHostId()) != 0 || Global.playersData.get(dataPack.getMessage(0)).type == Role.PLAYER) {//机器人且我不是房主  或者 是玩家且没有掉线 或者 是玩家且掉线当我不是房主
                    if (Integer.valueOf(dataPack.getMessage(2)) >= 0) {
                        Global.playersData.get(dataPack.getMessage(0)).setPlaneValid(Integer.valueOf(dataPack.getMessage(2)));
                    }
                    //Global.logManager.p("processDataPack:", "plane:", dataPack.getMessage(2));
                }
                break;
            case DataPack.E_GAME_PLAYER_DISCONNECTED:
                if (dataPack.getMessage(0).compareTo(Global.dataManager.getMyId()) != 0) {//不是我
                    if (dataPack.getMessage(0).compareTo(Global.dataManager.getHostId()) == 0) {//是房主  退出游戏
                        toast("房主离开了游戏");
                        gameOver();
                        board.startActivity(new Intent(board.getApplicationContext(), LanHallActivity.class));
                        Global.dataManager.giveUp(false);
                    } else {//由电脑托管
                        Global.playersData.get(dataPack.getMessage(0)).offline = true;
                    }
                }
                break;
        }
    }
    class GameWorker implements Runnable {
        private boolean running;

        @Override
        public void run() {
            running = true;
            int i = 0;
            while (running) {//control round
                Global.lanGameManager.turnTo(i);
                if (LanGameManager.this.dice != 6){
                    i = (i+1)%4;
                }
            }
        }

        public void stop() {
            running = false;
        }
    }
}

