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
package org.apache.nifi.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.groups.ProcessAdditions;
import org.apache.nifi.groups.ProcessTags;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class ProcessUtil {

    public static Set<String> getTags(final Element processGroupElement) {
        final Set<String> tags = new HashSet<>();
        final List<Element> tagElements = DomUtils.getChildElementsByTagName(processGroupElement, ProcessTags.TAG_NAME);
        for (final Element tagElement : tagElements) {
            final String value = tagElement.getTextContent();
            if (StringUtils.isNotBlank(value)) {
                tags.add(value);
            }
        }
        return tags;
    }

    public static Map<String, String> getAdditions(final Element processGroupElement) {
        final Map<String, String> additions = new HashMap<>();
        final List<Element> additionsElements = DomUtils.getChildElementsByTagName(processGroupElement, ProcessAdditions.ADDITIONS_NAME);
        for (final Element additionsElement : additionsElements) {
            final NodeList childNodes = additionsElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node node = childNodes.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }

                final Element child = (Element) childNodes.item(i);

                final String additionName = child.getTagName();
                // child.getNodeName();
                final String additionValue = child.getTextContent();
                if (additionName == null || additionValue == null) {
                    continue;
                }

                additions.put(additionName, additionValue);
            }
        }
        return additions;
    }
}
