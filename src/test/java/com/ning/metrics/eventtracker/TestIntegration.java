/*
 * Copyright 2010 Ning, Inc.
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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ning.metrics.serialization.event.ThriftToThriftEnvelopeEvent;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

public class TestIntegration
{
    private final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector");


    @BeforeTest(alwaysRun = true)
    private void setupTmpDir()
    {
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new RuntimeException("Failed to create: " + tmpDir);
        }

        if (!tmpDir.isDirectory()) {
            throw new RuntimeException("Path points to something that's not a directory: " + tmpDir);
        }
    }

    @AfterTest(alwaysRun = true)
    private void cleanupTmpDir()
    {
        tmpDir.delete();
    }

    @Test(enabled = false)
    public void testGuice() throws Exception
    {
        System.setProperty("eventtracker.type", "SCRIBE");
        System.setProperty("eventtracker.directory", tmpDir.getAbsolutePath());
        System.setProperty("eventtracker.scribe.host", "127.0.0.1");
        System.setProperty("eventtracker.scribe.port", "7911");

        Injector injector = Guice.createInjector(new CollectorControllerModule());
        CollectorController controller = injector.getInstance(CollectorController.class);
        ScribeSender sender = (ScribeSender) injector.getInstance(EventSender.class);

        sender.createConnection();

        fireEvents(controller);

        sender.shutdown();
    }

    @Test(enabled = false)
    public void testScribeFactory() throws Exception
    {
        CollectorController controller = ScribeCollectorFactory.createScribeController(
            "127.0.0.1",
            7911,
            10000,
            tmpDir.getAbsolutePath(),
            true,
            1,
            "NONE",
            2,
            10,
            1
        );

        fireEvents(controller);
    }

    private void fireEvents(CollectorController controller) throws Exception
    {
        controller.offerEvent(ThriftToThriftEnvelopeEvent.extractEvent("thrift", new DateTime(), new Click(UUID.randomUUID().toString(), new DateTime().getMillis(), "user agent")));
        Assert.assertEquals(controller.getEventsReceived().get(), 1);
        Assert.assertEquals(controller.getEventsLost().get(), 0);
        controller.commit();
        controller.flush();
        Thread.sleep(5000);
    }
}
