package org.apache.nifi.processors.mapper.exp;

/**
 * Types of Table
 * 
 * @author GU Guoqiang
 */
public enum MapperTableType {
    INPUT, OUTPUT;

    public String getPrefix() {
        return this.name().toLowerCase() + '.';
    }

    public String getPrefix(String name) {
        return this.name().toLowerCase() + '.' + name;
    }
}
