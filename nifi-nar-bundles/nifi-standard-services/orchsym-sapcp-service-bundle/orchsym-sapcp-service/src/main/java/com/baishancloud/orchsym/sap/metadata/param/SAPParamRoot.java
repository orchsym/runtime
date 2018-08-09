package com.baishancloud.orchsym.sap.metadata.param;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author GU Guoqiang
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SAPParamRoot {

    @JsonProperty("import")
    private List<SAPParam> importParams;

    @JsonProperty("export")
    private List<SAPParam> exportParams;

    @JsonProperty("tables")
    private List<SAPTabParam> tableParams;

    public List<SAPParam> getImportParams() {
        return importParams;
    }

    public void setImportParams(List<SAPParam> importParams) {
        this.importParams = importParams;
    }

    public List<SAPParam> getExportParams() {
        return exportParams;
    }

    public void setExportParams(List<SAPParam> exportParams) {
        this.exportParams = exportParams;
    }

    public List<SAPTabParam> getTableParams() {
        return tableParams;
    }

    public void setTableParams(List<SAPTabParam> tableParams) {
        this.tableParams = tableParams;
    }

    public static class Writer {

        public String write(SAPParamRoot root) throws JsonProcessingException {
            if (root == null) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(root);
        }
    }

    public static class Parser {

        public SAPParamRoot parse(String value) throws JsonProcessingException, IOException {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, SAPParamRoot.class);
        }
    }
}
