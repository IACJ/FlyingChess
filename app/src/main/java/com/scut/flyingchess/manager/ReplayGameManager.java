package com.scut.flyingchess.manager;

import android.os.Bundle;
import android.os.Message;

import com.scut.flyingchess.activity.replay.ReplayGameActivity;
import com.scut.flyingchess.Global;
import com.scut.flyingchess.entity.Role;

/**
 * Created by IACJ on 2018/4/15.
 */
public class ReplayGameManager  {//game process control
    private ReplayGameWorker gw;//thread
    private ReplayGameActivity board;
    private boolean finished;
    private int dice, whichPlane;

    public void startGame(ReplayGameActivity board) {//call by activity when game start
        Global.chessBoard.init();
        this.board = board;
        finished = false;
        gw = new ReplayGameWorker();
        new Thread(gw).start();
    }

    public void gameOver() {
        Global.soundManager.stopMusic(SoundManager.GAME);
        gw.exit();
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


            dice = Global.replayManager.getSavedDice();
            role.setDice(dice);
            Global.soundManager.playSound(SoundManager.DICE);
            diceAnimate(dice);
            Global.delay(200);

            boolean canFly = false;
            if (role.canIMove()) {
                whichPlane = Global.replayManager.getSavedWhichPlane();
                role.setWhichPlane(whichPlane);
                role.move();
                canFly = true;
            } else if (role.type == Role.ME) {
                toast("此回合不能移动");
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
        System.out.println(id+" id");
        System.out.println(Global.dataManager.getMyId()+" Global.dataManager.getMyId()");
        if (win) {
            if (Integer.valueOf(id) < 0) {
                String[] s = {"Red", "Green", "Blue", "Yellow"};
                toast(s[color] + " robot is the winner!");
            } else if (id.equals(Global.dataManager.getMyId()) ) {//我赢了
                toast("I am the winner!");
            } else {//player
                toast("player" + Global.playersData.get(id).name + "is the winner!");
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

    class ReplayGameWorker implements Runnable {
        private boolean run;

        public ReplayGameWorker() {
            run = true;
        }

        @Override
        public void run() {
            run = true;
            int i = 0;
            while (run) {//control round
                i = (i % 4);//轮询颜色
                Global.replayGameManager.turnTo(i);
                if (ReplayGameManager.this.dice != 6){
                    i++;
                }
            }
        }
        public void exit() {
            run = false;
        }
    }
}

