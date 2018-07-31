package com.baishancloud.orchsym.sap.metadata.param;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbsParam {
    @JsonProperty(required = true)
    private String name;

    private String desc;
    /*
     * if table, it's ESAPTabType. if param, it's ESAPMetaType
     */
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbsParam other = (AbsParam) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AbsParam [name=" + name + ", type=" + type + "]";
    }

    public abstract int getLength();

    public int getNucLength() {
        return getLength();
    }
}
