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

import com.google.inject.Inject;
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.event.ThriftToThriftEnvelopeEvent;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

@Test(groups = {"fast"})
@Guice(modules = MockCollectorControllerModule.class)
public class TestCollectorControllerProvider
{
    @Inject
    private CollectorController controller;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private DiskSpoolEventWriter spoolWriter;

    @Inject
    private EventSender eventSender;

    @Test
    public void testGet() throws Exception
    {
        Assert.assertTrue(controller.isAcceptEvents());

        final ThriftEnvelopeEvent event = ThriftToThriftEnvelopeEvent.extractEvent("thrift", new DateTime(), new Click(UUID.randomUUID().toString(), new DateTime().getMillis(), "user agent"));
        controller.offerEvent(event);
        // Too fast, we won't see anything. The shutdown hook will trigger a flush though
        Assert.assertEquals(((MockCollectorSender) eventSender).getSuccessCount(), 0);

        CollectorControllerProvider.mainEventTrackerShutdownHook(executor, spoolWriter, eventSender, controller);

        Assert.assertFalse(controller.isAcceptEvents());
        Assert.assertTrue(executor.isTerminated());

        Assert.assertEquals(((MockCollectorSender) eventSender).getSuccessCount(), 1);
        Assert.assertTrue(((MockCollectorSender) eventSender).isClosed());
    }
}
