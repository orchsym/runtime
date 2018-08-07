package org.apache.nifi.processors.hl7;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

public class TestPutHL7 {

    private long port = Math.round(Math.random()*1000)+1000;

    @Test
    public void testPutHL7(){
        initListener();
        String sourceMsg = "MSH|^~\\&|NES|NINTENDO|TESTSYSTEM|TESTFACILITY|20010101000000||ADT^A04|Q123456789T123456789X123456|P|2.3\r" + 
                "EVN|A04|20010101000000|||^KOOPA^BOWSER^^^^^^^CURRENT\r" + 
                "PID|1||123456789|0123456789^AA^^JP|BROS^MARIO||19850101000000|M|||123 FAKE STREET^MARIO \\T\\ LUIGI BROS PLACE^TOADSTOOL KINGDOM^NES^A1B2C3^JP^HOME^^1234|1234|(555)555-0123^HOME^JP:1234567|||S|MSH|12345678|||||||0|||||N\r" + 
                "NK1|1|PEACH^PRINCESS|SO|ANOTHER CASTLE^^TOADSTOOL KINGDOM^NES^^JP|(123)555-1234|(123)555-2345|NOK\r" + 
                "NK1|2|TOADSTOOL^PRINCESS|SO|YET ANOTHER CASTLE^^TOADSTOOL KINGDOM^NES^^JP|(123)555-3456|(123)555-4567|EMC\r" + 
                "PV1|1|O|ABCD^EFGH||||123456^DINO^YOSHI^^^^^^MSRM^CURRENT^^^NEIGHBOURHOOD DR NBR|^DOG^DUCKHUNT^^^^^^^CURRENT||CRD|||||||123456^DINO^YOSHI^^^^^^MSRM^CURRENT^^^NEIGHBOURHOOD DR NBR|AO|0123456789|1|||||||||||||||||||MSH||A|||20010101000000\r" + 
                "IN1|1|PAR^PARENT||||LUIGI\r" + 
                "IN1|2|FRI^FRIEND||||PRINCESS";
        String targetMsg = "MSA|AA|Q123456789T123456789X123456";
        final TestRunner runner = TestRunners.newTestRunner(PutHL7.class);
        runner.setProperty(PutHL7.HOST, "127.0.0.1");
        runner.setProperty(PutHL7.PORT, String.valueOf(port));
        runner.setIncomingConnection(true);
        ProcessContext processContext = runner.getProcessContext();
        PutHL7 processor = (PutHL7) runner.getProcessor();
        processor.updateScheduledTrue();
        ProcessSession session = runner.getProcessSessionFactory().createSession();
        FlowFile flowFile1 = session.create();
        flowFile1 = session.importFrom(new ByteArrayInputStream(sourceMsg.getBytes()), flowFile1);
        runner.enqueue(flowFile1);
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(PutHL7.REL_SUCCESS);
        for (final MockFlowFile flowFile : flowFiles) {
            String res = new String(flowFile.toByteArray());
            org.junit.Assert.assertTrue(res.indexOf(targetMsg)>-1);
        }
    }

    private void initListener() {
        final TestRunner runner = TestRunners.newTestRunner(ListenHL7.class);
        runner.setProperty(ListenHL7.PORT, String.valueOf(port));
        runner.setIncomingConnection(true);
        ProcessContext processContext = runner.getProcessContext();
        ListenHL7 processor = (ListenHL7) runner.getProcessor();
        processor.updateScheduledTrue();
        processor.onTrigger(processContext, runner.getProcessSessionFactory());
    }
}
