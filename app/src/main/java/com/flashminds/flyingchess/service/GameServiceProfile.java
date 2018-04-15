package com.flashminds.flyingchess.service;

import com.tencent.mars.sample.wrapper.service.MarsServiceProfile;

/**
 * Created by Wenpzhou on 2018/4/10 0010.
 */

public class GameServiceProfile implements MarsServiceProfile {
    public static final short MAGIC = 0x0110;
    public static final short PRODUCT_ID = 200;
    public static final String LONG_LINK_HOST = "localhost";
    public static final int[] LONG_LINK_PORTS = new int[]{8081};
    public static final int SHORT_LINK_PORT = 8080;

    @Override
    public short magic() {
        return MAGIC;
    }

    @Override
    public short productID() {
        return PRODUCT_ID;
    }

    @Override
    public String longLinkHost() {
        return LONG_LINK_HOST;
    }

    @Override
    public int[] longLinkPorts() {
        return LONG_LINK_PORTS;
    }

    @Override
    public int shortLinkPort() {
        return SHORT_LINK_PORT;
    }
}
