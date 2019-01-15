/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.web.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.controller.status.RunStatus;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.ResourceNotFoundException;
import org.apache.nifi.web.Revision;
import org.apache.nifi.web.api.entity.ProcessorEntity;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.FlowSnippetDTO;
import org.apache.nifi.util.FormatUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

/**
 * RESTful endpoint for retrieving a Processor or Process Group's validation informations.
 */
@Path(ValidationResource.PATH)
@Api(value = ValidationResource.PATH, //
        description = "Endpoint  for retrieving a Processor or Process Group's validation informations, return the processors that validate failed")
public class ValidationResource extends ApplicationResource {
    public static final String PATH = "/validation";

    private static final Logger logger = LoggerFactory.getLogger(ValidationResource.class);
    private NiFiServiceFacade serviceFacade;
    private FlowController flowController;

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/snippet/{id}")
    @ApiOperation(value = "Gets a processor's validation informations", //
            response = ArrayList.class)
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = StatsResource.CODE_MESSAGE_400), //
            @ApiResponse(code = 404, message = StatsResource.CODE_MESSAGE_401), //
            @ApiResponse(code = 409, message = StatsResource.CODE_MESSAGE_409) //
    })
    public Response validateProcessor(//
            @ApiParam(value = "The processor id.", required = true) //
            @PathParam("id") final String id//
    ) {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        List<String> blacklist = null;
        try {
            blacklist = loadBlacklist();
        } catch (Exception e) {
            logger.error("Failed to parse the scheduling period validation blacklist");
            return Response.serverError().entity("Failed to parse the scheduling period validation blacklist. " + e.getMessage()).build();
        }

        List<Map<String, String>> resultArr = retrieve(blacklist, id);

        // generate the response
        Gson gson = new Gson();
        String resultStr = gson.toJson(resultArr);

        return noCache(Response.ok(resultStr)).build();
    }

    private List<Map<String, String>> retrieve(List<String> blacklist, String id) {
        if (blacklist == null || blacklist.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> resultArr = new ArrayList<>();
        
        resultArr.addAll(retrieveProcessor(blacklist, id));

        // snippet, select the processor and group
        try {
            final Set<Revision> requestRevisions = serviceFacade.getRevisionsFromSnippet(id);
            for (Revision rev : requestRevisions) {
                final String componentId = rev.getComponentId();
                resultArr.addAll(retrieveProcessor(blacklist, componentId));
            }
        } catch (ResourceNotFoundException e) {
            // not snippet
        }
        return resultArr;
    }

    private List<Map<String, String>> retrieveProcessor(List<String> blacklist, String id) {
        if (blacklist == null || blacklist.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> resultArr = new ArrayList<>();
        // processor
        try {
            Map<String, String> warnResult = generateProcessorWarnInfo(id, blacklist);
            if (warnResult != null) {
                resultArr.add(warnResult);
            }
        } catch (ResourceNotFoundException e) {
            // not processor, try others
        }

        // processor group
        try {
            List<String> idArr = new ArrayList<>();
            collectProcessorsFromGroup(id, idArr);
            for (String processorId : idArr) {
                Map<String, String> warnResult = generateProcessorWarnInfo(processorId, blacklist);
                if (warnResult != null) {
                    resultArr.add(warnResult);
                }
            }
        } catch (ResourceNotFoundException e) {
            // not group, try others
        }

        return resultArr;
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/processor/{id}")
    @ApiOperation(value = "Gets a processor's validation informations", //
            response = ArrayList.class)
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = StatsResource.CODE_MESSAGE_400), //
            @ApiResponse(code = 404, message = StatsResource.CODE_MESSAGE_401), //
            @ApiResponse(code = 409, message = StatsResource.CODE_MESSAGE_409) //
    })
    public Response handleProcessor(//
            @ApiParam(value = "The processor id.", required = true) //
            @PathParam("id") final String id//
    ) {
        return validateProcessor(id);
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/process-group/{id}")
    @ApiOperation(value = "Gets all processor's validation informations in a process group", //
            response = ArrayList.class)
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = StatsResource.CODE_MESSAGE_400), //
            @ApiResponse(code = 404, message = StatsResource.CODE_MESSAGE_401), //
            @ApiResponse(code = 409, message = StatsResource.CODE_MESSAGE_409) //
    })
    public Response handleProcessorGroup(//
            @ApiParam(value = "The group id.", required = true) //
            @PathParam("id") final String id //
    ) {
        return validateProcessor(id);
    }

    // 针对那些可运行的起点组件，同时执行安排周期设置为0秒，且处于黑名单的组件，生成相应的警告信息
    private Map<String, String> generateProcessorWarnInfo(String id, List<String> blacklist) {
        final ProcessorEntity entity = serviceFacade.getProcessor(id);
        String type = entity.getComponent().getType();
        String typeName = type.substring(type.lastIndexOf('.') + 1);
        if (!blacklist.contains(typeName)) { // 不在黑名单中，忽略
            return null;
        }
        String schedulingPeriod = entity.getComponent().getConfig().getSchedulingPeriod();
        long schedulingNanos = 0L;
        if (StringUtils.isNotBlank(schedulingPeriod)) {
            schedulingNanos = FormatUtils.getTimeDuration(schedulingPeriod, TimeUnit.SECONDS);
        }
        String runStatus = entity.getStatus().getRunStatus();
        if (!runStatus.equals(RunStatus.Stopped.name()) || schedulingNanos != 0) {// 起点组件处于非停止状态，或schedulingPeriod已设置大于1秒，则忽略
            return null;
        }

        boolean hasIncomingConnection = flowController.getProcessorNode(id).hasIncomingConnection();
        if (hasIncomingConnection) { // 非起点组件，忽略
            return null;
        }

        Map<String, String> mapObj = new HashMap<>();
        String name = entity.getComponent().getName();
        String groupId = entity.getStatus().getGroupId();

        List<String> groupLevel = new ArrayList<>();
        collectProcessorGroupPath(groupId, groupLevel);
        Collections.reverse(groupLevel);

        mapObj.put("name", name);
        mapObj.put("id", id);
        mapObj.put("type", typeName);
        mapObj.put("groupId", groupId);
        mapObj.put("path", String.join("/", groupLevel));

        return mapObj;

    }

    private List<String> loadBlacklist() throws Exception {
        Gson gson = new Gson();
        final InputStream stream = ValidationResource.class.getResourceAsStream("/json/scheduling_period_validation_blacklist.json");
        String jsonStr = IOUtils.toString(stream, StandardCharsets.UTF_8);

        JsonObject object = new JsonParser().parse(jsonStr).getAsJsonObject();
        JsonArray objectArr = object.getAsJsonArray("blacklist");
        ArrayList<String> blacklist = gson.fromJson(objectArr, new TypeToken<ArrayList<String>>() {
        }.getType());
        return blacklist;
    }

    // 递归获取group下所有组件id列表
    private void collectProcessorsFromGroup(String groupId, List<String> ids) {
        final ProcessGroupEntity entity = serviceFacade.getProcessGroup(groupId);
        final FlowSnippetDTO dto = entity.getComponent().getContents();
        final Set<ProcessGroupDTO> processGroupDTO = dto.getProcessGroups();
        final Set<ProcessorDTO> processorsDTO = dto.getProcessors();
        if (processorsDTO != null)
            for (ProcessorDTO pDTO : processorsDTO)
                ids.add(pDTO.getId());

        if (processGroupDTO != null)
            for (ProcessGroupDTO pgDTO : processGroupDTO)
                collectProcessorsFromGroup(pgDTO.getId(), ids);

    }

    // 获取当前group所在的路径
    private void collectProcessorGroupPath(String groupId, List<String> groupLevel) {
        if (flowController.getRootGroupId().equals(groupId)) {
            return;
        }
        final ProcessGroupEntity processGroupEntity = serviceFacade.getProcessGroup(groupId);
        groupLevel.add(processGroupEntity.getComponent().getName());

        final String parentGroupId = processGroupEntity.getComponent().getParentGroupId();
        collectProcessorGroupPath(parentGroupId, groupLevel);
    }

    // setters
    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public void setFlowController(FlowController flowController) {
        super.setFlowController(flowController);
        this.flowController = flowController;
    }

}
