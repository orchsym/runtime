package org.apache.nifi.web.httprequestui.model;

import java.util.ArrayList;

public class PropertyInfoEntity {

	public String processorId;

	public String clientId;

	public Long revision;

	public String method = "get";

	public String version = "1.0.0";

	public String title;

	public String path;

	public String host;

	public String basePath;

	public String description;

	public String summary;

	public ArrayList<ParameterInfo> parameters;

	public ArrayList<RespInfo> respInfos;

	public ArrayList<RespModel> respModels;
}
