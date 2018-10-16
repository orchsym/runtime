package org.apache.nifi.web.api.entity;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.nifi.web.api.dto.stats.VarCounterDTO;

/**
 * A serialized representation of this class can be placed in the entity body of a response to the API. This particular entity holds a reference to the counter of controller services, processors .
 */
@XmlRootElement(name = "StatsVarsEntity")
public class StatsVarsEntity extends Entity {

    private List<VarCounterDTO> vars;

    public List<VarCounterDTO> getVars() {
        return vars;
    }

    public void setVars(List<VarCounterDTO> vars) {
        this.vars = vars;
    }


}
