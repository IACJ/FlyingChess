package com.flashminds.flyingchess.manager;

import android.util.Log;

import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.Global;
import com.flashminds.flyingchess.dataPack.Target;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by karthur on 2016/4/26.
 *
 * Edited by IACJ on 2018/4/18
 */
public class SocketManager  {
    private Socket sock = null;
    private SocketWriter sw;
    private SocketReader sr;
    private boolean connected = false;

    HashMap<Integer, Target> targets= new HashMap<>();

    private static final String TAG = "SocketManager";
    
    public void connectToLocalServer() {
        try {
            connected = true;
            sock = new Socket(InetAddress.getLocalHost(), 6666);
//            sock.setSoTimeout(2000);
            sock.setTcpNoDelay(true);
            sock.setSoTimeout(0);//cancle time out
            sw = new SocketWriter(sock.getOutputStream());
            sr = new SocketReader(sock.getInputStream());
            new Thread(sw).start();
            new Thread(sr).start();
        } catch (Exception e) {
            e.printStackTrace();
            connected = false;
        }
    }

    public void connectLanServer(final String ip) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connected = true;
                    sock = new Socket(InetAddress.getByName(ip), 6666);
                    sock.setSoTimeout(2000);
                    sock.setTcpNoDelay(true);
                    sw = new SocketWriter(sock.getOutputStream());
                    sr = new SocketReader(sock.getInputStream());
                    new Thread(sw).start();
                    new Thread(sr).start();

                    sock.setSoTimeout(0);//cancle time out
                } catch (Exception e) {
                    e.printStackTrace();
                    connected = false;
                }
            }
        }).start();
    }

    public void connectToRemoteServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    SSLContext sslContext = SSLContext.getInstance("SSLv3");
//                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
//                    KeyStore trustKeyStore = KeyStore.getInstance("BKS");
//                    trustKeyStore.load(activity.getBaseContext().getResources().openRawResource(R.raw.flyingchess), "hustcs1307".toCharArray());
//                    trustManagerFactory.init(trustKeyStore);
//                    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
//                    sock = (SSLSocket) sslContext.getSocketFactory().createSocket(Global.dataManager.data.ip, 6666);
                    sock = new Socket();
                    sock.connect( new InetSocketAddress(Global.dataManager.data.ip,6666),1500);

                    sock.setSoTimeout(1500);
                    sock.setTcpNoDelay(true);
                    sw = new SocketWriter(sock.getOutputStream());
                    sr = new SocketReader(sock.getInputStream());
                    new Thread(sw).start();
                    new Thread(sr).start();
                    connected = true;
                    sock.setSoTimeout(0);//cancle time out
                } catch (Exception e) {
                    e.printStackTrace();
                    connected = false;
                }
                DataPack dataPack = new DataPack(DataPack.CONNECTED, null);
                dataPack.setSuccessful(connected);
                processDataPack(dataPack);
            }
        }).start();
    }


    public boolean send(DataPack dataPack) {
        return sw.send(dataPack);
    }

    public boolean send(int command, Object... argv) {
        LinkedList<String> msgs = new LinkedList<>();
        for (int i = 0; i < argv.length; i++) {
            msgs.addLast(String.valueOf(argv[i]));
        }
        DataPack dataPack = new DataPack(command, msgs);
        return sw.send(dataPack);
    }


    public void registerActivity(int datapackCommond, Target target) {
        targets.put(datapackCommond, target);

    }

    public void showRegistedActivities(){
        System.out.println("打印已注册的活动");
        for (int key:targets.keySet()) {
            System.out.println(key+" "+ targets.get(key).getClass().getSimpleName());
        }
        System.out.println("打印结束");
    }

    protected void processDataPack(DataPack dataPack) {
        if (targets.containsKey(dataPack.getCommand())) {
            targets.get(dataPack.getCommand()).processDataPack(dataPack);
            Log.v(TAG, "processDataPack: 处理:"+dataPack);
        }else{
            Log.e(TAG, "processDataPack: 数据包未被处理"+dataPack);
        }
    }

    /**
     * 内部类 `SocketReader`
     *
     * 读取并处理数据包
     */
    public class SocketReader implements Runnable {

        private Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        private DataInputStream dis;

        private static final String TAG = "SocketReader";


        public SocketReader(InputStream is) {
            this.dis = new DataInputStream(is);
        }

        @Override
        public void run() {
            while (connected) {
                try {
                    // 接收一个数据包并处理
                    int blockSize;
                    blockSize = this.dis.readInt();
                    byte[] bytes = new byte[blockSize];
                    this.dis.readFully(bytes);
                    String json = new String(bytes, "UTF-8");
                    DataPack dataPack = gson.fromJson(json, DataPack.class);
                    Log.v(TAG, "接收："+dataPack);
                    Global.socketManager.processDataPack(dataPack);
                } catch (EOFException e){
                    Log.e(TAG, "run: Socket被强制断开");
                    connected = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    connected = false;
                }
            }
        }
    }

    /**
     * 内部类： `SocketWriter`
     *
     */
    public class SocketWriter implements Runnable {

        private Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        private LinkedBlockingQueue<DataPack> dataPackQueue = new LinkedBlockingQueue<>();
        private DataOutputStream dos;

        private static final String TAG = "SocketWriter";


        public SocketWriter(OutputStream os) {
            this.dos = new DataOutputStream(os);
        }

        @Override
        public void run() {

            while (true) {
                try {
                    // 发送dataPackQueue中的消息
                    List<DataPack> dataPackList = new ArrayList<>();
                    this.dataPackQueue.drainTo(dataPackList);
                    for (DataPack dataPack : dataPackList) {
                        byte[] sendBytes = gson.toJson(dataPack, DataPack.class).getBytes(Charset.forName("UTF-8"));
                        int bytesSize = sendBytes.length;

                        Log.v(TAG, "发送："+dataPack);
                        
                        this.dos.writeInt(bytesSize);
                        this.dos.write(sendBytes);
                        this.dos.flush();
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                    connected = false;
                    break;
                }
            }
        }

        /**
         * 把 datapack 放入消息队列
         */
        public boolean send(DataPack dataPack) {
            if (connected) {
                try {
                    this.dataPackQueue.put(dataPack);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                Log.e(TAG, "send: 已知位置未知错误");
            }
            return connected;
        }
    }
}
