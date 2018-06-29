package org.apache.nifi.processors.mapper.exp;

/**
 * Types of Var Table
 * 
 * @author GU Guoqiang
 */
public enum VarTableType {
    INPUT, OUTPUT, GLOBAL;
    public static final String VAR = "_var_";

    /**
     * global._var_.<var name>
     */
    public String getPrefix() {
        return this.name().toLowerCase() + '.' + VAR;
    }

    /**
     * only work for INPUT and OUTPUT.
     * 
     * input.<input name>._var_.<var name>, output.<output name>._var_.<var name>
     */
    public String getPrefix(String name) {
        return this.name().toLowerCase() + '.' + name + '.' + VAR;
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
