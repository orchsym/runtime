package org.apache.nifi.registry.api;

public class ApiInfo implements Cloneable {
    public enum State {
        init, running, stopped;
    }

    public String name;

    public String id;

    public String groupID = "";

    public String path;

    public String host;

    public String charset = "UTF-8";

    public String scheme = "http";

    public Integer port = 80;

    // 1 min, same as the default value of REQUEST_EXPIRATION in StandardHttpContextMap
    public Long requestTimeout = 1 * 60 * 1000L;

    public Boolean allowGet = false;

    public Boolean allowPost = false;

    public Boolean allowPut = false;

    public Boolean allowDelete = false;

    public Boolean allowHead = false;

    public Boolean allowOptions = false;

    public String state = State.init.name(); // init running stopped

    public transient String controllerServiceId;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public ApiInfo copy() {
        ApiInfo copied = null;
        try {
            copied = (ApiInfo) this.clone();

            copied.controllerServiceId = null; // remove
        } catch (CloneNotSupportedException e) {
            //
        }

        return copied;
    }

}
