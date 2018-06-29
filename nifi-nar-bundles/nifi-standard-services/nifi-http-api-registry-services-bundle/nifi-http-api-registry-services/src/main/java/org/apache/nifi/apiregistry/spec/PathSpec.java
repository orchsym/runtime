package org.apache.nifi.apiregistry.spec;

import java.util.ArrayList;
import java.util.Map; 


public class PathSpec {

	public String summary;
	public String description;
	public ArrayList<ParamSpec> parameters;

	public ArrayList<String> produces;

	/*          code           */
	public Map<String, RespSpec> responses;
}