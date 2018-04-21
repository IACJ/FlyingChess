package com.flashminds.flyingchess.manager;

import android.support.v7.app.AppCompatActivity;

import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.entity.Global;
import com.flashminds.flyingchess.entity.MsgHandler;
import com.flashminds.flyingchess.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by karthur on 2016/4/26.
 *
 * Edited by IACJ on 2018/4/18
 */
public class SocketManager extends MsgHandler {
    private Socket sock = null;
    private SocketWriter sw;
    private SocketReader sr;
    private boolean connected = false;


    public void connectToLocalServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sock = new Socket(InetAddress.getLocalHost(), 6666);
                    sock.setSoTimeout(2000);
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
            }
        }).start();
    }

    public void connectLanServer(final String ip) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sock = new Socket(InetAddress.getByName(ip), 6666);
                    sock.setSoTimeout(2000);
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

                    sock.setSoTimeout(2000);
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


    /**
     * 内部类 `SocketReader`
     *
     * 读取并处理数据包
     */
    public class SocketReader implements Runnable {

        private Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        private DataInputStream is = null;
        private boolean connected = false;


        public SocketReader(InputStream is) {
            this.is = new DataInputStream(is);
        }

        @Override
        public void run() {
            connected = true;
            while (true) {
                try {
                    Global.socketManager.processDataPack(receive());
                } catch (Exception e) {
                    e.printStackTrace();
                    connected = false;
                    Global.offlineTip();
                    break;
                }
            }
        }

        private DataPack receive() throws IOException {
            int blockSize;
            blockSize = this.is.readInt();
            byte[] bytes = new byte[blockSize];
            this.is.readFully(bytes);
            String json = new String(bytes, "UTF-8");
            return gson.fromJson(json, DataPack.class);
        }
    }

    /**
     * 内部类 `SocketWriter`
     *
     * 发送数据包
     */
    public class SocketWriter implements Runnable {

        private Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        private LinkedBlockingQueue<DataPack> dataPackQueue = new LinkedBlockingQueue<>();
        private DataOutputStream os = null;
        private boolean connected = false;

        public SocketWriter(OutputStream os) {
            this.os = new DataOutputStream(os);
        }

        @Override
        public void run() {
            connected = true;
            while (true) {
                try {
                    // 发送dataPackQueue中的消息
                    List<DataPack> dataPackList = new ArrayList<>();
                    this.dataPackQueue.drainTo(dataPackList);
                    for (DataPack dataPack : dataPackList) {
                        byte[] sendBytes = gson.toJson(dataPack, DataPack.class).getBytes(Charset.forName("UTF-8"));
                        int bytesSize = sendBytes.length;
                        this.os.writeInt(bytesSize);
                        this.os.write(sendBytes);
                        this.os.flush();
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
         *
         * @param dataPack The datapack to be queued.
         */
        public boolean send(DataPack dataPack) {
            if (connected) {
                try {
                    this.dataPackQueue.put(dataPack);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return connected;
        }
    }
}
