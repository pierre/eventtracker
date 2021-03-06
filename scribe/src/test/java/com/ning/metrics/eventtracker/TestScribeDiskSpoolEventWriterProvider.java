/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.eventtracker;

import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.thrift.ThriftEnvelope;
import com.ning.metrics.serialization.thrift.ThriftField;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.ObjectOutputEventSerializer;
import org.joda.time.DateTime;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class TestScribeDiskSpoolEventWriterProvider
{
    private static final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector-" + System.currentTimeMillis());
    private static final String EVENT_NAME = "myEnvelope";
    private static final DateTime EVENT_DATE_TIME = new DateTime();

    private EventTrackerConfig config;

    @BeforeTest(alwaysRun = true)
    public void setUp() throws Exception
    {
        System.setProperty("eventtracker.diskspool.path", tmpDir.getAbsolutePath());
        config = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
    }

    @AfterTest
    public void tearDown() throws Exception
    {
        tmpDir.delete();
    }

    @Test(groups = "fast")
    public void testThriftEnvelopeEvent() throws Exception
    {
        System.setProperty("eventtracker.event-type", "THRIFT");
        config = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);

        // Create a Thrift event
        final List<ThriftField> thriftFieldList = new ArrayList<ThriftField>();
        thriftFieldList.add(ThriftField.createThriftField("hello", (short) 1));
        thriftFieldList.add(ThriftField.createThriftField("world", (short) 12));
        final ThriftEnvelope envelope = new ThriftEnvelope(EVENT_NAME, thriftFieldList);
        final ThriftEnvelopeEvent event = new ThriftEnvelopeEvent(EVENT_DATE_TIME, envelope);

        final int numberOfThriftEventsToSend = 3;

        // Create the DiskSpoolEventWriter
        final AtomicInteger sendCalls = new AtomicInteger(0);
        final DiskSpoolEventWriter diskSpoolEventWriter = diskWriterProvider(new EventSender()
        {
            @Override
            public void send(final File file, final CallbackHandler handler)
            {
                sendCalls.incrementAndGet();
            }

            @Override
            public void close()
            {
            }
        });

        // Flush 3 events
        for (int i = 0; i < numberOfThriftEventsToSend; i++) {
            diskSpoolEventWriter.write(event);
        }
        diskSpoolEventWriter.commit();
        diskSpoolEventWriter.flush();

        Assert.assertEquals(sendCalls.get(), 1);

        // Flush another series
        for (int i = 0; i < numberOfThriftEventsToSend; i++) {
            diskSpoolEventWriter.write(event);
        }
        diskSpoolEventWriter.commit();
        diskSpoolEventWriter.flush();

        Assert.assertEquals(sendCalls.get(), 2);
    }

    /**
     * Create a DiskSpoolEventWriter with the specified EventSender
     *
     * @param sender EventSender to use
     * @return DiskSpoolEventWriter create via the DiskSpoolEventWriterProvider
     * @throws java.io.IOException generic serialization exception
     */
    private DiskSpoolEventWriter diskWriterProvider(EventSender sender) throws IOException
    {
        return new DiskSpoolEventWriterProvider(
            config,
            sender,
            new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory()),
            new ObjectOutputEventSerializer()
        ).get();
    }
}
