package com.flashminds.flyingchess.localServer.TCPServer;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.flashminds.flyingchess.dataPack.DataPack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

/**
 * Created by Ryan on 16/4/26.
 *
 * Edited by IACJ on 2018/4/22
 */
public class DataPackTcpSocket {
    protected Socket socket = null;
    protected DataInputStream is = null;
    protected DataOutputStream os = null;
    protected Gson dataPackGson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

    private static final String TAG = "DataPackTcpSocket";
    

    public DataPackTcpSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);
        this.os = new DataOutputStream(socket.getOutputStream());
        this.is = new DataInputStream(socket.getInputStream());
    }

    /**
     * 读取一个 DataPack
     */
    public DataPack receive() throws IOException {
        int blockSize = this.is.readInt();
        byte[] bytes = new byte[blockSize];
        this.is.readFully(bytes);

        Log.v(TAG, "receive:接收"+blockSize+new String(bytes, "UTF-8"));

        return dataPackGson.fromJson(new String(bytes, "UTF-8"), DataPack.class);
    }



    /**
     * 发送 DataPack
     */
    public synchronized void send(DataPack dataPack) throws IOException {
        try {

            byte[] sendBytes = dataPackGson.toJson(dataPack, DataPack.class).getBytes(Charset.forName("UTF-8"));
            int bytesSize = sendBytes.length;

            this.os.writeInt(bytesSize);
            this.os.flush();
            this.os.write(sendBytes);
            this.os.flush();


            Log.v(TAG, "send:发送"+bytesSize+dataPackGson.toJson(dataPack, DataPack.class));

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭 Socket
     */
    public void close() throws IOException {
        send(new DataPack(DataPack.TERMINATE));
        this.os.close();
        this.is.close();
        this.socket.close();
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(socket.getInetAddress(), socket.getPort());
    }
}
