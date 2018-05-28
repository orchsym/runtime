package org.apache.nifi.web.httprequestui.model;

import java.util.Map;

public class RespModel {

	public String name;

	/*    propertyName   <type->tpyename, description->description>*/
	public Map<String, Map<String, String>> properties;

}
