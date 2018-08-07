package com.baishancloud.orchsym.sap;

import java.util.Properties;

import org.junit.AfterClass;

import com.sap.conn.jco.ext.DestinationDataProvider;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbsSAPIT {
    static {
        // System.setProperty("jco.destinations.dir", new File("src/test/resources/conn").getAbsolutePath());
    }

    @AfterClass
    public static void clean() {
        SAPDataManager.getInstance().unregister();
    }

    protected static Properties getASProp() {
        Properties clientProperties = new Properties();

        clientProperties.setProperty(DestinationDataProvider.JCO_ASHOST, "172.18.28.4");
        clientProperties.setProperty(DestinationDataProvider.JCO_SYSNR, "00");
        clientProperties.setProperty(DestinationDataProvider.JCO_CLIENT, "800");
        clientProperties.setProperty(DestinationDataProvider.JCO_USER, "baishan");
        clientProperties.setProperty(DestinationDataProvider.JCO_PASSWD, "passw0rd");
        clientProperties.setProperty(DestinationDataProvider.JCO_LANG, "zh");
        return clientProperties;
    }

    protected static Properties getASwithPoolProp() {
        Properties clientProperties = getASProp();
        clientProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, "3");
        clientProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, "10");
        return clientProperties;
    }
}
