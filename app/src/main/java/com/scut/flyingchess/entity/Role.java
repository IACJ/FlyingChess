package com.scut.flyingchess.entity;

import com.scut.flyingchess.Global;
import com.scut.flyingchess.dataPack.DataPack;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by karthur on 2016/5/10.
 */
public class Role {
    public final static int ROBOT = 0;
    public final static int PLAYER = 1;
    public final static int ME = 2;

    public String id;
    public String name;
    public String score;
    public int color;
    public int type;//0 ROBOT 1 PLAYER 2 ME
    public boolean isHost;
    public boolean offline;

    public boolean waitForDice, waitForPlane;

    private boolean isDiceValid, isPlaneValid, canRoll, canChoosePlane;
    private int dice, whichPlane;

    public Role(String id, String name, String score, int color, int type, boolean isHost) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.color = color;
        this.type = type;
        this.isHost = isHost;
        this.offline = false;

        isDiceValid = false;
        isPlaneValid = false;
        canRoll = false;
        canChoosePlane = false;
        waitForDice = true;
        waitForPlane = true;
    }

    public boolean canIMove() {// 能否移动
        if (dice == 6) {
            return true;
        } else {
            int[] p = Global.chessBoard.getAirplane(color).position;
            for (int i = 0; i < 4; i++) {
                if (p[i] >= 0)//-1 : 未起飞，-2 :已完成;
                    return true;
            }
        }
        return false;
    }

    public boolean move() { // 规则：计算
        if ((Global.chessBoard.getAirplane(color).position[whichPlane] == -1 && dice != 6) ||
                Global.chessBoard.getAirplane(color).position[whichPlane] == -2) { //不能移动
            return false;
        }
        if (Global.chessBoard.getAirplane(color).position[whichPlane] == -1) { //起飞
            Global.chessBoard.getAirplane(color).position[whichPlane] = 0;
            return true;
        }
        Global.chessBoard.getAirplane(color).lastPosition[whichPlane] = Global.chessBoard.getAirplane(color).position[whichPlane];
        int nextStep = Global.chessBoard.getAirplane(color).position[whichPlane] + dice;

        if (nextStep > 56) { // 移动溢出
            nextStep = 56 - (nextStep - 56);
            Global.chessBoard.setOverflow(true);
        } else if (nextStep == 56) { // 胜利
            nextStep = -2;
        } else if (nextStep == 18) { // 飞行后小跳
            nextStep = 34;
        } else if (nextStep < 50) {
            if ((nextStep - 2) % 4 == 0) { //小跳
                nextStep += 4;
                if (nextStep == 18){ // 小跳后飞行
                    nextStep = 30;
                }
            }
        }
        Global.chessBoard.getAirplane(color).position[whichPlane] = nextStep;
        return true;
    }

    public void setDiceValid(int dice) {
        if (Global.dataManager.getMyId().compareTo(id) == 0) {//ME
            if (canRoll && !Global.dataManager.isGiveUp()) {
                this.dice = Global.chessBoard.getDice().roll();
                isDiceValid = true;
            }
        } else {
            this.dice = dice;
            waitForDice = false;
        }
    }

    public void setDiceValid2(int dice) {
        if (Global.dataManager.getMyId().compareTo(id) == 0) {//ME
            if (canRoll && !Global.dataManager.isGiveUp()) {
                this.dice = dice;
                isDiceValid = true;
            }
        } else {
            this.dice = dice;
            waitForDice = false;
        }
    }

    public void setPlaneValid(int whichPlane) {
        if (Global.dataManager.getMyId().compareTo(id) == 0) {//ME
            if (canChoosePlane && !Global.dataManager.isGiveUp()) {
                this.whichPlane = whichPlane;
                isPlaneValid = true;
            }
        } else {
            this.whichPlane = whichPlane;
            waitForPlane = false;
        }
    }

    public void setDice(int dice) {
        this.dice = dice;
    }

    public void setWhichPlane(int whichPlane) {
        this.whichPlane = whichPlane;
    }

    public int roll() {
        switch (type) {
            case ME: {
                canRoll = true;
                isDiceValid = false;
                while (!isDiceValid) {//等待按下骰子
                    if (Global.dataManager.isGiveUp()) {//托管
                        canRoll = false;//防止产生随机数时  玩家按下骰子
                        isDiceValid = false;
                        dice = AIDice();
                        break;
                    }
                    Global.delay(200);
                }

                canRoll = false;
                isDiceValid = false;
                break;
            }
            case PLAYER: {
                while (waitForDice) {
                    if (offline && Global.dataManager.getHostId().compareTo(Global.dataManager.getMyId()) == 0) {//断线且我是房主
                        dice = AIDice();

                        break;
                    }
                    Global.delay(100);
                }
                waitForDice = true;

                break;
            }
            case ROBOT: {
                if (Global.dataManager.getMyId().compareTo(Global.dataManager.getHostId()) == 0) {//我是房主
                    dice = AIDice();
                } else {
                    while (waitForDice) {
                        Global.delay(100);
                    }
                    waitForDice = true;
                }

                break;
            }
        }
        return dice;
    }

    public int choosePlane() {
        switch (type) {
            case ME: {
                canChoosePlane = true;
                isPlaneValid = false;
                while (!isPlaneValid) {
                    if (Global.dataManager.isGiveUp()) {//托管
                        canChoosePlane = false;
                        isPlaneValid = false;
                        whichPlane = AIChoosePlane();
                        break;
                    }
                    Global.delay(200);
                }
                canChoosePlane = false;
                isPlaneValid = false;

                break;
            }
            case PLAYER: {
                while (waitForPlane) {
                    if (offline && Global.dataManager.getHostId().compareTo(Global.dataManager.getMyId()) == 0) {//断线且我是房主
                        whichPlane = AIChoosePlane();

                        break;
                    }
                    Global.delay(100);
                }
                waitForPlane = true;

                break;
            }
            case ROBOT: {
                if (Global.dataManager.getMyId().compareTo(Global.dataManager.getHostId()) == 0) {//我是房主
                    whichPlane = AIChoosePlane();
                } else {

                    while (waitForPlane) {
                        Global.delay(100);
                    }
                    waitForPlane = true;
                }

                break;
            }
        }
        return whichPlane;
    }

    private int AIDice() {
        Global.delay(500);
        Random r = new Random(System.currentTimeMillis());
        return r.nextInt(6) + 1;
    }

    private int AIChoosePlane() {
        Global.delay(500);
        Random r = new Random(System.currentTimeMillis());
        int whichPlane = -1;
        //rule 寻找可用飞机
        ArrayList<Integer> avaPlane = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (Global.chessBoard.getAirplane(color).position[i] >= 0) {
                avaPlane.add(i);
            } else if (Global.chessBoard.getAirplane(color).position[i] == -1 && dice == 6) {
                avaPlane.add(i);
            }
        }
        if (avaPlane.size() > 0) {
            whichPlane = avaPlane.get(r.nextInt(avaPlane.size()));
        }
        //rule 寻找没有靠近终点的飞机
        ArrayList<Integer> farAwayPlane = new ArrayList<>();
        for (int i = 0; i < avaPlane.size(); i++) {
            if (Global.chessBoard.getAirplane(color).position[avaPlane.get(i)] < 50)
                farAwayPlane.add(avaPlane.get(i));
        }
        if (farAwayPlane.size() > 0) {
            whichPlane = farAwayPlane.get(r.nextInt(farAwayPlane.size()));
        }
        //rule 寻找正好正好多走四步的飞机
        for (int i = 0; i < farAwayPlane.size(); i++) {
            if ((Global.chessBoard.getAirplane(color).position[farAwayPlane.get(i)] + 2 + dice) % 4 == 0) {
                whichPlane = farAwayPlane.get(i);
                break;
            }
        }
        //rule 寻找正好到达的飞机
        for (int i = 0; i < 4; i++) {
            if (56 - Global.chessBoard.getAirplane(color).position[i] == dice) {
                whichPlane = i;
                break;
            }
        }
        return whichPlane;
    }
}
