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

import com.ning.metrics.serialization.event.Granularity;
import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.writer.SyncType;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;

@Test(enabled = false)
public class TestHttpSmileIntegration
{
    private final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector");

    class SomeEvent
    {
        public final long eventDate;
        public final String foo;

        SomeEvent(final long eventDate, final String foo)
        {
            this.eventDate = eventDate;
            this.foo = foo;
        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    @AfterTest(alwaysRun = true)
    private void cleanupTmpDir()
    {
        tmpDir.delete();
    }

    @Test(groups = "slow")
    public void testHttpFactory() throws Exception
    {
        final CollectorController controller = HttpCollectorFactory.createHttpController(
            "127.0.0.1",
            8080,
            EventType.SMILE,
            8000,
            60000, // keep-alive
            tmpDir.getAbsolutePath(),
            true,
            10,
            SyncType.NONE,
            10,
            10,
            10,
            50
        );

        fireSmileEvents(controller);
    }

    private void fireSmileEvents(final CollectorController controller) throws Exception
    {
        final SomeEvent event = new SomeEvent(System.currentTimeMillis(), "bar");
        controller.offerEvent(SmileEnvelopeEvent.fromPOJO("TestEvent", Granularity.DAILY, event));
        Assert.assertEquals(controller.getEventsReceived().get(), 1);
        Assert.assertEquals(controller.getEventsLost().get(), 0);
        controller.commit();
        controller.flush();
        Thread.sleep(5000);
    }
}
