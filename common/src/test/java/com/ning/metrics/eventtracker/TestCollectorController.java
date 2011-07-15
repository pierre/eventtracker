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
import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.event.StubEvent;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.MockEventWriter;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

@Guice(modules = MockCollectorControllerModule.class)
public class TestCollectorController
{
    private final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector");
    private Event event;

    @Inject
    private CollectorController controller;

    @Inject
    private DiskSpoolEventWriter diskWriter;

    @BeforeTest
    public void setUp() throws IOException
    {
        System.setProperty("eventtracker.type", "NO_LOGGING");
        System.setProperty("eventtracker.diskspool.path", tmpDir.getAbsolutePath());

        event = new StubEvent();
    }

    @AfterTest
    public void cleanupTmpDir()
    {
        tmpDir.delete();
    }

    @Test(groups = "fast")
    public void testOfferEvent() throws Exception
    {
        controller.offerEvent(event);
        Assert.assertEquals(controller.getEventsReceived().get(), 1);
        diskWriter.commit();
        Assert.assertEquals(controller.getEventsLost().get(), 0);
    }

    @Test(groups = "fast")
    public void testWriterThrowsException() throws Exception
    {
        final MockEventWriter diskWriter = new MockEventWriter();
        diskWriter.setWriteThrowsException(true);
        final CollectorController controller = new CollectorController(diskWriter);

        try {
            controller.offerEvent(event);
            Assert.fail("Should have thrown an IOException");
        }
        catch (IOException e) {
            Assert.assertEquals(diskWriter.getWrittenEventList().size(), 0);
            Assert.assertEquals(controller.getEventsLost().get(), 1);
        }
    }


    @Test(groups = "fast")
    public void testWriterPassThru() throws Exception
    {
        final MockEventWriter diskWriter = new MockEventWriter();
        final CollectorController controller = new CollectorController(diskWriter);

        Assert.assertEquals(diskWriter.getWrittenEventList().size(), 0);
        Assert.assertEquals(diskWriter.getCommittedEventList().size(), 0);

        controller.offerEvent(event);
        Assert.assertEquals(diskWriter.getWrittenEventList().size(), 1);
        Assert.assertEquals(diskWriter.getCommittedEventList().size(), 0);

        controller.commit();
        Assert.assertEquals(diskWriter.getWrittenEventList().size(), 0);
        Assert.assertEquals(diskWriter.getCommittedEventList().size(), 1);

        controller.flush();
        Assert.assertEquals(diskWriter.getWrittenEventList().size(), 0);
        Assert.assertEquals(diskWriter.getCommittedEventList().size(), 0);
    }
}
