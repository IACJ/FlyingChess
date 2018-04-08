package com.flashminds.flyingchess.manager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.flashminds.flyingchess.activity.ChessBoardActivity;
import com.flashminds.flyingchess.activity.GameInfoActivity;
import com.flashminds.flyingchess.activity.LocalGamingActivity;
import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.dataPack.Target;
import com.flashminds.flyingchess.entity.Game;
import com.flashminds.flyingchess.entity.Role;

import java.util.LinkedList;

/**
 * Created by IACJ on 2018/4/9.
 */
public class LocalGameManager {//game process control
    private LocalGameWorker gw;//thread
    private LocalGamingActivity board;
    private boolean finished;
    private int dice, whichPlane;

    private class LocalGameWorker implements Runnable {
        private boolean run;

        public LocalGameWorker() {
            run = true;
        }

        @Override
        public void run() {
            run = true;
            int i = 0;
            while (run) {//control round
                i = (i % 4);//轮询颜色
                LocalGameManager.this.turnTo(i);
                if (LocalGameManager.this.dice != 6){
                    i++;
                }
            }
        }
        public void exit() {
            run = false;
        }
    }

    public void startGame(LocalGamingActivity board) {//call by activity when game start
        Game.chessBoard.init();
        this.board = board;
        finished = false;
        gw = new LocalGameWorker();
        new Thread(gw).start();
    }

    public void gameOver() {
        gw.exit();
    }

    public void turnTo(int color) {//call by other thread  be careful
        Role role = null;
        for (String key : Game.playersData.keySet()) {
            if (Game.playersData.get(key).color == color) {
                role = Game.playersData.get(key);
            }
        }
        if (role == null){//此颜色没有玩家
            return;
        }

        whichPlane = -1;
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("color", color);
        msg.setData(b);
        msg.what = 6;
        board.handler.sendMessage(msg);

        dice = role.roll();

        Game.replayManager.saveDice(dice);
        Game.soundManager.playSound(SoundManager.DICE);
        diceAnimate(dice);
        if (role.canIMove()) {
            do {
                whichPlane = role.choosePlane();
            } while (!role.move());
            Game.replayManager.saveWhichPlane(whichPlane);
            flyNow(color, whichPlane);
            amIWin(role.id, color);
        } else if (role.type == Role.ME) {
            toast("本回合不能移动~");
        }
    }

    private void amIWin(String id, int color) {
        boolean win = true;
        for (int i = 0; i < 4; i++) {
            if (Game.chessBoard.getAirplane(color).position[i] != -2) {
                win = false;
            }
        }
        if (win) {
            if (Integer.valueOf(id) < 0) {
                String[] s = {"红方", "绿方", "蓝方", "黄方"};
                toast(s[color] + " AI 获胜!");
            } else if (id.compareTo(Game.dataManager.getMyId()) == 0) {//我赢了
                toast("噫！好了！我赢了!");
            }
            Game.dataManager.setWinner(id);
            gameOver();
            Message msg = new Message();
            msg.what = 5;
            board.handler.sendMessage(msg);
        }
    }

