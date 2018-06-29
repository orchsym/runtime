package org.apache.nifi.web.httprequestui.model;

import java.util.Map;
import java.util.ArrayList;

public class RespModel {

	public String id;

	public String name;

	public ArrayList<String> contentType;

	public String description;

	/*    propertyName   <type->tpyename, description->description>*/
	public Map<String, Map<String, String>> properties;

}
