package org.apache.nifi.web.api.dto.stats;

import java.util.List;

import org.apache.nifi.web.api.dto.ControllerServiceDTO;

/**
 * @author GU Guoqiang
 *
 */
public class ControllerServiceCounterDTO {
    private String service;
    private Long count;
    private Long propertiesCount;

    private List<ControllerServiceDTO> details;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getPropertiesCount() {
        return propertiesCount;
    }

    public void setPropertiesCount(Long propertiesCount) {
        this.propertiesCount = propertiesCount;
    }

    public List<ControllerServiceDTO> getDetails() {
        return details;
    }

    public void setDetails(List<ControllerServiceDTO> details) {
        this.details = details;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((service == null) ? 0 : service.hashCode());
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
        ControllerServiceCounterDTO other = (ControllerServiceCounterDTO) obj;
        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;
        return true;
    }

}
