package org.apache.nifi.registry.api;

import java.util.ArrayList;
import java.util.Map; 


public class PathSpec {

	public String summary;
	public String description;
	public ArrayList<ParamSpec> parameters;

	public ArrayList<String> produces;
	public ArrayList<String> consumes;

	/*          code           */
	public Map<String, RespSpec> responses;
}