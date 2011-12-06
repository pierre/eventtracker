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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ning.metrics.serialization.event.ThriftToThriftEnvelopeEvent;
import com.ning.metrics.serialization.writer.SyncType;
import org.joda.time.DateTime;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

@Test(enabled = false)
public class TestIntegration
{
    private final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector");

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

    @Test(groups = "slow", enabled = false)
    public void testGuiceThrift() throws Exception
    {
        System.setProperty("eventtracker.type", "SCRIBE");
        System.setProperty("eventtracker.directory", tmpDir.getAbsolutePath());
        System.setProperty("eventtracker.scribe.host", "127.0.0.1");
        System.setProperty("eventtracker.scribe.port", "7911");

        final Injector injector = Guice.createInjector(new CollectorControllerModule());
        final CollectorController controller = injector.getInstance(CollectorController.class);
        final ScribeSender sender = (ScribeSender) injector.getInstance(EventSender.class);

        sender.createConnection();

        fireThriftEvents(controller);

        sender.close();
    }

    @Test(groups = "slow", enabled = false)
    public void testScribeFactory() throws Exception
    {
        System.setProperty("eventtracker.type", "COLLECTOR");
        System.setProperty("eventtracker.directory", tmpDir.getAbsolutePath());
        System.setProperty("eventtracker.collector.host", "127.0.0.1");
        System.setProperty("eventtracker.collector.port", "8080");

        final EventTrackerConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
        final CollectorController controller = ScribeCollectorFactory.createScribeController(
            config.getScribeHost(),
            config.getScribePort(),
            config.getScribeRefreshRate(),
            config.getScribeMaxIdleTimeInMinutes(),
            config.getSpoolDirectoryName(),
            config.isFlushEnabled(),
            config.getFlushIntervalInSeconds(),
            SyncType.valueOf(config.getSyncType()),
            config.getSyncBatchSize(),
            config.getMaxUncommittedWriteCount(),
            config.getMaxUncommittedPeriodInSeconds()
        );

        fireThriftEvents(controller);
    }

    private void fireThriftEvents(final CollectorController controller) throws Exception
    {
        controller.offerEvent(ThriftToThriftEnvelopeEvent.extractEvent("thrift", new DateTime(), new Click(UUID.randomUUID().toString(), new DateTime().getMillis(), "user agent")));
        Assert.assertEquals(controller.getEventsReceived().get(), 1);
        Assert.assertEquals(controller.getEventsLost().get(), 0);
        controller.commit();
        controller.flush();
        Thread.sleep(5000);
    }
}
