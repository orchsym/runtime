package com.baishancloud.orchsym.processors.sap.event;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author GU Guoqiang
 *
 */
public class SAPTCPEvent {
    public volatile AtomicBoolean finished = new AtomicBoolean(false);
    public volatile String data;

    public String getAndClean() {
        // set for next request
        finished.set(false);
        
        String curData = data;
        data = null;

        return curData;
    }

    public String finishAndSet(String data) {
        finished.set(true);
        
        String oldData = this.data;
        this.data = data;

        return oldData;
    }

    public void finishAndClean() {
        finished.set(true);
        data = null; // clean data
    }
}
