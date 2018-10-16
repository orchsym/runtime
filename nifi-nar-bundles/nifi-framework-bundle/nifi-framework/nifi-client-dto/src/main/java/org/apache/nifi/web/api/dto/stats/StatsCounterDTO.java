package org.apache.nifi.web.api.dto.stats;

import java.util.List;

/**
 * 
 * @author GU Guoqiang
 *
 */
public class StatsCounterDTO {
    private SummaryCounterDTO summary;

    private List<ProcessorCounterDTO> processors;
    private List<ControllerServiceCounterDTO> services;

    public SummaryCounterDTO getSummary() {
        return summary;
    }

    public void setSummary(SummaryCounterDTO summary) {
        this.summary = summary;
    }

    public List<ProcessorCounterDTO> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorCounterDTO> processors) {
        this.processors = processors;
    }

    public List<ControllerServiceCounterDTO> getServices() {
        return services;
    }

    public void setServices(List<ControllerServiceCounterDTO> services) {
        this.services = services;
    }

}
