package com.baishancloud.orchsym.sap.metadata.param;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author GU Guoqiang
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SAPTabParam extends AbsParam {
    private List<SAPParam> params;

    @JsonIgnore
    @JsonIgnoreProperties
    private int tableLength;

    @JsonIgnore
    @JsonIgnoreProperties
    private int tableNUCLength;

    /**
     * when table, will have value
     */
    @JsonIgnore
    public ESAPTabType getTabType() {
        return ESAPTabType.get(this.getType());
    }

    public List<SAPParam> getParams() {
        return params;
    }

    public void setParams(List<SAPParam> params) {
        this.params = params;
    }

    public int getLength() {
        if (tableLength == 0) { // children length
            int tableLen = 0;
            final List<SAPParam> children = getParams();
            if (children != null) {
                for (SAPParam child : children) {
                    tableLen += child.getLength();
                }
            }
            tableLength = tableLen;
        }
        return tableLength;
    }

    public int getNucLength() {
        if (tableNUCLength == 0) { // children length
            int tableLen = 0;
            final List<SAPParam> children = getParams();
            if (children != null) {
                for (SAPParam child : children) {
                    tableLen += child.getNucLength();
                }
            }
            tableNUCLength = tableLen;
        }
        return tableNUCLength;
    }

}
