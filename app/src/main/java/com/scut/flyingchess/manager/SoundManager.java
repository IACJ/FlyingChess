package com.scut.flyingchess.manager;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.scut.flyingchess.Global;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 背景音乐控制类
 * Created by BingF on 2016/4/26.
 * Edited by IACJ on 2018/5/7
 */
public class SoundManager {


    public static final int ARRIVE = 1;
    public static final int BACKGROUND = 2;
    public static final int BUTTON = 3;
    public static final int DICE = 4;
    public static final int FLY_CRASH = 5;
    public static final int FLY_LONG = 6;
    public static final int FLY_SHORT = 7;
    public static final int FLY_MID = 8;
    public static final int FLY_OUT = 9;
    public static final int WIN = 10;
    public static final int LOSE = 11;
    public static final int GAME = 12;

    private static final int DICE_1 = 101;
    private static final int DICE_2 = 102;
    private static final int DICE_3 = 103;
    private static final int DICE_4 = 104;
    private static final int DICE_5 = 105;
    private static final int DICE_6 = 106;

    private MediaPlayer mediaPlayer, bk, game;
    private AssetManager assetManager;
    private LinkedList<MediaPlayer> mediaPlayers;
    private Map<Integer, String> soundMap;

    private static final String TAG = "SoundManager";

    public SoundManager(AppCompatActivity activity) {
        bk = new MediaPlayer();
        game = new MediaPlayer();
        assetManager = activity.getAssets();
        mediaPlayers = new LinkedList<>();
        initMap();
    }

    private void initMap() {
        soundMap = new HashMap<>();
        soundMap.put(ARRIVE, "music/arrive.ogg");
        soundMap.put(BACKGROUND, "music/backgroundmusic.mp3");
        soundMap.put(BUTTON, "music/button.ogg");
        soundMap.put(DICE, "music/dice.ogg");
        soundMap.put(FLY_CRASH, "music/flycrash.ogg");
        soundMap.put(FLY_LONG, "music/flylong.ogg");
        soundMap.put(FLY_SHORT, "music/flyshort.ogg");
        soundMap.put(FLY_MID, "music/flymid.ogg");
        soundMap.put(WIN, "music/win.ogg");
        soundMap.put(LOSE, "music/lose.ogg");
        soundMap.put(GAME, "music/gamemusic.mp3");

        soundMap.put(DICE_1,"music/DICE_1.wav");
        soundMap.put(DICE_2,"music/DICE_2.wav");
        soundMap.put(DICE_3,"music/DICE_3.wav");
        soundMap.put(DICE_4,"music/DICE_4.wav");
        soundMap.put(DICE_5,"music/DICE_5.wav");
        soundMap.put(DICE_6,"music/DICE_6.wav");
    }

    public void playSound(int type) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayers.addLast(mediaPlayer);
            AssetFileDescriptor assetFileDescriptor = assetManager.openFd(soundMap.get(type));
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();
            mediaPlayer.start();
            if (mediaPlayers.size() > 3) {
                mediaPlayers.getFirst().release();
                mediaPlayers.removeFirst();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playMusic(int type) {
        switch (type) {
            case BACKGROUND: {
                if (!bk.isPlaying()) {
                    try {
                        bk.reset();
                        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(soundMap.get(type));
                        bk.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
                        bk.setLooping(true);
                        bk.prepare();
                        bk.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (game.isPlaying()) {
                    game.stop();
                }
                break;
            }
            case GAME:{
                if (!game.isPlaying()) {
                    try {
                        game.reset();
                        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(soundMap.get(type));
                        game.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
                        game.setLooping(true);
                        game.prepare();
                        game.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (bk.isPlaying()) {
                    bk.stop();
                }
                break;
            }
            default: {
                Log.e(TAG, "playMusic: 未识别的音乐类型" );
            }
        }
    }

    public void pauseMusic() {
        if (Global.activityManager.isSuspend()) {
            if (bk.isPlaying())
                bk.pause();
            if (game.isPlaying())
                game.pause();
        }
    }

    public void resumeMusic(int type) {
        if (type == GAME && !game.isPlaying()) {
            game.start();
        } else if (type == BACKGROUND && !bk.isPlaying()) {
            bk.start();
        }
    }
    public void stopMusic(int type) {
        if (type == GAME ) {
            game.stop();
        } else if (type == BACKGROUND && !bk.isPlaying()) {
            bk.stop();
        }
    }
}
