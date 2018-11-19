package org.apache.nifi.web.api.dto.stats;

import java.util.Map;

/**
 * @author GU Guoqiang
 *
 */
public class SummaryCounterDTO {
    private Integer runningCount;
    private Integer stoppedCount;
    private Integer invalidCount;
    private Integer disabledCount;

    private Integer activeRemotePortCount;
    private Integer inactiveRemotePortCount;

    private Integer upToDateCount;
    private Integer locallyModifiedCount;
    private Integer staleCount;
    private Integer locallyModifiedAndStaleCount;
    private Integer syncFailureCount;

    private Integer inputPortCount;
    private Integer outputPortCount;

    private Long processorCount; // all processor for runtime to load
    private Long processorOwnedCount; // provide by us
    private Long processorUsedCount;
    private Long processorUsedTotalCount;
    private Long processorUsedPropertiesCount;
    private Map<String, Long> processorI18nCount;

    private Long controllerCount; // all controller services for runtime to load
    private Long controllerOwnedCount; // provide by us
    private Long controllerUsedCount;
    private Long controllerUsedTotalCount;
    private Long controllerUsedPropertiesCount;
    private Map<String, Long> controllerI18nCount;

    private Long connectionCount;
    private Long funnelCount;
    private Long groupCount;
    private Long groupLeavesCount;
    private Long labelCount;
    private Long varCount;
    private Long templateCount;

    public Integer getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(Integer runningCount) {
        this.runningCount = runningCount;
    }

    public Integer getStoppedCount() {
        return stoppedCount;
    }

    public void setStoppedCount(Integer stoppedCount) {
        this.stoppedCount = stoppedCount;
    }

    public Integer getInvalidCount() {
        return invalidCount;
    }

    public void setInvalidCount(Integer invalidCount) {
        this.invalidCount = invalidCount;
    }

    public Integer getDisabledCount() {
        return disabledCount;
    }

    public void setDisabledCount(Integer disabledCount) {
        this.disabledCount = disabledCount;
    }

    public Integer getActiveRemotePortCount() {
        return activeRemotePortCount;
    }

    public void setActiveRemotePortCount(Integer activeRemotePortCount) {
        this.activeRemotePortCount = activeRemotePortCount;
    }

    public Integer getInactiveRemotePortCount() {
        return inactiveRemotePortCount;
    }

    public void setInactiveRemotePortCount(Integer inactiveRemotePortCount) {
        this.inactiveRemotePortCount = inactiveRemotePortCount;
    }

    public Integer getUpToDateCount() {
        return upToDateCount;
    }

    public void setUpToDateCount(Integer upToDateCount) {
        this.upToDateCount = upToDateCount;
    }

    public Integer getLocallyModifiedCount() {
        return locallyModifiedCount;
    }

    public void setLocallyModifiedCount(Integer locallyModifiedCount) {
        this.locallyModifiedCount = locallyModifiedCount;
    }

    public Integer getStaleCount() {
        return staleCount;
    }

    public void setStaleCount(Integer staleCount) {
        this.staleCount = staleCount;
    }

    public Integer getLocallyModifiedAndStaleCount() {
        return locallyModifiedAndStaleCount;
    }

    public void setLocallyModifiedAndStaleCount(Integer locallyModifiedAndStaleCount) {
        this.locallyModifiedAndStaleCount = locallyModifiedAndStaleCount;
    }

    public Integer getSyncFailureCount() {
        return syncFailureCount;
    }

    public void setSyncFailureCount(Integer syncFailureCount) {
        this.syncFailureCount = syncFailureCount;
    }

    public Integer getInputPortCount() {
        return inputPortCount;
    }

    public void setInputPortCount(Integer inputPortCount) {
        this.inputPortCount = inputPortCount;
    }

    public Integer getOutputPortCount() {
        return outputPortCount;
    }

    public void setOutputPortCount(Integer outputPortCount) {
        this.outputPortCount = outputPortCount;
    }

    public Long getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(Long connectionCount) {
        this.connectionCount = connectionCount;
    }

    public Long getFunnelCount() {
        return funnelCount;
    }

    public void setFunnelCount(Long funnelCount) {
        this.funnelCount = funnelCount;
    }

    public Long getProcessorCount() {
        return processorCount;
    }

    public void setProcessorCount(Long processorCount) {
        this.processorCount = processorCount;
    }

    public Long getProcessorOwnedCount() {
        return processorOwnedCount;
    }

    public void setProcessorOwnedCount(Long processorOwnedCount) {
        this.processorOwnedCount = processorOwnedCount;
    }

    public Long getProcessorUsedCount() {
        return processorUsedCount;
    }

    public void setProcessorUsedCount(Long processorUsedCount) {
        this.processorUsedCount = processorUsedCount;
    }

    public Long getProcessorUsedPropertiesCount() {
        return processorUsedPropertiesCount;
    }

    public void setProcessorUsedPropertiesCount(Long processorUsedPropertiesCount) {
        this.processorUsedPropertiesCount = processorUsedPropertiesCount;
    }

    public void setProcessorUsedTotalCount(Long processorUsedTotalCount) {
        this.processorUsedTotalCount = processorUsedTotalCount;
    }

    public Long getProcessorUsedTotalCount() {
        return processorUsedTotalCount;
    }

    public Long getControllerCount() {
        return controllerCount;
    }

    public void setControllerCount(Long controllerCount) {
        this.controllerCount = controllerCount;
    }

    public Long getControllerOwnedCount() {
        return controllerOwnedCount;
    }

    public void setControllerOwnedCount(Long controllerOwnedCount) {
        this.controllerOwnedCount = controllerOwnedCount;
    }

    public Long getControllerUsedCount() {
        return controllerUsedCount;
    }

    public void setControllerUsedCount(Long controllerUsedCount) {
        this.controllerUsedCount = controllerUsedCount;
    }

    public Long getControllerUsedTotalCount() {
        return controllerUsedTotalCount;
    }

    public void setControllerUsedTotalCount(Long controllerUsedTotalCount) {
        this.controllerUsedTotalCount = controllerUsedTotalCount;
    }

    public Long getControllerUsedPropertiesCount() {
        return controllerUsedPropertiesCount;
    }

    public void setControllerUsedPropertiesCount(Long controllerUsedPropertiesCount) {
        this.controllerUsedPropertiesCount = controllerUsedPropertiesCount;
    }

    public Long getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(Long groupCount) {
        this.groupCount = groupCount;
    }

    public Long getGroupLeavesCount() {
        return groupLeavesCount;
    }

    public void setGroupLeavesCount(Long groupLeavesCount) {
        this.groupLeavesCount = groupLeavesCount;
    }

    public Long getLabelCount() {
        return labelCount;
    }

    public void setLabelCount(Long labelCount) {
        this.labelCount = labelCount;
    }

    public Long getVarCount() {
        return varCount;
    }

    public void setVarCount(Long varCount) {
        this.varCount = varCount;
    }

    public Long getTemplateCount() {
        return templateCount;
    }

    public void setTemplateCount(Long templateCount) {
        this.templateCount = templateCount;
    }

    public Map<String, Long> getProcessorI18nCount() {
        return processorI18nCount;
    }

    public void setProcessorI18nCount(Map<String, Long> processorI18nCount) {
        this.processorI18nCount = processorI18nCount;
    }

    public Map<String, Long> getControllerI18nCount() {
        return controllerI18nCount;
    }

    public void setControllerI18nCount(Map<String, Long> controllerI18nCount) {
        this.controllerI18nCount = controllerI18nCount;
    }

}
