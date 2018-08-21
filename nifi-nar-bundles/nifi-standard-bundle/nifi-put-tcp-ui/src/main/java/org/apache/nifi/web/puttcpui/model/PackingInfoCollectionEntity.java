package org.apache.nifi.web.puttcpui.model;

import java.util.ArrayList;

public class PackingInfoCollectionEntity {

	public String processorId = "";

	public String clientId = "";

	public Long revision = 0L;

	public String alignType = "align-left"; //align-left(default), align-right

	//字段内容写入tcp的顺序与该infos数组的顺序一致
	public ArrayList<PackingInfoEntity> infos = new ArrayList();
	
}

// {
// 	"processorId": "123",
// 	"infos": [{
// 		"name": "custName",
// 		"length": 6,
// 		"stuffing": " ",
// 		"charSet": "GBK"
// 	}, {
// 		"name": "mobileNo",
// 		"length": 12
// 	}, {
// 		"name": "idNo",
// 		"length": 18
// 	}, {
// 		"name": "reportSn",
// 		"length": 24
// 	}]
// }