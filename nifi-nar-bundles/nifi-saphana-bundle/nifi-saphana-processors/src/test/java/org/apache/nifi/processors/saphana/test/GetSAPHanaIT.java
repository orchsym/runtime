package org.apache.nifi.processors.saphana.test;

import java.util.List;

import junit.framework.Assert;

import org.apache.nifi.processors.saphana.GetSAPHana;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class GetSAPHanaIT extends ExecuteSAPHanaSQL{
    
    @Test
    public void testOnTriggerProcessContextProcessSession() throws InitializationException {
        final TestRunner runner = executeSQL("select * from baishanTable");
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(GetSAPHana.REL_SUCCESS);
        long rowCount = 0;
        for (final MockFlowFile flowFile : flowFiles) {
            rowCount = Long.parseLong(flowFile.getAttribute(GetSAPHana.RESULT_ROW_COUNT));
        }
        Assert.assertEquals(5, rowCount);
    }

}
