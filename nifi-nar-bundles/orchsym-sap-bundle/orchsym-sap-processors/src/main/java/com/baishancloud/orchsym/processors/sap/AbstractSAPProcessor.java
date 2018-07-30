package com.baishancloud.orchsym.processors.sap;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.avro.generic.GenericData;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;

import com.baishancloud.orchsym.processors.sap.i18n.Messages;
import com.baishancloud.orchsym.processors.sap.option.BoolOption;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbstractSAPProcessor extends AbstractProcessor {

    static final String APPLICATION_JSON = "application/json"; //$NON-NLS-1$
    static final byte[] EMPTY_JSON_OBJECT = "{}".getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$

    static final PropertyDescriptor JSON_IGNORE_EMPTY_VALUES = new PropertyDescriptor.Builder()//
            .name("json-ignore-empty-values")//$NON-NLS-1$
            .displayName(Messages.getString("SAPProcessor.IgnoreEmptyValues"))//$NON-NLS-1$
            .description(Messages.getString("SAPProcessor.IgnoreEmptyValues_Desc"))//$NON-NLS-1$
            .required(false)//
            .defaultValue(BoolOption.NO.getValue())//
            .allowableValues(BoolOption.getAll())//
            .build();
    
    static final Relationship REL_SUCCESS = new Relationship.Builder().name("Success")//$NON-NLS-1$
            .description("A Response FlowFile will be routed upon success.")//
            .build();

    static final Relationship REL_FAILURE = new Relationship.Builder().name("Failure")//$NON-NLS-1$
            .description("The original FlowFile will be routed on any type of connection failure, timeout or general exception. "//
                    + "It will have new attributes detailing the request.")//
            .build();

    protected List<PropertyDescriptor> descriptors;
    protected Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        this.descriptors = Collections.unmodifiableList(Collections.emptyList());

        final Set<Relationship> relationships = new HashSet<>(2);
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);

    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        // do something
    }

    @OnStopped
    public void onStopped(final ProcessContext context) {
        // do something
    }

     static void writeStr(OutputStream out, String result) throws IOException {
        final byte[] outputBytes = (result == null) ? EMPTY_JSON_OBJECT : result.getBytes(StandardCharsets.UTF_8);
        out.write(outputBytes);
    }

    static void write(OutputStream out, Object result) throws IOException {
        final byte[] outputBytes = (result == null) ? EMPTY_JSON_OBJECT : GenericData.get().toString(result).getBytes(StandardCharsets.UTF_8);
        out.write(outputBytes);
    }
}
