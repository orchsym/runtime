package org.apache.nifi.apiregistry;

public class ApiInfo {

	public String name;

	public String id;

	public String groupID = "";

	public String path;

	public String host;

	public String charset = "UTF-8";

	public String scheme = "http";

	public int port = 80;

	public long requestTimeout = 60000;

	public Boolean allowGet = true;

	public Boolean allowPost = true;

	public Boolean allowPut = true;

	public Boolean allowDelete = true;

	public Boolean allowHead = false;

	public Boolean allowOptions = false;	

	public String state = "init";  // init running stopped
}
