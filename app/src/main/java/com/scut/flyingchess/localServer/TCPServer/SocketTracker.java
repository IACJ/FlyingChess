package com.scut.flyingchess.localServer.TCPServer;


import android.util.Log;

import com.scut.flyingchess.dataPack.DataPackUtil;
import com.scut.flyingchess.dataPack.DataPack;
import com.scut.flyingchess.localServer.TCPServer.GameObjects.Player;
import com.scut.flyingchess.localServer.TCPServer.GameObjects.Room;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ryan on 16/4/12.
 *
 * Edited by IACJ on 2018/04/22
 */
public class SocketTracker implements Runnable {
    private Player selfPlayer = null;
    private MyTcpSocket socket = null;
    private TCPServer parent = null;

    private static final String TAG = "SocketTracker";

    public SocketTracker(MyTcpSocket socket, TCPServer server) throws IOException {
        this.socket = socket;
        this.parent = server;
    }

    public void run() {
        try {

            while (true) {
                processDataPack(socket.receive());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            /**
             *  when someone disconnected
             */
            Log.d(TAG, "run: 有人逃跑");

            try {
                if ( parent.getSelfRoom() != null){
                    parent.getSelfRoom().removePlayer(selfPlayer);
                }

                selfPlayer.getSocket().close();
                Log.d(TAG, "run: 关闭了服务器端一个Socket");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "逃跑已处理");
        }
    }


    /**
     * Process the incoming data packs.
     *
     * @param dataPack The data pack to be processed.
     */
    private void processDataPack(DataPack dataPack) throws SocketException, IOException {
        try {
            switch (dataPack.getCommand()) {
                case DataPack.INVALID: {
                    return;
                }
                case DataPack.R_LOGIN: {
                    Player player = Player.createPlayer(dataPack.getMessage(0));
                    // set the host if the player is the owner
                    if (socket.getInetSocketAddress().getAddress().isLoopbackAddress()){
                        player.setHost(true);
                    }

                    parent.getSelfRoom().addPlayer(player);
                    player.setSocket(socket);
                    this.selfPlayer = player;

                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(player.getId()));
                    msgList.addAll(DataPackUtil.getRoomPlayerInfoMessage(parent.getSelfRoom()));

                    socket.send(new DataPack(DataPack.A_ROOM_ENTER, true, msgList));
                    return;
                }
                case DataPack.R_LOGOUT: {
                    Player player = null;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    if (id < 0 && id >= -4)
                        player = Player.createPlayer("Robot");
                    else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    if (player != null) {
                        parent.getSelfRoom().removePlayer(player);
                    }
                    return;
                }
                case DataPack.R_ROOM_POSITION_SELECT: {
                    Room room = parent.getSelfRoom();
                    Player player = null;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    int position = Integer.valueOf(dataPack.getMessage(4));
                    if (id < 0) {
                        player = Player.createRobot(id);
                    } else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    boolean isSuccessful = room.playerSelectPosition(player, position);

                    if (isSuccessful)
                        socket.send(new DataPack(DataPack.E_ROOM_POSITION_SELECT, true, DataPackUtil.getPlayerInfoMessage(player)));
                    else
                        socket.send(new DataPack(DataPack.E_ROOM_POSITION_SELECT, false));

                    return;
                }
                case DataPack.R_ROOM_EXIT: {
                    Player player;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    if (id < 0 && id >= -4)
                        player = Player.createPlayer("Robot");
                    else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    Room room = parent.getSelfRoom();
                    room.removePlayer(player);
                    return;
                }
                case DataPack.R_GAME_START: {
                    Player player = null;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    if (id < 0 && id >= -4)
                        player = Player.createPlayer("Robot");
                    else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    Room room = parent.getSelfRoom();
                    if (player.isHost() && room.containsPlayer(player)) {
                        room.startGame();
                    }
                    return;
                }
                // the following 2 commands' logic is basically the same(simply forward the datapack)
                case DataPack.R_GAME_FINISHED: {
                    Player player = null;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    if (id < 0 && id >= -4)
                        player = Player.createPlayer("Robot");
                    else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    Room room = parent.getSelfRoom();
                    room.finishGame();
                    for (Player roomPlayer : room.getPlayers()) {
                        if (!roomPlayer.isRobot()) {
                            if (roomPlayer.equals(player)) {
                                player.setPoints(player.getPoints() + 10);
                            } else {
                                roomPlayer.setPoints(roomPlayer.getPoints() - 5);
                            }
                        }
                    }
                    room.broadcastToOthers(selfPlayer, new DataPack(DataPack.E_GAME_FINISHED, DataPackUtil.getRoomPlayerInfoMessage(room)));
                    return;
                }
                case DataPack.R_GAME_PROCEED_DICE: {
                    Player player = null;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    if (id < 0 && id >= -4)
                        player = Player.createPlayer("Robot");
                    else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    Room room = parent.getSelfRoom();
                    // set the command
                    dataPack.setCommand(DataPack.E_GAME_PROCEED_DICE);
                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    room.broadcastToOthers(selfPlayer, dataPack);
                    return;
                }
                case DataPack.R_GAME_PROCEED_PLANE: {
                    Player player = null;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    if (id < 0 && id >= -4)
                        player = Player.createPlayer("Robot");
                    else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    Room room = parent.getSelfRoom();

                    // set the command
                    dataPack.setCommand(DataPack.E_GAME_PROCEED_PLANE);
                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    if (room==null){
                        Log.e(TAG, "processDataPack: 房间已经不存在" );
                    }
                    room.broadcastToOthers(selfPlayer, dataPack);
                    return;
                }
                case DataPack.R_GAME_EXIT: {
                    Player player = null;
                    int id = Integer.valueOf(dataPack.getMessage(0));
                    if (id < 0 && id >= -4)
                        player = Player.createPlayer("Robot");
                    else
                        player = this.parent.getSelfRoom().getPlayer(id);

                    Room room = parent.getSelfRoom();

                    // generate datapack
                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(player.getId()));
                    if (player.isHost()) {
                        msgList.add("1");
                    } else {
                        msgList.add("0");
                    }
                    dataPack.setCommand(DataPack.E_GAME_PLAYER_DISCONNECTED);
                    dataPack.setDate(new Date());
                    dataPack.setMessageList(msgList);

                    // broadcast disconnected info
                    room.broadcastToOthers(selfPlayer, dataPack);

                    socket.send(new DataPack(DataPack.A_GAME_EXIT, true));
                    return;
                }
                default:{
                    Log.e(TAG, "processDataPack: 未知数据包错误" );
                }

            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
}
