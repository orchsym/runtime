package com.hashmapinc.tempus.processors;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.Identifiers;

@Tags({"OPC", "OPCUA", "UA"})
@CapabilityDescription("Retrieves the namespace from an OPC UA server")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GetOPCNodeList extends AbstractProcessor {

    private String node_filter = null;
    private String print_indentation = "No";
    private String remove_opc_string = "No";
    private Integer max_recursiveDepth;
    private Integer max_reference_per_node;

    public static final PropertyDescriptor OPCUA_SERVICE = new PropertyDescriptor.Builder()
	        .name("OPC UA Service")
	        .description("Specifies the OPC UA Service that can be used to access data")
	        .required(true)
	        .identifiesControllerService(OPCUAService.class)
	        .build();

    public static final PropertyDescriptor NODE_FILTER = new PropertyDescriptor
	        .Builder().name("Node Filter")
	        .description("Provide regular expression for nodes you want to fetch node-list. Default it will fetch node-list for all nodes starting from root node. Separate multiple regex with a pipe(|)")
	        .addValidator(StandardValidators.REGULAR_EXPRESSION_VALIDATOR)
	        .build();

    public static final PropertyDescriptor RECURSIVE_DEPTH = new PropertyDescriptor
	        .Builder().name("Recursive Depth")
	        .description("Maximum depth from the starting node to read, Default is 3")
	        .required(true)
	        .defaultValue("3")
	        .addValidator(StandardValidators.INTEGER_VALIDATOR)
	        .build();

    public static final PropertyDescriptor PRINT_INDENTATION = new PropertyDescriptor
	        .Builder().name("Print Indentation")
	        .description("Should Nifi add indentation to the output text")
	        .required(true)
	        .allowableValues("No", "Yes")
	        .defaultValue("No")
	        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
	        .build();

    public static final PropertyDescriptor REMOVE_OPC_STRING = new PropertyDescriptor
	        .Builder().name("Remove Expanded Node ID")
	        .description("Should remove Expanded Node ID string from the list")
	        .required(true)
	        .allowableValues("No", "Yes")
	        .defaultValue("No")
	        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
	        .build();

    public static final PropertyDescriptor MAX_REFERENCE_PER_NODE = new PropertyDescriptor
	        .Builder().name("Max References Per Node")
	        .description("The number of Reference Descriptions to pull per node query.")
	        .required(true)
	        .defaultValue("1000")
	        .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
	        .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
	        .name("Success")
	        .description("Successful OPC read")
	        .build();

    public static final Relationship FAILURE = new Relationship.Builder()
	        .name("Failure")
	        .description("Failed OPC read")
	        .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(OPCUA_SERVICE);
        descriptors.add(RECURSIVE_DEPTH);
        descriptors.add(NODE_FILTER);
        descriptors.add(PRINT_INDENTATION);
        descriptors.add(REMOVE_OPC_STRING);
        descriptors.add(MAX_REFERENCE_PER_NODE);

        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

        print_indentation = context.getProperty(PRINT_INDENTATION).getValue();
        max_recursiveDepth = Integer.valueOf(context.getProperty(RECURSIVE_DEPTH).getValue());
        node_filter = context.getProperty(NODE_FILTER).getValue();
        remove_opc_string = context.getProperty(REMOVE_OPC_STRING).getValue();
        max_reference_per_node = Integer.valueOf(context.getProperty(MAX_REFERENCE_PER_NODE).getValue());
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

        final ComponentLog logger = getLogger();
        StringBuilder stringBuilder = new StringBuilder();

        try {

            // Submit to getValue
            final OPCUAService opcUAService = context.getProperty(OPCUA_SERVICE).asControllerService(OPCUAService.class);

            if (opcUAService.updateSession()) {
                logger.debug("Session current");
            } else {
                logger.error("Session update failed");
                return;
            }

            Pattern pattern;
            if (node_filter == null) {
                pattern = Pattern.compile("[^\\.].*");
            } else {
                pattern = Pattern.compile(node_filter);
            }

            String nameSpace = opcUAService.getNameSpace(print_indentation, max_recursiveDepth, pattern, new UnsignedInteger(max_reference_per_node));
            if (StringUtils.isNotBlank(nameSpace)) {
                stringBuilder.append(nameSpace);

                // Write the results back out to a flow file
                FlowFile flowFile = session.create();

                if (flowFile != null) {
                    try {
                        flowFile = session.putAttribute(flowFile, "recursiveDepth", Integer.toString(max_recursiveDepth));
                        flowFile = session.putAttribute(flowFile, "nodeFilter", node_filter);
                        flowFile = session.write(flowFile, new OutputStreamCallback() {
                            public void process(OutputStream out) throws IOException {

                                switch (remove_opc_string) {

                                case "Yes": {
                                    String str = stringBuilder.toString();
                                    String parts[] = str.split("\\r?\\n");
                                    String outString = "";
                                    for (int i = 0; i < parts.length; i++) {
                                        if (parts[i].startsWith("nsu")) {
                                            continue;
                                        }
                                        outString = outString + parts[i] + System.getProperty("line.separator");
                                    }
                                    outString.trim();
                                    out.write(outString.getBytes());
                                    break;
                                }
                                case "No": {
                                    out.write(stringBuilder.toString().getBytes());
                                    break;
                                }

                                }
                            }
                        });
                        // Transfer data to flow file
                        session.transfer(flowFile, SUCCESS);
                    } catch (ProcessException ex) {
                        logger.error("Unable to process", ex);
                        session.transfer(flowFile, FAILURE);
                    }
                } else {

                    logger.error("Flowfile is null");
                    session.transfer(flowFile, FAILURE);
                }
            } else {
                logger.error("No namespace found: opcUAService.getNameSpace(...) returned empty or null");
            }
        } catch (Exception e) {
            logger.error("Error in GetOPCNodeList.onTrigger() method --> " + e.getMessage());
        }

    }
	
}