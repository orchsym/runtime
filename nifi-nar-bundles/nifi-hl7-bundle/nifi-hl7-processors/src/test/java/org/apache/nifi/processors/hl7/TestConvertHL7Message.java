package org.apache.nifi.processors.hl7;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

public class TestConvertHL7Message {
    static{
        System.setProperty("line.separator", "\n");
    }
    String lineSeparator;
    
    private String getPIPE(){
        lineSeparator = "\r";
        String PIPE = "MSH|^~\\&|foo|foo||foo|200108151718||ACK^A01^ACK|1|D^P|2.4"+lineSeparator + 
                "MSA|AA"+lineSeparator;
        return PIPE;
    }
    private String getXML(){
        String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator + 
        "<ACK xmlns=\"urn:hl7-org:v2xml\">" + lineSeparator + 
        "    <MSH>" + lineSeparator + 
        "        <MSH.1>|</MSH.1>" + lineSeparator + 
        "        <MSH.2>^~\\&amp;</MSH.2>" + lineSeparator + 
        "        <MSH.3>" + lineSeparator + 
        "            <HD.1>foo</HD.1>" + lineSeparator + 
        "        </MSH.3>" + lineSeparator + 
        "        <MSH.4>" + lineSeparator + 
        "            <HD.1>foo</HD.1>" + lineSeparator + 
        "        </MSH.4>" + lineSeparator + 
        "        <MSH.6>" + lineSeparator + 
        "            <HD.1>foo</HD.1>" + lineSeparator + 
        "        </MSH.6>" + lineSeparator + 
        "        <MSH.7>" + lineSeparator + 
        "            <TS.1>200108151718</TS.1>" + lineSeparator + 
        "        </MSH.7>" + lineSeparator + 
        "        <MSH.9>" + lineSeparator + 
        "            <MSG.1>ACK</MSG.1>" + lineSeparator + 
        "            <MSG.2>A01</MSG.2>" + lineSeparator + 
        "            <MSG.3>ACK</MSG.3>" + lineSeparator + 
        "        </MSH.9>" + lineSeparator + 
        "        <MSH.10>1</MSH.10>" + lineSeparator + 
        "        <MSH.11>" + lineSeparator + 
        "            <PT.1>D</PT.1>" + lineSeparator + 
        "            <PT.2>P</PT.2>" + lineSeparator + 
        "        </MSH.11>" + lineSeparator + 
        "        <MSH.12>" + lineSeparator + 
        "            <VID.1>2.4</VID.1>" + lineSeparator + 
        "        </MSH.12>" + lineSeparator + 
        "    </MSH>" + lineSeparator + 
        "    <MSA>" + lineSeparator + 
        "        <MSA.1>AA</MSA.1>" + lineSeparator + 
        "    </MSA>" + lineSeparator + 
        "</ACK>";
        return XML;
    }

    @Test
    public void testPIPE2XML(){
        final TestRunner runner = TestRunners.newTestRunner(ConvertHL7Message.class);
        runner.setProperty(ConvertHL7Message.SOURCE_TYPE, String.valueOf("PIPE"));
        runner.setProperty(ConvertHL7Message.TARGET_TYPE, String.valueOf("XML"));
        runner.setIncomingConnection(true);
        ProcessContext processContext = runner.getProcessContext();
        ConvertHL7Message processor = (ConvertHL7Message) runner.getProcessor();
        processor.updateScheduledTrue();
        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile flowFile1 = session.create();
        flowFile1 = session.importFrom(new ByteArrayInputStream(getPIPE().getBytes()), flowFile1);
        runner.enqueue(flowFile1);
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(ConvertHL7Message.REL_SUCCESS);
        for (final MockFlowFile flowFile : flowFiles) {
            String res = new String(flowFile.toByteArray());
            lineSeparator = "\n";
            org.junit.Assert.assertTrue(getXML().equals(res.trim()));
        }
    }
    
    @Test
    public void testXML2PIPE() throws UnsupportedEncodingException{
        final TestRunner runner = TestRunners.newTestRunner(ConvertHL7Message.class);
        runner.setProperty(ConvertHL7Message.SOURCE_TYPE, String.valueOf("XML"));
        runner.setProperty(ConvertHL7Message.TARGET_TYPE, String.valueOf("PIPE"));
        runner.setIncomingConnection(true);
        ProcessContext processContext = runner.getProcessContext();
        ConvertHL7Message processor = (ConvertHL7Message) runner.getProcessor();
        processor.updateScheduledTrue();
        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile flowFile1 = session.create();
        flowFile1 = session.importFrom(new ByteArrayInputStream(getXML().getBytes()), flowFile1);
        runner.enqueue(flowFile1);
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(ConvertHL7Message.REL_SUCCESS);
        for (final MockFlowFile flowFile : flowFiles) {
            String res = new String(flowFile.toByteArray(),"UTF-8");
            lineSeparator = "\r";
            org.junit.Assert.assertTrue(getPIPE().equals(res));
        }
    }
}
