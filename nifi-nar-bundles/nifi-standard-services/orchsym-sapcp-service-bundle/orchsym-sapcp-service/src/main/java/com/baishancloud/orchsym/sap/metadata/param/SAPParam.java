package com.baishancloud.orchsym.sap.metadata.param;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author GU Guoqiang
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SAPParam extends AbsParam {
    /*
     * 
     * ucByteLength - Data field length in bytes for Unicode layout
     * 
     * recordUCLength - the byte length of the structure with Unicode layout
     * 
     * 
     */
    private int length;

    /*
     * nucByteLength - Data field length in bytes for non-Unicode layout
     * 
     * recordNUCLength - the byte length of the structure with non-Unicode layout
     */
    private int nucLength;

    /*
     * required for TYPE_BCD or TYPE_FLOAT, this is for Data field number of decimals
     */
    private int precision;

    @JsonProperty("default")
    private String defaultValue;

    /*
     * structure or table
     */
    @JsonProperty("tab")
    private SAPTabParam tabMeta;

    @JsonIgnore
    public ESAPMetaType getMetaType() {
        return ESAPMetaType.get(getType());
    }

    public int getLength() {
        if (tabMeta != null) { // use the table or structure length
            return tabMeta.getLength();
        }
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getNucLength() {
        if (tabMeta != null) { // use the table or structure length
            return tabMeta.getNucLength();
        }
        if (nucLength == 0) { // reuse the unicode length
            return getLength();
        }
        return nucLength;
    }

    public void setNucLength(int nucLength) {
        this.nucLength = nucLength;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public SAPTabParam getTabMeta() {
        return tabMeta;
    }

    public void setTabMeta(SAPTabParam tabMeta) {
        this.tabMeta = tabMeta;
    }

}
