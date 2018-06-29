package org.apache.nifi.web.httprequestui.model;

import java.util.ArrayList;

public class PropertyInfoEntity {

	public String processorId = "";

	public String clientId = "";

	public Long revision = 0L;

	public ArrayList<String> method = new ArrayList();

	public String version = "1.0.0";

	public String title = "";

	public String path = "";

	public String host = "";

	public String basePath = "/";

	public String description = "";

	public String summary = "";

	public ArrayList<String> contentType = new ArrayList();

	public ArrayList<ParameterInfo> parameters = new ArrayList();

	public ArrayList<RespInfo> respInfos = new ArrayList();

	public ArrayList<RespModel> respModels = new ArrayList();
}
