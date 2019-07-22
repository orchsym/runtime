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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.nifi.processor.Processor;

public class TestRunners {

    public static TestRunner newTestRunner(final Processor processor) {
        return new StandardProcessorTestRunner(processor);
    }

    public static TestRunner newTestRunner(final Class<? extends Processor> processorClass) {
        try {
            return newTestRunner(processorClass.newInstance());
        } catch (final Exception e) {
            System.err.println("Could not instantiate instance of class " + processorClass.getName() + " due to: " + e);
            throw new RuntimeException(e);
        }
    }

    protected static final String TEST_RES = "src/test/resources";

    public static String loadContents(String dataFileName) throws IOException {
        final File dataJsonFile = new File(TEST_RES, dataFileName);
        if (!dataJsonFile.exists()) {
            throw new FileNotFoundException(dataJsonFile.getAbsolutePath());
        }
        StringWriter sw = new StringWriter();
        char[] buffer = new char[2048];
        try (InputStreamReader input = new InputStreamReader(new FileInputStream(dataJsonFile), StandardCharsets.UTF_8)) {
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                sw.write(buffer, 0, n);
            }
        }
        return sw.toString();
    }
}
