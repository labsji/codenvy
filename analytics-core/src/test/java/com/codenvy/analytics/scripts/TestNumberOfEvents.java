/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.scripts;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.metrics.MetricParameter;
import com.codenvy.analytics.scripts.executor.pig.PigExecutor;
import com.codenvy.analytics.scripts.util.Event;
import com.codenvy.analytics.scripts.util.LogGenerator;

import org.apache.pig.data.Tuple;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.testng.Assert.*;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestNumberOfEvents extends BaseTest {

    private Map<String, String> params = new HashMap<>();

    @BeforeClass
    public void setUp() throws IOException {
        List<Event> events = new ArrayList<>();
        events.add(Event.Builder.createTenantCreatedEvent("ws1", "user1").withDate("2013-01-01").build());
        events.add(Event.Builder.createTenantCreatedEvent("ws2", "user1").withDate("2013-01-01").build());

        File log = LogGenerator.generateLog(events);

        MetricParameter.FROM_DATE.put(params, "20130101");
        MetricParameter.TO_DATE.put(params, "20130101");
        MetricParameter.USER.put(params, MetricParameter.USER_TYPES.REGISTERED.name());
        MetricParameter.WS.put(params, MetricParameter.WS_TYPES.PERSISTENT.name());
        MetricParameter.EVENT.put(params, EventType.TENANT_CREATED.toString());
        MetricParameter.CASSANDRA_STORAGE.put(params, "fake");
        MetricParameter.CASSANDRA_COLUMN_FAMILY.put(params, "fake");
        MetricParameter.LOG.put(params, log.getAbsolutePath());
    }

    @Test
    public void testExecute() throws Exception {
        Iterator<Tuple> iterator = PigExecutor.executeAndReturn(ScriptType.NUMBER_OF_EVENTS, params);

        assertTrue(iterator.hasNext());
        Tuple tuple = iterator.next();
        assertEquals(tuple.get(1).toString(), "(date,20130101)");
        assertEquals(tuple.get(2).toString(), "(value,2)");

        assertFalse(iterator.hasNext());
    }
}
