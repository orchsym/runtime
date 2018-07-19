package org.apache.nifi.apiregistry.spec; 
import java.util.Map;

public class ParamSpec {

	public String name;
	public String in;
	public String description;
	public String type;
	public String format;
	public Boolean required;
	public Map<String, String> schema;
}