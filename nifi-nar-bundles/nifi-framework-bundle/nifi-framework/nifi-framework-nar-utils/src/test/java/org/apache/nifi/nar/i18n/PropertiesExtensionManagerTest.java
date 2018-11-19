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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
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
        final File file = new File(workingFolder, TYPE + '_' + locale + MessagesProvider.EXT);
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
        TypeLocale tl = PropertiesExtensionManager.createTypeLocale(file.toURI().toURL());
        assertNull(tl);

    }

    @Test
    public void testCreateTypeLocale_onlyLang() throws IOException {
        final File file = createTestFile(workingFolder, "en");
        TypeLocale tl = PropertiesExtensionManager.createTypeLocale(file.toURI().toURL());
        assertNotNull(tl);
        assertEquals(TYPE, tl.type);
        assertEquals(new Locale("en"), tl.locale);
    }

    @Test
    public void testCreateTypeLocale_withCountry() throws IOException {
        File file = createTestFile(workingFolder, "en_US");
        TypeLocale tl = PropertiesExtensionManager.createTypeLocale(file.toURI().toURL());
        assertNotNull(tl);
        assertEquals(TYPE, tl.type);
        assertEquals("en", tl.locale.getLanguage());
        assertEquals("US", tl.locale.getCountry());
        assertEquals(new Locale("en", "US"), tl.locale);
    }

    @Test
    public void testCreateTypeLocale_unknownLang() throws IOException {
        File file = createTestFile(workingFolder, "NIFI");
        TypeLocale tl = PropertiesExtensionManager.createTypeLocale(file.toURI().toURL());
        assertNotNull(tl);
        assertEquals(TYPE, tl.type);
        assertEquals("nifi", tl.locale.getLanguage());
        assertEquals(new Locale("NIFI"), tl.locale);
    }

    @Test
    public void testCreateTypeLocale_unknownCountry() throws IOException {
        File file = createTestFile(workingFolder, "en_NIFI");
        TypeLocale tl = PropertiesExtensionManager.createTypeLocale(file.toURI().toURL());
        assertNotNull(tl);
        assertEquals(TYPE, tl.type);
        assertEquals("en", tl.locale.getLanguage());
        assertEquals("NIFI", tl.locale.getCountry());
        assertEquals(new Locale("en", "NIFI"), tl.locale);
    }
}
