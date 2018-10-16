package org.apache.nifi.web.api.dto.stats;

import java.util.List;

import org.apache.nifi.web.api.dto.ProcessorDTO;

/**
 * @author GU Guoqiang
 *
 */
public class ProcessorCounterDTO {
    private String name;
    private String version;
    private Long count;
    private Long propertiesCount;

    private List<ProcessorDTO> details;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public List<ProcessorDTO> getDetails() {
        return details;
    }

    public void setDetails(List<ProcessorDTO> details) {
        this.details = details;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        ProcessorCounterDTO other = (ProcessorCounterDTO) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

}
