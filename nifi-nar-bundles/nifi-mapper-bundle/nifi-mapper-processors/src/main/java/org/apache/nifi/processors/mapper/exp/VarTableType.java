package org.apache.nifi.processors.mapper.exp;

/**
 * Types of Var Table
 * 
 * @author GU Guoqiang
 */
public enum VarTableType {
    INPUT, OUTPUT, GLOBAL;
    public static final String VAR = "var";

    public String getPrefix() {
        return this.name().toLowerCase() + '.' + VAR;
    }

    public String getPrefix(String name) {
        return getPrefix() + '.' + name;
    }

    public static VarTableType matchTableType(MapperTableType tableType) {
        if (tableType == MapperTableType.INPUT) {
            return INPUT;
        }
        if (tableType == MapperTableType.OUTPUT) {
            return OUTPUT;
        }
        return GLOBAL;
    }
}
