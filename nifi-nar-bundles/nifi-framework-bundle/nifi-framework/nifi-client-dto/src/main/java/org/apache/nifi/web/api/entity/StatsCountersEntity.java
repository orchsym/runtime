package org.apache.nifi.web.api.entity;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.nifi.web.api.dto.stats.StatsCounterDTO;

/**
 * A serialized representation of this class can be placed in the entity body of a response to the API. This particular entity holds a reference to the counter of controller services, processors .
 */
@XmlRootElement(name = "StatsCountersEntity")
public class StatsCountersEntity extends StatsTimeEntity {

    private StatsCounterDTO counters;

    public StatsCounterDTO getCounters() {
        return counters;
    }

    public void setCounters(StatsCounterDTO counters) {
        this.counters = counters;
    }

}
