package com.scut.flyingchess.localServer.UDPServer;


import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.scut.flyingchess.dataPack.DataPack;
import com.scut.flyingchess.localServer.LocalServer;
import com.scut.flyingchess.localServer.TCPServer.GameObjects.Room;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by BingF on 2016/5/15.
 *
 * Edited by IACJ on 2018/4/22
 */
public class UDPServer {
    private BroadcastSender sender = null;
    private BroadcastReceiver receiver = null;
    private LocalServer parent = null;
    private ConcurrentHashMap<UUID, DataPack> roomMap = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private AppCompatActivity activity = null;

    private static final String TAG = "UDPServer";

    public UDPServer(LocalServer parent, AppCompatActivity activity) {
        this.parent = parent;
        this.activity = activity;
    }

    public DataPack createRoomInfoListDataPack() {
        List<String> msgList = new LinkedList<>();
        for (DataPack dataPack : roomMap.values()) {
            msgList.addAll(dataPack.getMessageList().subList(0, 4));
        }

        return new DataPack(DataPack.A_ROOM_LOOKUP, msgList);
    }

    public void onRoomChanged(Room room) {
        this.sender.onRoomChanged(room);
    }

    void dataPackReceived(DataPack dataPack) {
        UUID id = UUID.fromString(dataPack.getMessage(0));
        switch (dataPack.getCommand()) {
            case DataPack.E_ROOM_REMOVE_BROADCAST: {
                roomMap.remove(id);
                parent.onDataPackReceived(createRoomInfoListDataPack());
                break;
            }
            case DataPack.E_ROOM_CREATE_BROADCAST: {
                roomMap.put(id, dataPack);
                parent.onDataPackReceived(createRoomInfoListDataPack());
                break;
            }
            default: {
                Log.e(TAG, "dataPackReceived: 未知数据包错误");
            }
                
        }
    }

    public Map<UUID, DataPack> getRoomMap() {
        return roomMap;
    }

    public void startBroadcast() {
        if (this.sender == null) {
            Log.d(TAG, "startBroadcast: 开启广播发送");
            this.sender = new BroadcastSender(this, activity);
            this.executor.submit(this.sender);
        }else {
            Log.e(TAG, "startBroadcast: 重复开启广播发送");
        }
    }
    public void stopBroadcast(@NotNull Room room) {
        if (this.sender != null){
            Log.d(TAG, "startBroadcast: 关闭广播发送");
            this.sender.stop(room);
            this.sender = null;
        }else{
            Log.e(TAG, "startBroadcast: 重复关闭广播发送");
        }
    }

    public void startListen() {
        if (this.receiver == null){
            Log.d(TAG, "startListen: 开启广播接收");
            this.receiver = new BroadcastReceiver(this);
            this.executor.submit(this.receiver);
        }else{
            Log.e(TAG, "startBroadcast: 重复开启广播接收");
        }
    }

    public void stopListen() {
        if (this.receiver != null){
            Log.d(TAG, "stopListen: 关闭广播接收");
            this.roomMap.clear();
            this.receiver.stop();
            receiver=null;
        }else{
            Log.e(TAG, "stopListen: 重复关闭广播接收");
        }

    }

    /**
     * 内部类： 房间信息广播发送者
     *
     * 每个房间对应一个发送者
     */
    public class BroadcastSender implements Runnable {
        private MyUdpSocket sendSocket;
        private boolean isRunning = true;
        private String localIp = null;
        private int port = 6667;
        private UDPServer parent = null;
        private DataPack dataPack = null;
        private String ipBroadcast;

        private static final String TAG = "BroadcastSender";

        public BroadcastSender(UDPServer parent, final AppCompatActivity activity) {
            this.parent = parent;
            try {
                WifiManager wm = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                localIp = getLocalHostIp();

                ipBroadcast = getBroadcast();
                sendSocket = new MyUdpSocket();
                Log.v(TAG, "BroadcastSender: localIP为 "+localIp+",ipBroadcast为"+ ipBroadcast);
                Log.v(TAG, "BroadcastSender: Dhcp信息："+wm.getDhcpInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                while (isRunning) {
                    if (this.dataPack != null) {
                        Log.v(TAG, "run: 向"+ipBroadcast+"发送广播"+dataPack);
                        this.sendSocket.send(this.dataPack, InetAddress.getByName(ipBroadcast), port);
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void stop(Room room) {
            this.isRunning = false;
            try {
                if (room != null) {
                    List<String> msgList = new ArrayList<>();
                    msgList.add(room.getId().toString());
                    this.sendSocket.send(new DataPack(DataPack.E_ROOM_REMOVE_BROADCAST, msgList), InetAddress.getByName(ipBroadcast), port);
                }
                sendSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void onRoomChanged(Room room) {
            List<String> msgList = new ArrayList<>();
            msgList.add(room.getId().toString());
            msgList.add(room.getName());
            msgList.add(String.valueOf(room.getAllPlayers().size()));
            msgList.add(room.isPlaying() ? "1" : "0");
            msgList.add(this.localIp);
            msgList.add(String.valueOf(port));
            this.dataPack = new DataPack(DataPack.E_ROOM_CREATE_BROADCAST, msgList);
        }


        /**
         * 获取本地IP
         *
         * @return 本地IP
         */
        private String getLocalHostIp() {
            String ipaddress = "";
            try {
                Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                while (en.hasMoreElements()) {
                    NetworkInterface nif = en.nextElement();
                    Enumeration<InetAddress> inet = nif.getInetAddresses();
                    while (inet.hasMoreElements()) {
                        InetAddress ip = inet.nextElement();
                        if (!ip.isLoopbackAddress() && ip.getHostAddress().length() <= 15) {
                            return ip.getHostAddress();
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
            return ipaddress;
        }

        public  String getBroadcast() throws SocketException {
            System.setProperty("java.net.preferIPv4Stack", "true");
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() != null) {
                            return interfaceAddress.getBroadcast().toString().substring(1);
                        }
                    }
                }
            }
            return null;
        }

    }

    /**
     * 内部类： 房间信息广播接收者
     *
     * 每个用户在`大厅活动`中获得一个广播接收者
     */
    public class BroadcastReceiver implements Runnable {
        private MyUdpSocket receiveSocket = null;
        private UDPServer parent = null;

        private boolean isRunning = false;
        private final static int port = 6667;

        private static final String TAG = "BroadcastReceiver";

        public BroadcastReceiver(UDPServer parent) {
            try {
                this.parent = parent;
                this.receiveSocket = new MyUdpSocket(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            isRunning = true;
            while (isRunning) {
                try {
                    DataPack dataPack = this.receiveSocket.receive();
                    parent.dataPackReceived(dataPack);
                } catch (Exception e) {
                    if (e.getMessage().equals("Socket closed") ){
                        Log.d(TAG, "run: 广播接收器Socket被强行关闭");
                    }
                }
            }
        }

        public void stop() {
            isRunning = false;
            try {
                receiveSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
