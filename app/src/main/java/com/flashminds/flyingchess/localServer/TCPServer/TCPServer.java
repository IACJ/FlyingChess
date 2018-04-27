package com.flashminds.flyingchess.localServer.TCPServer;


import android.os.Build;
import android.util.Log;

import com.flashminds.flyingchess.localServer.LocalServer;
import com.flashminds.flyingchess.localServer.TCPServer.GameObjects.Room;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TCPServer {
    private ServerSocket serverSocket = null;
    private ExecutorService socketExecutor =  Executors.newCachedThreadPool();
    private LocalServer parent = null;
    private Room selfRoom = null;
    private boolean isRunning = true;

    private static final String TAG = "TCPServer";


    public TCPServer(LocalServer parent) {
        this.parent = parent;
    }

    public void start() {
        Log.d(TAG, "start: TCPServer 打开");
        isRunning = true;
        try {
            if (serverSocket == null || !serverSocket.isBound() || serverSocket.isClosed()){
                this.serverSocket = new ServerSocket(6666);
                serverSocket.setSoTimeout(0);
            }

            this.selfRoom = new Room(new Build().MODEL, this);
            this.onRoomChanged(selfRoom);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRunning) {
                        try {
                            Socket sock = serverSocket.accept();
                            Runnable socketRunnable = new SocketTracker(new MyTcpSocket(sock), TCPServer.this);
                            socketExecutor.submit(socketRunnable);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Room getSelfRoom() {
        return this.selfRoom;
    }

    public void onRoomChanged(Room room) {
        this.selfRoom = room;
        parent.setRoomInfoForBroadCast(room);
    }

    public Room stop() {
//        try {
//            // send shutdown datapack to ever online users
//            // and stop the dSocket.
//            for (Player player : this.selfRoom.getAllPlayers()) {
//                player.getSocket().send(new DataPack(DataPack.TERMINATE));
//                player.getSocket().close();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        Log.d(TAG, "stop: TCPServer 关闭");
        Room room = this.selfRoom;
        this.isRunning = false;
        this.selfRoom = null;
        return room;
    }
}
