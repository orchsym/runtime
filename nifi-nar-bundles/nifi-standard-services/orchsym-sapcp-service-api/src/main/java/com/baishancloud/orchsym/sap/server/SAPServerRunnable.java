package com.baishancloud.orchsym.sap.server;

import java.util.concurrent.Callable;

/**
 * @author GU Guoqiang
 *
 */
public interface SAPServerRunnable extends Callable<Boolean> {

    boolean isRunning();

    void stop();
}
