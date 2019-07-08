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
package com.orchsym.processor.attributes.xml;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;

/**
 * @author GU Guoqiang
 *
 */
public class ExtractXPathResolver implements XPathVariableResolver {
    private final ProcessContext context;
    private final FlowFile flowFile;

    public ExtractXPathResolver(ProcessContext context, FlowFile flowFile) {
        super();
        this.context = context;
        this.flowFile = flowFile;
    }

    @Override
    public Object resolveVariable(QName qName) {
        final String localPart = qName.getLocalPart();
        if (StringUtils.isNotBlank(localPart)) {
            final PropertyValue pv = context.getProperty(localPart);
            if (null != pv) {
                String value = pv.evaluateAttributeExpressions(flowFile).getValue();
                if (null == value) {
                    value = "";
                }
                return value;
            }
        }

        return null; // ?
    }

}
