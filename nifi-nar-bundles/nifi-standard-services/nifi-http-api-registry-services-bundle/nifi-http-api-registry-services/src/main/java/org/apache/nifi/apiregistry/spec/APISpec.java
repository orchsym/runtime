package org.apache.nifi.apiregistry.spec;

import java.util.ArrayList;
import java.util.Map; 

public class APISpec {

	public String swagger = "2.0";
 	public InfoSpec info;
 	public String host;
 	public ArrayList<String> schemes;
 	public String basePath;
 	// public ArrayList<String> consumes;
 	public ArrayList<String> produces;

 	/*          uri        method         */
 	public Map<String, Map<String, PathSpec>> paths;

 	/*   model name,    properties,     */
 	public Map<String, Map<String, Map<String, PropertySpec>>> definitions;
}
