package com.baishancloud.orchsym.sap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class SAPDataManager {

    static class CustomDestinationDataProvider implements DestinationDataProvider {
        private DestinationDataEventListener el;

        private Map<ESAPServerType, Properties> destTypesPropertiesMap = new HashMap<>();

        public Properties getDestinationProperties(String destinationName) {
            ESAPServerType serverType = ESAPServerType.valueOf(destinationName.toUpperCase());
            if (serverType == null) {
                throw new RuntimeException(Messages.getString("SAPDataManager.Unsupport_Destination", destinationName)); //$NON-NLS-1$
            }
            Properties prop = destTypesPropertiesMap.get(serverType);
            if (prop == null) {
                throw new RuntimeException(Messages.getString("SAPDataManager.Unavailable_Destination", destinationName)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return prop;
        }

        public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
            this.el = eventListener;
        }

        public boolean supportsEvents() {
            return false;
        }

        void changeProperties(ESAPServerType serverType, Properties properties) {
            final String destinationName = serverType.name();
            if (properties == null) {
                if (el != null)
                    el.deleted(destinationName);

                destTypesPropertiesMap.remove(serverType);
            } else {
                Properties oldProp = destTypesPropertiesMap.get(serverType);
                if (oldProp == null || !oldProp.equals(properties)) {
                    destTypesPropertiesMap.put(serverType, properties);

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
                throw new RuntimeException(Messages.getString("SAPDataManager.Unavailable_Server", serverName)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return prop;
        }

        @Override
        public void setServerDataEventListener(ServerDataEventListener eventListener) {
            this.el = eventListener;
        }

        @Override
        public boolean supportsEvents() {
            return false;
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
    private final Map<String, CustomDestinationDataProvider> clientDataProvidersMap;
    private final Map<String, CustomServerDataProvider> serverDataProvidersMap;
    private CustomDestinationDataProvider previousClientDataProvider;
    private CustomServerDataProvider previousServerDataProvider;

    private SAPDataManager() {
        clientDataProvidersMap = new ConcurrentHashMap<>(5);
        serverDataProvidersMap = new ConcurrentHashMap<>(5);
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
            if (previousClientDataProvider != null)
                Environment.unregisterDestinationDataProvider(previousClientDataProvider);
        } catch (IllegalStateException e) {
            // nothing to do
        }

        // wrong previous, try to remove from list one by one
        for (Entry<String, CustomDestinationDataProvider> entry : clientDataProvidersMap.entrySet()) {
            try {
                Environment.unregisterDestinationDataProvider(entry.getValue());
            } catch (IllegalStateException e2) {
                // nothing to do
            }
        }
    }

    public void unregisterServer() {
        try {
            if (previousServerDataProvider != null)
                Environment.unregisterServerDataProvider(previousServerDataProvider);
        } catch (IllegalStateException e) {
            // nothing to do
        }

        // wrong previous, try to remove from list one by one
        for (Entry<String, CustomServerDataProvider> entry : serverDataProvidersMap.entrySet()) {
            try {
                Environment.unregisterServerDataProvider(entry.getValue());
            } catch (IllegalStateException e2) {
                // nothing to do
            }
        }
    }

    /**
     * set the client properties via provider, else will be xxx.jcoDestination file
     */
    public synchronized void updateClientProp(String identifier, ESAPServerType serverType, Properties properties) {
        if (serverType != null) {
            CustomDestinationDataProvider customDestinationDataProvider = clientDataProvidersMap.get(identifier);
            if (customDestinationDataProvider == null) {
                customDestinationDataProvider = new CustomDestinationDataProvider();
                clientDataProvidersMap.put(identifier, customDestinationDataProvider);
            }
            customDestinationDataProvider.changeProperties(serverType, properties);
        }
    }

    /**
     * set the server properties via provider, else will be xxx.jcoServer file
     */
    public synchronized void updateServerProp(String identifier, String serverName, Properties properties) {
        if (serverName != null) {
            CustomServerDataProvider customServerDataProvider = serverDataProvidersMap.get(identifier);
            if (customServerDataProvider == null) {
                customServerDataProvider = new CustomServerDataProvider();
                serverDataProvidersMap.put(identifier, customServerDataProvider);
            }
            customServerDataProvider.changeProperties(serverName, properties);
        }
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
        CustomDestinationDataProvider customDestinationDataProvider = clientDataProvidersMap.get(identifier);
        if (customDestinationDataProvider == null) {
            return null;
        }
        if (Environment.isDestinationDataProviderRegistered()) {//
            if (previousClientDataProvider != customDestinationDataProvider) {
                unregisterClient();

                Environment.registerDestinationDataProvider(customDestinationDataProvider);
            }
        } else {
            Environment.registerDestinationDataProvider(customDestinationDataProvider);
        }

        previousClientDataProvider = customDestinationDataProvider;

        JCoDestination jcoDest = JCoDestinationManager.getDestination(serverType.name());
        return jcoDest;
    }

    public synchronized JCoServer getServer(String identifier, String serverName) throws JCoException {
        if (identifier == null) {
            return null;
        }
        CustomServerDataProvider customServerDataProvider = serverDataProvidersMap.get(identifier);
        if (customServerDataProvider == null) {
            return null;
        }
        if (Environment.isServerDataProviderRegistered()) {//
            if (previousServerDataProvider != customServerDataProvider) {
                unregisterServer();

                Environment.registerServerDataProvider(customServerDataProvider);
            }
        } else {
            Environment.registerServerDataProvider(customServerDataProvider);
        }

        previousServerDataProvider = customServerDataProvider;

        JCoServer server = JCoServerFactory.getServer(serverName);
        return server;
    }
}
