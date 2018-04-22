package com.flashminds.flyingchess.localServer.UDPServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.flashminds.flyingchess.dataPack.DataPack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Created by Ryan on 16/5/15.
 *
 * Edited by IACJ on 2018/4/19
 */
public class MyUdpSocket {
    private DatagramSocket dSocket = null;
    private Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
    private byte[] buffer = new byte[1024];

    ///////////////////////////////////////////////////////<构造方法> begin
    public MyUdpSocket() throws SocketException {
        this.dSocket = new DatagramSocket();
    }

    public MyUdpSocket(int port) throws SocketException {
        this.dSocket = new DatagramSocket(port);
    }
    ////////////////////////////////////////////////////////</构造方法> end

    /**
     * 向目标 ip 发送 dataPack
     *`
     * @param dataPack 要发送的dataPack
     * @param ip       目标ip
     */
    public void send(DataPack dataPack, InetAddress ip, int port) throws IOException {
        byte[] bytes = gson.toJson(dataPack, DataPack.class).getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip, port);
        dSocket.send(packet);
    }

    /**
     * 接收一个 dataPack
     */
    public DataPack receive() throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        dSocket.receive(packet);
        return gson.fromJson(new String(packet.getData()).trim(), DataPack.class);
    }

    /**
     * 关闭 dSocket
     */
    public void close() throws IOException {
        this.dSocket.close();
    }
}
