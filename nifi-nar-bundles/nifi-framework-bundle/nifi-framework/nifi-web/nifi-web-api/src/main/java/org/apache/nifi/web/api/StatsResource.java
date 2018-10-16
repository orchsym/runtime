package org.apache.nifi.web.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.authorization.Authorizer;
import org.apache.nifi.authorization.RequestAction;
import org.apache.nifi.authorization.resource.Authorizable;
import org.apache.nifi.authorization.user.NiFiUserUtils;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.groups.ProcessGroup;
import org.apache.nifi.web.NiFiServiceFacade;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * RESTful endpoint for statistics the flows and components.
 * 
 * @author GU Guoqiang
 */
@Path(StatsResource.PATH)
@Api(value = StatsResource.PATH, description = "Endpoint for accessing the statistics of flows and components.")
public class StatsResource extends ApplicationResource {
    public static final String PATH = "/stats";

    public static final String CODE_MESSAGE_400 = "NiFi was unable to complete the request because it was invalid. The request should not be retried without modification.";
    public static final String CODE_MESSAGE_401 = "Client could not be authenticated.";
    public static final String CODE_MESSAGE_403 = "Client is not authorized to make this request.";
    public static final String CODE_MESSAGE_409 = "The request was valid but NiFi was not in the appropriate state to process it. Retrying the same request later may be successful.";

    public static final String AUTH_READ = "Read - " + PATH;

    private NiFiServiceFacade serviceFacade;
    private Authorizer authorizer;
    private FlowController flowController;

    public StatsResource() {
        super();
    }

    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public void setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    public void setFlowController(FlowController flowController) {
        this.flowController = flowController;
    }

    /**
     * Authorizes access to the flow.
     */
    private void authorizeFlow() {
        serviceFacade.authorizeAccess(lookup -> {
            final Authorizable flow = lookup.getFlow();
            flow.authorize(authorizer, RequestAction.READ, NiFiUserUtils.getNiFiUser());
        });
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
            response = StatsCountersEntity.class, //
            authorizations = { @Authorization(value = AUTH_READ) })
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getCounters() {

        authorizeFlow();

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

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
            response = StatsProcessorsEntity.class, //
            authorizations = { @Authorization(value = AUTH_READ) })
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getProcessors() {
        authorizeFlow();

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        final List<ProcessorCounterDTO> processorCounters = getProcessorCounters(false);
        StatsProcessorsEntity entity = new StatsProcessorsEntity();
        entity.setProcessors(processorCounters);
        // generate the response
        return generateOkResponse(entity).build();
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
            response = StatsServicesEntity.class, //
            authorizations = { @Authorization(value = AUTH_READ) })
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getServices() {
        authorizeFlow();

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        final List<ControllerServiceCounterDTO> serviceCounters = getServiceCounters(false);
        StatsServicesEntity entity = new StatsServicesEntity();
        entity.setServices(serviceCounters);
        // generate the response
        return generateOkResponse(entity).build();
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
            response = StatsVarsEntity.class, //
            authorizations = { @Authorization(value = AUTH_READ) })
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getVars() {
        authorizeFlow();

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        final List<VarCounterDTO> varCounter = getVarCount(flowController.getRootGroupId(), false);
        StatsVarsEntity entity = new StatsVarsEntity();
        entity.setVars(varCounter);

        // generate the response
        return generateOkResponse(entity).build();
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

        final Set<DocumentedTypeDTO> processorTypes = serviceFacade.getProcessorTypes(null, null, null);
        summaryDTO.setProcessorCount((long) processorTypes.size());

        final Set<DocumentedTypeDTO> serviceTypes = serviceFacade.getControllerServiceTypes(null, null, null, null, null, null, null);
        summaryDTO.setControllerCount((long) serviceTypes.size());

        final long processorMarkedCount = processorTypes.stream().filter(dto -> Marks.ORCHSYM.equals(dto.getVendor())).count();
        summaryDTO.setProcessorOwnedCount(processorMarkedCount);
        
        final long controllerMarkedCount = serviceTypes.stream().filter(dto -> Marks.ORCHSYM.equals(dto.getVendor())).count();
        summaryDTO.setControllerOwnedCount(controllerMarkedCount);

        return summaryDTO;
    }

    private long getGroupCount(String parentGroupId, boolean all) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = 0;
        if (all || !processGroup.getProcessors().isEmpty()) { // have components, else ignore
            count = processGroup.getProcessGroups().size();
        }
        for (ProcessGroup group : processGroup.getProcessGroups()) {
            count += getGroupCount(group.getIdentifier(), all);
        }
        return count;
    }

    private long getLabelCount(String parentGroupId) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = processGroup.getLabels().size();

        for (ProcessGroup group : processGroup.getProcessGroups()) {
            count += getLabelCount(group.getIdentifier());
        }
        return count;
    }

    private long getConnectionCount(String parentGroupId) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = processGroup.getConnections().size();

        for (ProcessGroup group : processGroup.getProcessGroups()) {
            count += getConnectionCount(group.getIdentifier());
        }
        return count;
    }

    private long getFunnelCount(String parentGroupId) {
        final ProcessGroup processGroup = flowController.getGroup(parentGroupId);
        long count = processGroup.getFunnels().size();

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

        final long size = (long) var.getVariableRegistry().getVariables().size();
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

        List<ControllerServiceCounterDTO> serviceCounterList = new ArrayList<>();
        controllerServices.stream() //
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

        List<ProcessorCounterDTO> processorCounterList = new ArrayList<>();
        processors.stream() //
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
}
