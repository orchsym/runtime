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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.groups.ProcessGroup;
import org.apache.nifi.nar.i18n.LanguageHelper;
import org.apache.nifi.nar.i18n.MessagesProvider;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.ResourceNotFoundException;
import org.apache.nifi.web.Revision;
import org.apache.nifi.web.api.dto.ControllerServiceDTO;
import org.apache.nifi.web.api.dto.DocumentedTypeDTO;
import org.apache.nifi.web.api.dto.ProcessorConfigDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.apache.nifi.web.api.dto.stats.ControllerServiceCounterDTO;
import org.apache.nifi.web.api.dto.stats.ProcessorCounterDTO;
import org.apache.nifi.web.api.dto.stats.StatsCounterDTO;
import org.apache.nifi.web.api.dto.stats.SummaryCounterDTO;
import org.apache.nifi.web.api.dto.stats.VarCounterDTO;
import org.apache.nifi.web.api.entity.ControllerServiceEntity;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;
import org.apache.nifi.web.api.entity.ProcessorEntity;
import org.apache.nifi.web.api.entity.StatsCountersEntity;
import org.apache.nifi.web.api.entity.StatsProcessorsEntity;
import org.apache.nifi.web.api.entity.StatsServicesEntity;
import org.apache.nifi.web.api.entity.StatsVarsEntity;
import org.apache.nifi.web.api.entity.TemplateEntity;
import org.apache.nifi.web.api.entity.VariableRegistryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * RESTful endpoint for statistics the flows and components.
 * 
 * @author GU Guoqiang
 */
@Path(StatsResource.PATH)
@Api(value = StatsResource.PATH, description = "Endpoint for accessing the statistics of flows and components.")
public class StatsResource extends ApplicationResource implements ICodeMessages{
    public static final String PATH = "/stats";

    private static final Logger logger = LoggerFactory.getLogger(StatsResource.class);


    private NiFiServiceFacade serviceFacade;
    private FlowController flowController;

    public StatsResource() {
        super();
    }

    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public void setFlowController(FlowController flowController) {
        super.setFlowController(flowController);
        this.flowController = flowController;
    }

