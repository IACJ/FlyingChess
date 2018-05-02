package com.scut.flyingchess.localServer;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.scut.flyingchess.dataPack.DataPack;
import com.scut.flyingchess.localServer.TCPServer.GameObjects.Room;
import com.scut.flyingchess.localServer.TCPServer.TCPServer;
import com.scut.flyingchess.localServer.UDPServer.UDPServer;
import com.scut.flyingchess.dataPack.Target;

import java.util.UUID;


/**
 * Created by BingF on 2016/5/15.
 */
public class LocalServer {
    private UDPServer udpServer = null;
    private TCPServer tcpServer = null;

    private Target target;

    private static final String TAG = "LocalServer";

    public LocalServer(AppCompatActivity activity) {
        udpServer = new UDPServer(this, activity);
        tcpServer = new TCPServer(this);
    }

    /**
     * Message notification from tcp server.
     *
     * @param room
     */
    public void setRoomInfoForBroadCast(Room room) {
        udpServer.onRoomChanged(room);
    }

    public void onDataPackReceived(DataPack dataPack) {
        Log.v(TAG, "onDataPackReceived: 请求处理数据包："+target.getClass().getSimpleName()+"-->"+dataPack);
        target.processDataPack(dataPack);
    }

    public String getRoomIp(String roomId) {
        return udpServer.getRoomMap().get(UUID.fromString(roomId)).getMessage(4);
    }

    public int getPort(String roomId) {
        return Integer.valueOf(udpServer.getRoomMap().get(UUID.fromString(roomId)).getMessage(5));
    }

    public void updateRoomListImmediately() {
        onDataPackReceived(udpServer.createRoomInfoListDataPack());
    }

    public void startListen() {
        udpServer.startListen();
    }

    public void stopListen(){
        udpServer.stopListen();
    }

    public void startHost() {
        udpServer.startBroadcast();
        tcpServer.start();
    }

    public void stopHost() {
        Room closedRoom = tcpServer.stop();
        udpServer.stopBroadcast(closedRoom);
    }

    public void stop() {
        udpServer.stopListen();
    }

    public void registerMsg(Target target) {
        this.target = target;
    }

}
