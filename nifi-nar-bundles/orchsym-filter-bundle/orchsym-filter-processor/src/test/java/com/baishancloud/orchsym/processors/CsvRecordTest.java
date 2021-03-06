/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baishancloud.orchsym.processors;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CsvRecordTest {
    private TestRunner testRunner;
    private InputStream csvInput;
    private String csvSQL;
    @Before
    public void init() throws IOException {
        testRunner = TestRunners.newTestRunner(FilterRecord.class);
        csvInput = IOUtils.toInputStream(TestRunners.loadContents("/recordTest.csv"));
        csvSQL = TestRunners.loadContents("/SQL.txt");
    }
    @After
    public void clean(){
        testRunner=null;
        csvInput=null;
    }
    protected MockFlowFile runForSuccessFlowFile() {
        testRunner.enqueue(csvInput);
        testRunner.run();
        //断言REL_FILTER连线上有数据
        testRunner.assertTransferCount(FilterRecord.REL_FILTER, 1);
        final List<MockFlowFile> flowFiles = testRunner.getFlowFilesForRelationship(FilterRecord.REL_FILTER);
        final MockFlowFile successFlowFile = flowFiles.get(0);
        return successFlowFile;
    }
    @Test
    public void testJson_AND(){
        testRunner.setProperty(FilterRecord.CONTENT_TYPE, Constant.CONTENT_TYPE_TXT);
        testRunner.setProperty(FilterRecord.LOGICAL_TYPE, Constant.LOGICAL_TYPE_AND);
        testRunner.setProperty(FilterRecord.SETTINGS,csvSQL);

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("name,age,city,isChina\n" +
                "张三,22,GZ,true\n");

        testRunner.assertTransferCount(FilterRecord.REL_REJECT, 1);
    }
    @Test
    public void testJson_OR(){
        testRunner.setProperty(FilterRecord.CONTENT_TYPE, Constant.CONTENT_TYPE_TXT);
        testRunner.setProperty(FilterRecord.LOGICAL_TYPE, Constant.LOGICAL_TYPE_OR);
        testRunner.setProperty(FilterRecord.SETTINGS,csvSQL);

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("name,age,city,isChina\n" +
                "张三,22,GZ,true\n" +
                "李四,23,BJ,true\n" +
                "Linda,20,New York,false\n" +
                "Tom,22,London,false\n");

        testRunner.assertTransferCount(FilterRecord.REL_REJECT, 1);
    }
    @Test
    public void testJson_NOT(){
        testRunner.setProperty(FilterRecord.CONTENT_TYPE, Constant.CONTENT_TYPE_TXT);
        testRunner.setProperty(FilterRecord.LOGICAL_TYPE, Constant.LOGICAL_TYPE_NOT);
        testRunner.setProperty(FilterRecord.SETTINGS,csvSQL);

        MockFlowFile incompatible = runForSuccessFlowFile();
        incompatible.assertContentEquals("name,age,city,isChina\n" +
                "张三,22,GZ,true\n" +
                "李四,23,BJ,true\n");

        testRunner.assertTransferCount(FilterRecord.REL_REJECT, 1);
    }
}