    /**
     * Retrieves all the counters of services, processors.
     *
     * @return A StatsEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the summaries", //
            response = StatsCountersEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getCounters() {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        try {
            // create the response entity
            final StatsCountersEntity entity = new StatsCountersEntity();
            entity.setReportTime(new Date());
            StatsCounterDTO dto = new StatsCounterDTO();
            entity.setCounters(dto);

            final String rootGroupId = flowController.getRootGroupId();

            final SummaryCounterDTO summaryDTO = getSummaryDTO();
            dto.setSummary(summaryDTO);

            summaryDTO.setGroupCount(getGroupCount(rootGroupId, true));
            summaryDTO.setGroupLeavesCount(getGroupCount(rootGroupId, false));
            summaryDTO.setLabelCount(getLabelCount(rootGroupId));

            final List<VarCounterDTO> varCounter = getVarCount(rootGroupId, true);
            summaryDTO.setVarCount(varCounter.stream().collect(Collectors.summingLong(VarCounterDTO::getCount)));

            summaryDTO.setConnectionCount(getConnectionCount(rootGroupId));
            summaryDTO.setFunnelCount(getFunnelCount(rootGroupId));
            summaryDTO.setTemplateCount(getTemplateCount(rootGroupId));

            final List<ProcessorCounterDTO> processorCounters = getProcessorCounters(true);
            summaryDTO.setProcessorUsedCount((long) processorCounters.size());
            summaryDTO.setProcessorUsedTotalCount(processorCounters.stream().collect(Collectors.summingLong(ProcessorCounterDTO::getCount)));
            summaryDTO.setProcessorUsedPropertiesCount(processorCounters.stream()//
                    .filter(pc -> pc.getPropertiesCount() != null)//
                    .map(pc -> pc.getPropertiesCount())//
                    .reduce(Long::sum)//
                    .orElse(0L));
            dto.setProcessors(processorCounters.stream().sorted(new Comparator<ProcessorCounterDTO>() {

                @Override
                public int compare(ProcessorCounterDTO o1, ProcessorCounterDTO o2) {
                    int compare = o2.getCount() != null ? o2.getCount().compareTo(o1.getCount()) : 0;
                    if (compare != 0) {
                        return compare;
                    }
                    return o2.getName().compareTo(o1.getName());
                }
            }).collect(Collectors.toList()));

            final List<ControllerServiceCounterDTO> serviceCounters = getServiceCounters(true);
            summaryDTO.setControllerUsedCount((long) serviceCounters.size());
            summaryDTO.setControllerUsedTotalCount(serviceCounters.stream().collect(Collectors.summingLong(ControllerServiceCounterDTO::getCount)));
            summaryDTO.setControllerUsedPropertiesCount(serviceCounters.stream()//
                    .filter(csc -> csc.getPropertiesCount() != null)//
                    .map(csc -> csc.getPropertiesCount())//
                    .reduce(Long::sum)//
                    .orElse(0L));
            dto.setServices(serviceCounters.stream().sorted(new Comparator<ControllerServiceCounterDTO>() {

                @Override
                public int compare(ControllerServiceCounterDTO o1, ControllerServiceCounterDTO o2) {
                    int compare = o2.getCount() != null ? o2.getCount().compareTo(o1.getCount()) : 0;
                    if (compare != 0) {
                        return compare;
                    }
                    return o2.getService().compareTo(o1.getService());
                }
            }).collect(Collectors.toList()));

            // generate the response
            return generateOkResponse(entity).build();
        } catch (Throwable t) {
            return createExceptionResponse(t);
        }
    }

    private Response createExceptionResponse(Throwable t) {
        logger.error(t.getMessage(), t);

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw);) {
            t.printStackTrace(pw);

            return noCache(Response.serverError().entity(sw.toString())).build();
        }
    }

    /**
     * Retrieves all the details of processors.
     *
     * @return A StatsProcessorsEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/processors")
    @ApiOperation(value = "Gets all processors", //
            response = StatsProcessorsEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getProcessors() {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        try {
            final List<ProcessorCounterDTO> processorCounters = getProcessorCounters(false);
            StatsProcessorsEntity entity = new StatsProcessorsEntity();
            entity.setProcessors(processorCounters);
            // generate the response
            return generateOkResponse(entity).build();
        } catch (Throwable t) {
            return createExceptionResponse(t);
        }
    }

    /**
     * Retrieves all the details of Services.
     *
     * @return A StatsServicesEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/services")
    @ApiOperation(value = "Gets all Services", //
            response = StatsServicesEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getServices() {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        try {
            final List<ControllerServiceCounterDTO> serviceCounters = getServiceCounters(false);
            StatsServicesEntity entity = new StatsServicesEntity();
            entity.setServices(serviceCounters);
            // generate the response
            return generateOkResponse(entity).build();
        } catch (Throwable t) {
            return createExceptionResponse(t);
        }
    }

    /**
     * Retrieves all the details of Vars.
     *
     * @return A StatsVarsEntity.
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/vars")
    @ApiOperation(value = "Gets all vars", //
            response = StatsVarsEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getVars() {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        try {
            final List<VarCounterDTO> varCounter = getVarCount(flowController.getRootGroupId(), false);
            StatsVarsEntity entity = new StatsVarsEntity();
            entity.setVars(varCounter);

            // generate the response
            return generateOkResponse(entity).build();
        } catch (Throwable t) {
            return createExceptionResponse(t);
        }
    }

    private SummaryCounterDTO getSummaryDTO() {
        SummaryCounterDTO summaryDTO = new SummaryCounterDTO();
        final ProcessGroupEntity processGroup = serviceFacade.getProcessGroup(flowController.getRootGroupId());

        summaryDTO.setActiveRemotePortCount(processGroup.getActiveRemotePortCount());
        summaryDTO.setDisabledCount(processGroup.getDisabledCount());
        summaryDTO.setInactiveRemotePortCount(processGroup.getInactiveRemotePortCount());
        summaryDTO.setInputPortCount(processGroup.getInputPortCount());
        summaryDTO.setInvalidCount(processGroup.getInvalidCount());
        summaryDTO.setLocallyModifiedAndStaleCount(processGroup.getLocallyModifiedAndStaleCount());
        summaryDTO.setLocallyModifiedCount(processGroup.getLocallyModifiedCount());
        summaryDTO.setOutputPortCount(processGroup.getOutputPortCount());
        summaryDTO.setRunningCount(processGroup.getRunningCount());
        summaryDTO.setStaleCount(processGroup.getStaleCount());
        summaryDTO.setStoppedCount(processGroup.getStoppedCount());
        summaryDTO.setSyncFailureCount(processGroup.getSyncFailureCount());
        summaryDTO.setUpToDateCount(processGroup.getUpToDateCount());

        final Predicate<? super DocumentedTypeDTO> zhDescPredicate = type -> {
            String desc = MessagesProvider.getDescription(Locale.CHINESE, type.getType());
            if (StringUtils.isNotBlank(desc) && LanguageHelper.isChinese(desc)) {
                return true;
            }
            return false;
        };

        Map<String, Long> processorI18nCount = new HashMap<>();
        final Set<DocumentedTypeDTO> processorTypes = serviceFacade.getProcessorTypes(null, null, null);
        summaryDTO.setProcessorCount((long) processorTypes.size());
        processorI18nCount.put(Locale.CHINESE.getLanguage(), processorTypes.stream().filter(zhDescPredicate).count());
        summaryDTO.setProcessorI18nCount(processorI18nCount);

        Map<String, Long> controllerI18nCount = new HashMap<>();
        final Set<DocumentedTypeDTO> controllerTypes = serviceFacade.getControllerServiceTypes(null, null, null, null, null, null, null);
        summaryDTO.setControllerCount((long) controllerTypes.size());
        controllerI18nCount.put(Locale.CHINESE.getLanguage(), controllerTypes.stream().filter(zhDescPredicate).count());
        summaryDTO.setControllerI18nCount(controllerI18nCount);

        // serviceFacade.getReportingTaskTypes(null, null, null);

        final Predicate<? super DocumentedTypeDTO> ownedPredicate = dto -> Marks.ORCHSYM.equals(dto.getVendor());

        final long processorMarkedCount = processorTypes.stream().filter(ownedPredicate).count();
        summaryDTO.setProcessorOwnedCount(processorMarkedCount);

        final long controllerMarkedCount = controllerTypes.stream().filter(ownedPredicate).count();
        summaryDTO.setControllerOwnedCount(controllerMarkedCount);

        return summaryDTO;
    }

    private long getGroupCount(String parentGroupId, boolean all) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = 0;
        // have components, else ignore
        if (all || processGroup != null && processGroup.getProcessors() != null && !processGroup.getProcessors().isEmpty()) {
            count = processGroup.getProcessGroups().size();
        }
        for (ProcessGroup group : processGroup.getProcessGroups()) {
            count += getGroupCount(group.getIdentifier(), all);
        }
        return count;
    }

    private long getLabelCount(String parentGroupId) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = 0L;
        if (processGroup != null && processGroup.getLabels() != null) {
            count = processGroup.getLabels().size();
        }
        for (ProcessGroup group : processGroup.getProcessGroups()) {
            count += getLabelCount(group.getIdentifier());
        }
        return count;
    }

    private long getConnectionCount(String parentGroupId) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = 0L;
        if (processGroup != null && processGroup.getConnections() != null) {
            count = processGroup.getConnections().size();
        }
        for (ProcessGroup group : processGroup.getProcessGroups()) {
            count += getConnectionCount(group.getIdentifier());
        }
        return count;
    }

    private long getFunnelCount(String parentGroupId) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = 0L;
        if (processGroup != null && processGroup.getFunnels() != null) {
            count = processGroup.getFunnels().size();
        }
        for (ProcessGroup group : processGroup.getProcessGroups()) {
            count += getFunnelCount(group.getIdentifier());
        }
        return count;
    }

    private long getTemplateCount(String parentGroupId) {
        final Set<TemplateEntity> templates = serviceFacade.getTemplates();
        long count = templates.size();

        return count;
    }

    private List<VarCounterDTO> getVarCount(String parentGroupId, final boolean simple) {
        final VariableRegistryEntity var = serviceFacade.getVariableRegistry(parentGroupId, false);

        List<VarCounterDTO> allVars = new ArrayList<>();

        long size = 0L;
        if (var.getVariableRegistry() != null && var.getVariableRegistry().getVariables() != null) {
            size = (long) var.getVariableRegistry().getVariables().size();
        }
        if (size > 0) {
            VarCounterDTO dto = new VarCounterDTO();
            if (!simple)
                dto.setDetail(var);
            dto.setCount(size);
            allVars.add(dto);
        }

        final Set<ProcessGroupEntity> processGroups = serviceFacade.getProcessGroups(parentGroupId);
        for (ProcessGroupEntity group : processGroups) {
            allVars.addAll(getVarCount(group.getId(), simple));
        }
        return allVars;
    }

    private List<ControllerServiceCounterDTO> getServiceCounters(final boolean simple) {
        // get all the controller services
        final Set<ControllerServiceEntity> controllerServices = serviceFacade.getControllerServices(flowController.getRootGroupId(), false, true);

        // services error
        List<ControllerServiceEntity> nullProcessorsList = controllerServices.stream() //
                .filter(p -> p.getComponent() == null || p.getComponent().getType() == null).collect(Collectors.toList());
        if (!nullProcessorsList.isEmpty()) { // exited null components?
            logger.warn("All services: " + controllerServices.size());
            logger.warn("NULL services: " + nullProcessorsList.size());

            List<String> okComponents = controllerServices.stream().filter(p -> p.getComponent() != null && p.getComponent().getType() != null).map(p -> p.getComponent().getType())
                    .map(t -> t.substring(t.lastIndexOf('.') + 1)).collect(Collectors.toList());
            logger.info("OK services: " + String.join(",", okComponents));

            for (int i = 0; i < nullProcessorsList.size(); i++) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    logger.error(i + " NULL service: " + objectMapper.writeValueAsString(nullProcessorsList.get(i)));
                } catch (JsonProcessingException e1) {
                    logger.error(e1.getMessage());
                }
            }
        }

        List<ControllerServiceCounterDTO> serviceCounterList = new ArrayList<>();
        controllerServices.stream() //
                .filter(p -> p.getComponent() != null && p.getComponent().getType() != null)//
                .collect(Collectors.groupingBy(e -> e.getComponent().getType(), Collectors.counting())).entrySet() //
                .forEach(entry -> {
                    ControllerServiceCounterDTO controllerServiceCounterDTO = new ControllerServiceCounterDTO();
                    String serviceName = removePackage(entry.getKey());
                    controllerServiceCounterDTO.setService(serviceName);
                    controllerServiceCounterDTO.setCount(entry.getValue());

                    List<ControllerServiceDTO> details = controllerServices.stream() //
                            .filter(e -> e.getComponent().getType().equals(entry.getKey())) //
                            .map(e -> e.getComponent())//
                            .collect(Collectors.toList());

                    long basePropertiesCount = 0;
                    if (!details.isEmpty() && details.get(0).getDescriptors() != null) {
                        basePropertiesCount = details.get(0).getDescriptors().size();
                    }
                    controllerServiceCounterDTO.setPropertiesCount(basePropertiesCount * details.size());

                    if (!simple) {
                        controllerServiceCounterDTO.setDetails(details.stream().sorted(new Comparator<ControllerServiceDTO>() {

                            @Override
                            public int compare(ControllerServiceDTO o1, ControllerServiceDTO o2) {
                                if (o1.getBundle() != null && o2.getBundle() != null) {
                                    int compare = o2.getBundle().getGroup().compareTo(o1.getBundle().getGroup());
                                    if (compare != 0) {
                                        return compare;
                                    }
                                    compare = o2.getBundle().getArtifact().compareTo(o1.getBundle().getArtifact());
                                    if (compare != 0) {
                                        return compare;
                                    }
                                    compare = o2.getBundle().getVersion().compareTo(o1.getBundle().getVersion());
                                    if (compare != 0) {
                                        return compare;
                                    }
                                }
                                return o2.getType().compareTo(o2.getType());
                            }
                        }).collect(Collectors.toList()));
                    }

                    serviceCounterList.add(controllerServiceCounterDTO);
                });
        return serviceCounterList;
    }

    private String removePackage(String fullname) {
        String name = fullname;
        final int packageIndex = name.lastIndexOf('.');
        if (packageIndex > 0) {
            name = name.substring(packageIndex + 1);
        }
        return name;
    }

    private List<ProcessorCounterDTO> getProcessorCounters(final boolean simple) {
        final Set<ProcessorEntity> processors = serviceFacade.getProcessors(flowController.getRootGroupId(), true);

        // process error
        List<ProcessorEntity> nullProcessorsList = processors.stream() //
                .filter(p -> p.getComponent() == null || p.getComponent().getType() == null).collect(Collectors.toList());
        if (!nullProcessorsList.isEmpty()) { // exited null components?
            logger.warn("All components: " + processors.size());
            logger.warn("NULL components: " + nullProcessorsList.size());

            List<String> okComponents = processors.stream().filter(p -> p.getComponent() != null && p.getComponent().getType() != null).map(p -> p.getComponent().getType())
                    .map(t -> t.substring(t.lastIndexOf('.') + 1)).collect(Collectors.toList());
            logger.info("OK components: " + String.join(",", okComponents));

            for (int i = 0; i < nullProcessorsList.size(); i++) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    logger.error(i + " NULL component: " + objectMapper.writeValueAsString(nullProcessorsList.get(i)));
                } catch (JsonProcessingException e1) {
                    logger.error(e1.getMessage());
                }
            }
        }

        List<ProcessorCounterDTO> processorCounterList = new ArrayList<>();
        processors.stream() //
                .filter(p -> p.getComponent() != null && p.getComponent().getType() != null)//
                .collect(Collectors.groupingBy(e -> e.getComponent().getType(), Collectors.counting())).entrySet() //
                .forEach(entry -> {
                    ProcessorCounterDTO processorCounterDTO = new ProcessorCounterDTO();
                    String compName = removePackage(entry.getKey());
                    processorCounterDTO.setName(compName);
                    processorCounterDTO.setCount(entry.getValue());

                    List<ProcessorDTO> details = processors.stream() //
                            .filter(e -> e.getComponent().getType().equals(entry.getKey())) //
                            .map(e -> e.getComponent())//
                            .collect(Collectors.toList());

                    long basePropertiesCount = 0;
                    if (!details.isEmpty()) {
                        final ProcessorConfigDTO config = details.get(0).getConfig();
                        if (config != null && config.getDescriptors() != null) {
                            basePropertiesCount = config.getDescriptors().size();
                        }
                    }
                    processorCounterDTO.setPropertiesCount(basePropertiesCount * details.size());

                    if (!simple) {
                        processorCounterDTO.setDetails(details.stream().sorted(new Comparator<ProcessorDTO>() {

                            @Override
                            public int compare(ProcessorDTO o1, ProcessorDTO o2) {
                                if (o1.getBundle() != null && o2.getBundle() != null) {
                                    int compare = o2.getBundle().getGroup().compareTo(o1.getBundle().getGroup());
                                    if (compare != 0) {
                                        return compare;
                                    }
                                    compare = o2.getBundle().getArtifact().compareTo(o1.getBundle().getArtifact());
                                    if (compare != 0) {
                                        return compare;
                                    }
                                    compare = o2.getBundle().getVersion().compareTo(o1.getBundle().getVersion());
                                    if (compare != 0) {
                                        return compare;
                                    }
                                }
                                return o2.getType().compareTo(o1.getType());
                            }
                        }).collect(Collectors.toList()));
                    }

                    processorCounterList.add(processorCounterDTO);
                });

        return processorCounterList;
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/delete/{id}")
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response verifyDelete(@Context final HttpServletRequest httpServletRequest,
            @ApiParam(value = "The id of processor, or group(remote), or snippet.", required = true) @PathParam("id") final String id) {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        Response response = null;

        // label
        response = findAndVerifyId(() -> serviceFacade.getLabel(id), null); // if found, just popup warning dialog
        if (response != null)
            return response;

        // processor
        response = findAndVerifyId(() -> serviceFacade.getProcessor(id), () -> serviceFacade.verifyDeleteProcessor(id));
        if (response != null)
            return response;

        // group
        response = findAndVerifyId(() -> serviceFacade.getProcessGroup(id), () -> serviceFacade.verifyDeleteProcessGroup(id));
        if (response != null)
            return response;

        // remote group
        response = findAndVerifyId(() -> serviceFacade.getRemoteProcessGroup(id), () -> serviceFacade.verifyDeleteRemoteProcessGroup(id));
        if (response != null)
            return response;

        // snippet
        response = findAndVerifyId(() -> serviceFacade.getRevisionsFromSnippet(id), () -> {
            final Set<Revision> requestRevisions = serviceFacade.getRevisionsFromSnippet(id);
            serviceFacade.verifyDeleteSnippet(id, requestRevisions.stream().map(rev -> rev.getComponentId()).collect(Collectors.toSet()));
        });
        if (response != null)
            return response;

        return noCache(Response.status(Status.FORBIDDEN)).build(); // no thing to do for other
    }

    private Response findAndVerifyId(final Runnable finder, final Runnable verifier) {
        try {
            if (finder != null)
                finder.run();
        } catch (ResourceNotFoundException e) {
            // not found
            return null;
        }

        // if found
        try {
            if (verifier != null)
                verifier.run();
            return generateOkResponse().build(); // will popup the warning dialog to confirm the deleting operation
        } catch (Throwable t) {
            // if can't delete, will have exception and don't popup the warning dialog
            return noCache(Response.status(Status.FORBIDDEN)).build();
        }

    }

}
