package org.apache.nifi.web.httprequestui.model;

public class ParameterInfo {

	public String name;

	// 参数位置: query, header, path, cookie, formData etc
	public String position;
	public boolean required = true;

	//参数类型: string, number, boolean
	public String type;
	public String format;
	public String consumes;

	public String ref;
	
	public String description;
	
}
