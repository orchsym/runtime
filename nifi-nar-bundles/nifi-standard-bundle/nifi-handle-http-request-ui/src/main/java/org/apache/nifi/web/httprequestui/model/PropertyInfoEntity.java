package org.apache.nifi.web.httprequestui.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class PropertyInfoEntity {

	public String processorId = "";

	public String clientId = "";

	public Long revision = 0L;

	public ArrayList<String> methods = new ArrayList();

	public String version = "1.0.0";

	public String title = "";

	public String path = "/";

	public String host = "";

	public String basePath = "/";

	public String summary = "";

	public Map<String, String> description = new HashMap();

	public Map<String, ArrayList<String>> contentType = new HashMap();

	public Map<String, ArrayList<ParameterInfo>> parameters = new HashMap();;

	public Map<String, ArrayList<RespInfo>> respInfos = new HashMap();;

	public ArrayList<RespModel> respModels = new ArrayList();
}
