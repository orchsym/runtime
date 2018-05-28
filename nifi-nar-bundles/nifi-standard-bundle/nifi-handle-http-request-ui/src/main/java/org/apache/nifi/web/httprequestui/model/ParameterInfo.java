package org.apache.nifi.web.httprequestui.model;

public class ParameterInfo {

	public String name;

	// 参数位置: query, header, path, cookie
	public String position;
	public boolean required = true;

	//参数类型: string, interger, boolean, number, array, object
	public String type;

	public String defaultValue;
	public String description;
}
