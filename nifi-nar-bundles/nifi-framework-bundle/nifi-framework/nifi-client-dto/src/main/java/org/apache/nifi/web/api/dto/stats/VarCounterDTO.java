package org.apache.nifi.web.api.dto.stats;

import org.apache.nifi.web.api.entity.VariableRegistryEntity;

/**
 * @author GU Guoqiang
 *
 */
public class VarCounterDTO {
    private Long count;
    private VariableRegistryEntity detail;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public VariableRegistryEntity getDetail() {
        return detail;
    }

    public void setDetail(VariableRegistryEntity detail) {
        this.detail = detail;
    }

}
