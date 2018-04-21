package com.flashminds.flyingchess.localServer.UDPServer;


import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.entity.Global;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * Created by BingF on 2016/5/15.
 */
public class BroadcastReceiver implements Runnable {
    private MyUdpSocket receiveSocket = null;
    private UDPServer parent = null;

    private boolean isRunning = true;
    private final static int port = 6667;

    public BroadcastReceiver(UDPServer parent) {
        try {
            this.parent = parent;
            this.receiveSocket = new MyUdpSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (isRunning) {
            try {
                if (this.receiveSocket == null)
                    Global.delay(500);
                DataPack dataPack = this.receiveSocket.receive();
                parent.dataPackReceived(dataPack);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            receiveSocket.close();
            receiveSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