    private void diceAnimate(int dice) {
        for (int i = 0; i < 5; i++) {
            Message msg = new Message();
            Bundle b = new Bundle();
            b.putInt("dice", Game.chessBoard.getDice().roll());
            msg.setData(b);
            msg.what = 2;
            board.handler.sendMessage(msg);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("dice", dice);
        msg.setData(b);
        msg.what = 2;
        board.handler.sendMessage(msg);
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
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void planeCrash(int color, int crashPlane) {
        Game.soundManager.playSound(SoundManager.FLYCRASH);
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("color", color);
        b.putInt("whichPlane", crashPlane);
        b.putInt("pos", Game.chessBoard.getAirplane(color).position[crashPlane]);
        msg.setData(b);
        msg.what = 4;
        board.handler.sendMessage(msg);
    }

    private void flyNow(int color, int whichPlane) { // 游戏规则——动画
        int toPos = Game.chessBoard.getAirplane(color).position[whichPlane];
        int curPos = Game.chessBoard.getAirplane(color).lastPosition[whichPlane];
        if (curPos + dice == toPos || curPos == -1) {
            for (int pos = curPos + 1; pos <= toPos; pos++) { // 直接走
                Game.soundManager.playSound(SoundManager.FLYSHORT);
                planeAnimate(color, pos);
            }
            crash(color, toPos, whichPlane);
        } else if (curPos + dice + 4 == toPos) { // 走完之后小跳
            for (int pos = curPos + 1; pos <= curPos + dice; pos++) {
                Game.soundManager.playSound(SoundManager.FLYSHORT);
                planeAnimate(color, pos);
            }
            crash(color, curPos + dice, whichPlane);
            Game.soundManager.playSound(SoundManager.FLYMID);
            planeAnimate(color, toPos);
            crash(color, toPos, whichPlane);
        } else if (toPos == 30) { // 走完之后小跳，然后飞
            for (int pos = curPos + 1; pos <= curPos + dice; pos++) {
                Game.soundManager.playSound(SoundManager.FLYSHORT);
                planeAnimate(color, pos);
            }
            crash(color, curPos + dice, whichPlane);
            Game.soundManager.playSound(SoundManager.FLYMID);
            planeAnimate(color, 18);
            crash(color, 18, whichPlane);
            Game.soundManager.playSound(SoundManager.FLYLONG);
            planeAnimate(color, 30);
            crash(color, 30, whichPlane);
        } else if (toPos == 34) { // 走玩之后飞，然后小跳
            for (int pos = curPos + 1; pos <= 18; pos++) {
                Game.soundManager.playSound(SoundManager.FLYSHORT);
                planeAnimate(color, pos);
            }
            crash(color, 18, whichPlane);
            Game.soundManager.playSound(SoundManager.FLYLONG);
            planeAnimate(color, 30);
            crash(color, 30, whichPlane);
            Game.soundManager.playSound(SoundManager.FLYMID);
            planeAnimate(color, 34);
            crash(color, 34, whichPlane);
        } else if (Game.chessBoard.isOverflow()) { // 溢出回退
            for (int pos = curPos + 1; pos <= 56; pos++) {
                Game.soundManager.playSound(SoundManager.FLYSHORT);
                planeAnimate(color, pos);
            }
            for (int pos = 55; pos >= toPos; pos--) {
                Game.soundManager.playSound(SoundManager.FLYSHORT);
                planeAnimate(color, pos);
            }
            crash(color, toPos, whichPlane);
            Game.chessBoard.setOverflow(false);
        } else if (toPos == -2) { // 胜利
            for (int pos = curPos + 1; pos <= 55; pos++) {
                Game.soundManager.playSound(SoundManager.FLYSHORT);
                planeAnimate(color, pos);
            }
            Game.soundManager.playSound(SoundManager.ARRIVE);
            planeAnimate(color, 56);
        }
    }

    public void crash(int color, int pos, int whichPlane) {
        if (pos >= 50)//不被人撞
            return;
        int crashColor = color;
        int crashPlane = whichPlane;
        int count = 0;
        int curX = Game.chessBoard.map[color][pos][0];
        int curY = Game.chessBoard.map[color][pos][1];
        for (int i = 0; i < 4; i++) {
            if (i != color) {
                for (int j = 0; j < 4; j++) {
                    int crashPos = Game.chessBoard.getAirplane(i).position[j];
                    if (crashPos > 0) {
                        if (Game.chessBoard.map[i][crashPos][0] == curX && Game.chessBoard.map[i][crashPos][1] == curY) {//撞别人
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
            Game.chessBoard.getAirplane(crashColor).position[crashPlane] = -1;
            Game.chessBoard.getAirplane(crashColor).lastPosition[crashPlane] = -1;
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
}


