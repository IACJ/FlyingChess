package com.flashminds.flyingchess.localServer.UDPServer;


import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.flashminds.flyingchess.Global;
import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.localServer.LocalServer;
import com.flashminds.flyingchess.localServer.TCPServer.GameObjects.Room;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.support.v7.widget.StaggeredGridLayoutManager.TAG;

/**
 * Created by BingF on 2016/5/15.
 *
 * Edited by IACJ on 2018/4/22
 */
public class UDPServer {
    private BroadcastSender sender = null;
    private BroadcastReceiver receiver = null;
    private LocalServer parent = null;
    private HashMap<UUID, DataPack> roomMap = new HashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private AppCompatActivity activity = null;

    private static final String TAG = "UDPServer";

    public UDPServer(LocalServer parent, AppCompatActivity activity) {
        this.parent = parent;
        this.activity = activity;
    }

    public DataPack createRoomInfoListDataPack() {
        List<String> msgList = new LinkedList<>();
        for (DataPack dataPack : roomMap.values())
            msgList.addAll(dataPack.getMessageList().subList(0, 4));

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
                if (roomMap.get(id) == null ||
                        Integer.valueOf(roomMap.get(id).getMessage(2)) != Integer.valueOf(dataPack.getMessage(2)) ||
                        Integer.valueOf(roomMap.get(id).getMessage(3)) != Integer.valueOf(dataPack.getMessage(3))) {
                    roomMap.put(id, dataPack);
                    parent.onDataPackReceived(createRoomInfoListDataPack());
                }
                break;
            }
            default:
                break;
        }
    }

    public Map<UUID, DataPack> getRoomMap() {
        return roomMap;
    }

    public void startBroadcast() {
//        if (this.sender != null)
//            this.sender.stop(null);

        Log.d(TAG, "startBroadcast: 开启广播发送");
        this.sender = new BroadcastSender(this, activity);
        this.executor.submit(this.sender);
    }

    public void startListen() {

        Log.d(TAG, "startListen: 开启广播接收");
        this.receiver = new BroadcastReceiver(this);
        this.executor.submit(this.receiver);
    }

    public void stopBroadcast(Room room) {
        this.sender.stop(room);
    }

    public void stopListen() {
        this.roomMap.clear();
        this.receiver.stop();
        receiver=null;
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
        private List<String> ipSection = null;

        private static final String TAG = "BroadcastSender";

        public BroadcastSender(UDPServer parent, final AppCompatActivity activity) {
            this.parent = parent;
            try {
                WifiManager wm = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
                localIp = getLocalHostIp();
                ipSection = getIpSection(localIp, wm.getDhcpInfo().netmask);
                sendSocket = new MyUdpSocket();
                Log.d(TAG, "BroadcastSender: localIP为 "+localIp+",  ipSection为 "+ ipSection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String getLocalHostIp() {
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


        public void run() {
            try {
                while (isRunning) {
                    if (this.dataPack != null) {
                        for (String ip : ipSection) {
                            Log.d(TAG, "run: 向ip地址广播发送dataPack "+ip);
                            this.sendSocket.send(this.dataPack, InetAddress.getByName(ip), port);
                        }
                    }
                    Thread.sleep(3000);
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
                    for (String ip : ipSection)
                        this.sendSocket.send(new DataPack(DataPack.E_ROOM_REMOVE_BROADCAST, msgList), InetAddress.getByName(ip), port);
                }
                sendSocket.close();
                sendSocket = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        void onRoomChanged(Room room) {
            List<String> msgList = new ArrayList<>();
            msgList.add(room.getId().toString());
            msgList.add(room.getName());
            msgList.add(String.valueOf(room.getAllPlayers().size()));
            msgList.add(room.isPlaying() == true ? "1" : "0");
            msgList.add(this.localIp);
            msgList.add(String.valueOf(port));
            this.dataPack = new DataPack(DataPack.E_ROOM_CREATE_BROADCAST, msgList);
        }


        /**
         * Converts an string formatted ip into int, note that this conversion simply
         * put the dotted format into its corresponding int format, without transforming
         * into network representation.
         *
         * @param ip String formatted ip.
         * @return Int formatted ip.
         */
        public int stringToInt(String ip) {
            ip.trim();

            String[] dots = ip.split("\\.");

            if (dots.length < 4)
                throw new IllegalArgumentException();

            return (Integer.valueOf(dots[0]) << 24) + (Integer.valueOf(dots[1]) << 16) + (Integer.valueOf(dots[2]) << 8) + Integer.valueOf(dots[3]);
        }

        /**
         * Converts an int formatted ip into string, note that this conversion simply
         * put the int format into its corresponding dotted ip format, without transforming
         * from network representation.
         *
         * @param ip Int formatted ip.
         * @return String formatted ip.
         */
        public String intToString(int ip) {
            StringBuilder sb = new StringBuilder();

            sb.append(String.valueOf((ip >>> 24)));
            sb.append(".");

            sb.append(String.valueOf((ip & 0x00FFFFFF) >>> 16));
            sb.append(".");

            sb.append(String.valueOf((ip & 0x0000FFFF) >>> 8));
            sb.append(".");

            sb.append(String.valueOf((ip & 0x000000FF)));
            return sb.toString();
        }

        /**
         * Given the string formatted ip and network represented mask,
         * returns the ip sections (List of string formatted ip).
         *
         * @param ip   Any string formatted ip in the section.
         * @param mask Sub-net mask.
         * @return List of string formatted ip in the section without
         * broadcast addresses and itself.
         */
        public List<String> getIpSection(String ip, Integer mask) {
            List<String> ipSection = new LinkedList<>();

            int orderedMask = ((mask & 0xFF000000) >>> 24) | ((mask & 0x00FF0000) >>> 8) | ((mask & 0x0000FF00) << 8) | ((mask & 0x000000FF) << 24);

            int startIp = stringToInt(ip) & orderedMask;
            for (int i = startIp; i < ((startIp) | (~orderedMask)); i++) {

                String ipStr = intToString(i);
                if (ipStr.equals(ip) || ipStr.contains("255"))
                    continue;

                ipSection.add(ipStr);
            }
            return ipSection;
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
                    if (this.receiveSocket == null){
                        Log.e(TAG, "run: 已知位置,未知错误!" );
                        Global.delay(500);
                    }
                       

                    DataPack dataPack = this.receiveSocket.receive();
                    parent.dataPackReceived(dataPack);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    if (e.getMessage().equals("Socket closed") ){
                        System.out.println("强行关闭socket连接");
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
