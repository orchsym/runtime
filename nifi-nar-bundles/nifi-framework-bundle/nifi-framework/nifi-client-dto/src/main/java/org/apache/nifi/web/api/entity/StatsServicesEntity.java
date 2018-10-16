package org.apache.nifi.web.api.entity;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.nifi.web.api.dto.stats.ControllerServiceCounterDTO;

/**
 * A serialized representation of this class can be placed in the entity body of a response to the API. This particular entity holds a reference to the counter of controller services, processors .
 */
@XmlRootElement(name = "StatsProcessorsEntity")
public class StatsServicesEntity extends Entity {

    private List<ControllerServiceCounterDTO> services;

    public List<ControllerServiceCounterDTO> getServices() {
        return services;
    }

    public void setServices(List<ControllerServiceCounterDTO> services) {
        this.services = services;
    }

}
