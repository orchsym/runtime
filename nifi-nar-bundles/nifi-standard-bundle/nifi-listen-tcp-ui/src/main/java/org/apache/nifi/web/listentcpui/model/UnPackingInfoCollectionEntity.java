package org.apache.nifi.web.listentcpui.model;

import java.util.ArrayList;

public class UnPackingInfoCollectionEntity {

	public String processorId = "";

	public String clientId = "";

	public Long revision = 0L;

	//解包顺序与该infos数组的顺序一致
	public ArrayList<UnPackingInfoEntity> infos = new ArrayList();
	
}

// {
// 	"processorId": "123",
// 	"infos": [{
// 		"name": "mobileNo",
// 		"length": 12
// 	}, {
// 		"name": "state",
// 		"length": 5
// 	}, {
// 		"name": "message",
// 		"length": 20
// 	}]
// }