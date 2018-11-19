package org.apache.nifi.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author GU Guoqiang
 *
 */
public class OrchsymPropertiesTest {

    private NiFiProperties loadProperties(final String propsPath, final Map<String, String> additionalProperties) {
        String realPath = null;
        try {
            realPath = OrchsymPropertiesTest.class.getResource(propsPath).toURI().getPath();
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        return NiFiProperties.createBasicNiFiProperties(realPath, additionalProperties);
    }

    @Test
    public void test_load() {
        NiFiProperties properties = loadProperties("/NiFiProperties/conf/nifi.properties", null);

        doTestBasic(properties);
    }

    @Test
    public void test_addition() {
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("nifi.key.junit.xx", "xxxxx");
        additionalProperties.put("orchsym.key.junit.yy", "yyyy");
        additionalProperties.put("mykey.abc", "xyz");

        NiFiProperties properties = loadProperties("/NiFiProperties/conf/nifi.properties", additionalProperties);

        doTestBasic(properties);

        assertEquals("xxxxx", properties.getProperty("nifi.key.junit.xx"));
        assertEquals("xxxxx", properties.getProperty("orchsym.key.junit.xx"));

        assertEquals("yyyy", properties.getProperty("nifi.key.junit.yy"));
        assertEquals("yyyy", properties.getProperty("orchsym.key.junit.yy"));

        assertEquals("xyz", properties.getProperty("mykey.abc"));

    }

    private void doTestBasic(NiFiProperties properties) {
        assertEquals("./target/flow.xml.gz", properties.getProperty("nifi.flow.configuration.file"));
        assertEquals("./target/flow.xml.gz", properties.getProperty("orchsym.flow.configuration.file"));

        assertEquals("8080", properties.getProperty("nifi.web.http.port"));
        assertEquals("8080", properties.getProperty("orchsym.web.http.port"));
    }
}
