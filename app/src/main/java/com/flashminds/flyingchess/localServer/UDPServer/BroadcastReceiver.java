package com.flashminds.flyingchess.localServer.UDPServer;


import com.flashminds.flyingchess.dataPack.DataPack;
import com.flashminds.flyingchess.Global;

import java.io.IOException;

/**
 * Created by BingF on 2016/5/15.
 *
 * Edited by IACJ on 2018/04/22
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

                if (this.receiveSocket == null)
                    break;
                DataPack dataPack = this.receiveSocket.receive();
                parent.dataPackReceived(dataPack);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (e.getMessage().equals("Socket closed") ){
                    System.out.println("强行关闭socket连接");
                }
//                e.printStackTrace();
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
