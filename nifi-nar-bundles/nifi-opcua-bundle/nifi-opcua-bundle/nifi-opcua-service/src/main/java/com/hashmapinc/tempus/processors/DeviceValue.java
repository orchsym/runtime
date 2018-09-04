package com.hashmapinc.tempus.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DeviceValue {

    private List<DataValue> dataValues;

    public List<DataValue> getDataValues(){
        return dataValues;
    }

    public DeviceValue(){
        dataValues = new ArrayList<>();
    }

    public void addDataValue(DataValue value){
        dataValues.add(value);
    }
}