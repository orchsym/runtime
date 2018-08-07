package com.baishancloud.orchsym.sap.server;

import java.util.Properties;
import java.util.UUID;

import org.junit.BeforeClass;

import com.baishancloud.orchsym.sap.AbsSAPIT;
import com.baishancloud.orchsym.sap.option.ESAPServerType;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataProvider;

/**
 * @author GU Guoqiang
 *
 */
public class AbsSAPServerIT extends AbsSAPIT {
    static ESAPServerType serverType;

    @BeforeClass
    public static void init() throws Exception {
        // JCo.setTrace(4, null);// open trace

        serverType = ESAPServerType.AS_POOL;
    }
    
    protected static Properties getServerProp(Properties asPoolProp) {

        Properties servertProperties = new Properties();
        // same IP as client
        servertProperties.setProperty(ServerDataProvider.JCO_GWHOST, asPoolProp.getProperty(DestinationDataProvider.JCO_ASHOST));
        // 00 is SYSNR, also can find from parameters in SMGW
        servertProperties.setProperty(ServerDataProvider.JCO_GWSERV, "sapgw" + asPoolProp.getProperty(DestinationDataProvider.JCO_SYSNR));
        // from Program ID in SM59
        servertProperties.setProperty(ServerDataProvider.JCO_PROGID, "ZBSY_JCO_STEST");
        // server type of client
        servertProperties.setProperty(ServerDataProvider.JCO_REP_DEST, serverType.getValue());
        servertProperties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, "2");
        return servertProperties;
    }
}
