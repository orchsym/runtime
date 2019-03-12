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
package org.apache.nifi.nar.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import org.apache.nifi.nar.i18n.PropertiesExtensionManager.TypeLocale;
import org.apache.nifi.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PropertiesExtensionManagerTest {
    private static final String TYPE = "org.apache.nifi.Test";
    File workingFolder;

    @Before
    public void prepare() throws IOException {
        workingFolder = File.createTempFile(PropertiesExtensionManagerTest.class.getSimpleName(), "junit");
        workingFolder.delete();
        workingFolder.mkdirs();

    }

    private static File createTestFile(File workingFolder, String locale) throws IOException {
        return createTestFile(workingFolder, null, locale);
    }

    private static File createTestFile(File workingFolder, String version, String locale) throws IOException {
        if (version == null) {
            version = "";
        }
        final File file = new File(workingFolder, TYPE + version + '_' + locale + MessagesProvider.EXT);
        file.createNewFile();
        return file;
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteFile(workingFolder, true);
    }

    @Test
    public void testCreateTypeLocale_invalid() throws IOException {
        doTestInvalid(TYPE);
        doTestInvalid(TYPE + ".txt");
        doTestInvalid(TYPE + MessagesProvider.EXT);

        doTestInvalid(TYPE + '_' + MessagesProvider.EXT);

    }

    private void doTestInvalid(String name) throws IOException {
        File file = new File(workingFolder, name);
        file.createNewFile();
        List<TypeLocale> tls = PropertiesExtensionManager.createTypeLocales(file.toURI().toURL());
        assertNotNull(tls);
        assertEquals(0, tls.size());

    }

    /**
     * 
     * like org.apache.nifi.processors.kafka.pubsub.ConsumeKafka_1_0_zh.properties, org.apache.nifi.processors.kafka.pubsub.ConsumeKafka_0_11_zh.properties
     */
    @Test
    public void testCreateTypeLocale_Versioning() throws IOException {
        File file = createTestFile(workingFolder, "_1_0", "zh");
        List<TypeLocale> tls = PropertiesExtensionManager.createTypeLocales(file.toURI().toURL());
        assertNotNull(tls);
        assertEquals(2, tls.size());

        assertEquals(TYPE + "_1_0", tls.get(0).type);
        assertEquals("zh", tls.get(0).locale.getLanguage());
        assertEquals("", tls.get(0).locale.getCountry());
        assertEquals(new Locale("zh"), tls.get(0).locale);

        assertEquals(TYPE + "_1", tls.get(1).type);
        assertEquals("0", tls.get(1).locale.getLanguage());
        assertEquals("ZH", tls.get(1).locale.getCountry());
        assertEquals(new Locale("0", "zh"), tls.get(1).locale);

        file = createTestFile(workingFolder, "_0_11", "en_US");
        tls = PropertiesExtensionManager.createTypeLocales(file.toURI().toURL());
        assertNotNull(tls);
        assertEquals(2, tls.size());

        assertEquals(TYPE + "_0_11_en", tls.get(0).type);
        assertEquals("us", tls.get(0).locale.getLanguage());
        assertEquals("", tls.get(0).locale.getCountry());
        assertEquals(new Locale("US"), tls.get(0).locale);

        assertEquals(TYPE + "_0_11", tls.get(1).type);
        assertEquals("en", tls.get(1).locale.getLanguage());
        assertEquals("US", tls.get(1).locale.getCountry());
        assertEquals(new Locale("en", "US"), tls.get(1).locale);

        file = createTestFile(workingFolder, "_1.0", "zh_CN");
        tls = PropertiesExtensionManager.createTypeLocales(file.toURI().toURL());
        assertNotNull(tls);
        assertEquals(2, tls.size());

        assertEquals(TYPE + "_1.0_zh", tls.get(0).type);
        assertEquals("cn", tls.get(0).locale.getLanguage());
        assertEquals("", tls.get(0).locale.getCountry());
        assertEquals(new Locale("CN"), tls.get(0).locale);

        assertEquals(TYPE + "_1.0", tls.get(1).type);
        assertEquals("zh", tls.get(1).locale.getLanguage());
        assertEquals("CN", tls.get(1).locale.getCountry());
        assertEquals(new Locale("zh", "CN"), tls.get(1).locale);

        file = createTestFile(workingFolder, "1.0", "zh_CN");
        tls = PropertiesExtensionManager.createTypeLocales(file.toURI().toURL());
        assertNotNull(tls);
        assertEquals(2, tls.size());

        assertEquals(TYPE + "1.0_zh", tls.get(0).type);
        assertEquals("cn", tls.get(0).locale.getLanguage());
        assertEquals("", tls.get(0).locale.getCountry());
        assertEquals(new Locale("CN"), tls.get(0).locale);

        assertEquals(TYPE + "1.0", tls.get(1).type);
        assertEquals("zh", tls.get(1).locale.getLanguage());
        assertEquals("CN", tls.get(1).locale.getCountry());
        assertEquals(new Locale("zh", "CN"), tls.get(1).locale);
    }

    @Test
    public void testCreateTypeLocale_onlyLang() throws IOException {
        final File file = createTestFile(workingFolder, "en");
        List<TypeLocale> tls = PropertiesExtensionManager.createTypeLocales(file.toURI().toURL());
        assertNotNull(tls);
        assertEquals(1, tls.size());
        assertEquals(TYPE, tls.get(0).type);
        assertEquals(new Locale("en"), tls.get(0).locale);
    }

    @Test
    public void testCreateTypeLocale_unknownLang() throws IOException {
        File file = createTestFile(workingFolder, "NIFI");
        List<TypeLocale> tls = PropertiesExtensionManager.createTypeLocales(file.toURI().toURL());
        assertNotNull(tls);
        assertEquals(1, tls.size());
        assertEquals(TYPE, tls.get(0).type);
        assertEquals(new Locale("NIFI"), tls.get(0).locale);
    }

    @Test
    public void testCreateTypeLocales_withCountry() throws IOException {
        File file = createTestFile(workingFolder, "en_US");
        doTestLangCountry(file.toURI().toURL(), "en", "US");
    }

    @Test
    public void testCreateTypeLocales_unknownCountry() throws IOException {
        File file = createTestFile(workingFolder, "en_NIFI");
        doTestLangCountry(file.toURI().toURL(), "en", "NIFI");
    }

    private void doTestLangCountry(final URL url, final String lang, final String country) {
        List<TypeLocale> tls = PropertiesExtensionManager.createTypeLocales(url);
        assertNotNull(tls);
        assertEquals(2, tls.size());

        assertEquals(TYPE + "_" + lang, tls.get(0).type);
        assertEquals(country.toLowerCase(), tls.get(0).locale.getLanguage());
        assertEquals("", tls.get(0).locale.getCountry());
        assertEquals(new Locale(country, ""), tls.get(0).locale);

        assertEquals(TYPE, tls.get(1).type);
        assertEquals(lang, tls.get(1).locale.getLanguage());
        assertEquals(country.toUpperCase(), tls.get(1).locale.getCountry());
        assertEquals(new Locale(lang, country.toUpperCase()), tls.get(1).locale);
    }
}
