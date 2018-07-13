package org.apache.nifi.processors.mapper.exp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpVarTable {
    private String name;

    @JsonIgnoreProperties
    @JsonIgnore
    private VarTableType type = VarTableType.GLOBAL;

    private List<ExpVar> vars;

    public ExpVarTable() {
        super();
    }

    public ExpVarTable(String name, ExpVar... expVars) {
        this(name, VarTableType.GLOBAL, expVars);
    }

    public ExpVarTable(String name, VarTableType type, ExpVar... expVars) {
        this();
        this.name = name;
        this.type = type;
        if (expVars != null)
            setVars(Arrays.asList(expVars));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VarTableType getType() {
        return type;
    }

    public void setType(VarTableType type) {
        this.type = type;
    }

    public List<ExpVar> getVars() {
        if (vars == null) {
            vars = new ArrayList<>();
        }
        return vars;
    }

    public void setVars(List<ExpVar> vars) {
        this.vars = vars;
    }

    public String getVarPrefix() {
        final VarTableType varTableType = getType();
        // global without the table name
        final String varPrefix = (varTableType == VarTableType.GLOBAL) ? varTableType.getPrefix() : varTableType.getPrefix(getName());
        return varPrefix;
    }

    /**
     * parse the value to ExpVarTable
     */
    public static class Parser {
        public ExpVarTable parse(String value) throws IOException {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(value, ExpVarTable.class);
        }
    }

    /**
     * write the ExpVarTable to string
     */
    public static class Writer {
        public String write(ExpVarTable table) throws IOException {
            if (table == null) {
                return "";
            }
            final ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(table);
        }
    }
}
