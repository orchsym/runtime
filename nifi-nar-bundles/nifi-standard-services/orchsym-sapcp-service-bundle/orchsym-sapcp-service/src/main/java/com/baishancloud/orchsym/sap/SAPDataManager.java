package com.baishancloud.orchsym.sap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Adler32;

import org.apache.commons.codec.digest.DigestUtils;

import com.baishancloud.orchsym.sap.i18n.Messages;
import com.baishancloud.orchsym.sap.option.ESAPServerType;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.util.Codecs.MD5;

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class SAPDataManager {

    static class CustomDestinationDataProvider implements DestinationDataProvider {
        private DestinationDataEventListener el;

        private Map<String, Properties> destTypesPropertiesMap = new HashMap<>();

        public Properties getDestinationProperties(String destinationName) {
            Properties prop = destTypesPropertiesMap.get(destinationName);
            if (prop == null) {
                // throw new RuntimeException(Messages.getString("SAPDataManager.Unavailable_Destination", destinationName)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return prop;
        }

        public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
            this.el = eventListener;
        }

        public boolean supportsEvents() {
            return true; // enable to delete
        }

        void changeProperties(String destinationName, Properties properties) {
            if (properties == null) {
                if (el != null)
                    el.deleted(destinationName);

                destTypesPropertiesMap.remove(destinationName);
            } else {
                Properties oldProp = destTypesPropertiesMap.get(destinationName);
                if (oldProp == null || !oldProp.equals(properties)) {
                    destTypesPropertiesMap.put(destinationName, properties);

                    if (el != null)
                        el.updated(destinationName);
                }
            }
        }
    }

    static class CustomServerDataProvider implements ServerDataProvider {
        private ServerDataEventListener el;
        private Map<String, Properties> serverPropertiesMap = new HashMap<>();

        @Override
        public Properties getServerProperties(String serverName) {
            Properties prop = serverPropertiesMap.get(serverName);
            if (prop == null) {
                // throw new RuntimeException(Messages.getString("SAPDataManager.Unavailable_Server", serverName)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return prop;
        }

        @Override
        public void setServerDataEventListener(ServerDataEventListener eventListener) {
            this.el = eventListener;
        }

        @Override
        public boolean supportsEvents() {
            return true; // enable to delete
        }

        void changeProperties(String serverName, Properties properties) {
            if (properties == null) {
                if (el != null)
                    el.deleted(serverName);

                serverPropertiesMap.remove(serverName);
            } else {
                Properties oldProp = serverPropertiesMap.get(serverName);
                if (oldProp == null || !oldProp.equals(properties)) {
                    serverPropertiesMap.put(serverName, properties);

                    if (el != null)
                        el.updated(serverName);
                }
            }
        }
    }

    private static SAPDataManager instance;
    private final CustomDestinationDataProvider clientDataProvider;
    private final CustomServerDataProvider serverDataProvider;

    private SAPDataManager() {
        clientDataProvider = new CustomDestinationDataProvider();
        serverDataProvider = new CustomServerDataProvider();
    }

    public static SAPDataManager getInstance() {
        if (instance == null) {
            synchronized (SAPDataManager.class) {
                if (instance == null) {
                    instance = new SAPDataManager();
                }
            }
        }
        return instance;
    }

    public void unregister() {
        unregisterServer();
        unregisterClient();
    }

    public void unregisterClient() {
        try {
            Environment.unregisterDestinationDataProvider(clientDataProvider);
        } catch (IllegalStateException e2) {
            // nothing to do
        }
    }

    public void unregisterServer() {
        try {
            Environment.unregisterServerDataProvider(serverDataProvider);
        } catch (IllegalStateException e2) {
            // nothing to do
        }
    }

    /**
     * set the client properties via provider, else will be xxx.jcoDestination file
     */
    public synchronized void updateClientProp(String identifier, ESAPServerType serverType, Properties properties) {
        if (serverType != null) {
            clientDataProvider.changeProperties(getDestinationName(identifier, serverType), properties);
        }
    }

    /**
     * set the server properties via provider, else will be xxx.jcoServer file
     */
    public synchronized void updateServerProp(String identifier, String serverName, Properties properties) {
        if (serverName != null) {
            serverDataProvider.changeProperties(getServerName(identifier, serverName), properties);
        }
    }

    public static String getDestinationName(String identifier, ESAPServerType serverType) {
        return serverType.getValue() + '_' + getChecksum(identifier);
    }

    public static String getServerName(String identifier, String serverName) {
        return serverName + '_' + getChecksum(identifier);
    }

    public static String getChecksum(String identifier) {
        Adler32 a32 = new Adler32();
        a32.update(identifier.getBytes(StandardCharsets.UTF_8));
        return Long.toString(a32.getValue(), 32).toUpperCase();
    }
    // public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
    // this.clientDataProvider.setDestinationDataEventListener(eventListener);
    // }
    //
    // public void setServerDataEventListener(ServerDataEventListener eventListener) {
    // this.serverDataProvider.setServerDataEventListener(eventListener);
    // }

    public synchronized JCoDestination getDestination(String identifier, ESAPServerType serverType) throws JCoException {
        if (identifier == null) {
            return null;
        }
        if (!Environment.isDestinationDataProviderRegistered()) {//
            Environment.registerDestinationDataProvider(clientDataProvider);
        }
        JCoDestination jcoDest = JCoDestinationManager.getDestination(getDestinationName(identifier, serverType));
        return jcoDest;
    }

    public synchronized JCoServer getServer(String identifier, String serverName) throws JCoException {
        if (identifier == null) {
            return null;
        }
        if (!Environment.isServerDataProviderRegistered()) {//
            Environment.registerServerDataProvider(serverDataProvider);
        }

        JCoServer server = JCoServerFactory.getServer(getServerName(identifier, serverName));
        return server;
    }
}
