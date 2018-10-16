package org.apache.nifi.web.api.entity;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.nifi.web.api.dto.util.TimestampAdapter;

import io.swagger.annotations.ApiModelProperty;

/**
 * A serialized representation of this class can be placed in the entity body of a response to the API. This particular entity holds a reference to the counter of controller services, processors .
 */
@XmlRootElement(name = "StatsTimeEntity")
public class StatsTimeEntity extends Entity {

    private Date reportTime;

    /**
     * @return current time on the server
     */
    @XmlJavaTypeAdapter(TimestampAdapter.class)
    @ApiModelProperty(value = "The current time on the system.", dataType = "string")
    public Date getReportTime() {
        return reportTime;
    }

    public void setReportTime(Date currentTime) {
        this.reportTime = currentTime;
    }
}
