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

import com.google.inject.AbstractModule;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.EventWriter;
import com.ning.metrics.serialization.writer.StubScheduledExecutorService;
import com.ning.metrics.serialization.writer.SyncType;
import org.skife.config.ConfigurationObjectFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockCollectorControllerModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        final EventTrackerConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
        bind(EventTrackerConfig.class).toInstance(config);

        final EventSender eventSender = new MockCollectorSender();
        bind(EventSender.class).toInstance(eventSender);

        final ScheduledExecutorService executor = new StubScheduledExecutorService()
        {
            public AtomicBoolean isShutdown = new AtomicBoolean(false);

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
            {
                return true;
            }

            @Override
            public void shutdown()
            {
                isShutdown.set(true);
            }

            @Override
            public boolean isShutdown()
            {
                return isShutdown.get();
            }

            @Override
            public boolean isTerminated()
            {
                return isShutdown.get();
            }
        };
        bind(ScheduledExecutorService.class).toInstance(executor);

        bind(CollectorController.class).toProvider(CollectorControllerProvider.class).asEagerSingleton();

        bind(DiskSpoolEventWriter.class).toInstance(new MockDiskSpoolEventWriter(new EventHandler()
        {
            @Override
            public void handle(File file, CallbackHandler handler)
            {
                // Send a dummy event
                eventSender.send(file, handler);
            }
        }, config.getSpoolDirectoryName(), config.isFlushEnabled(), config.getFlushIntervalInSeconds(), executor,
            SyncType.valueOf(config.getSyncType()), config.getSyncBatchSize(), config.getRateWindowSizeMinutes()));
        bind(EventWriter.class).toProvider(ThresholdEventWriterProvider.class);
    }

    static class MockDiskSpoolEventWriter extends DiskSpoolEventWriter
    {
        private final EventHandler delegate;

        public MockDiskSpoolEventWriter(EventHandler eventHandler, String spoolPath, boolean flushEnabled, long flushIntervalInSeconds, ScheduledExecutorService executor, SyncType syncType, int syncBatchSize, int rateWindowSizeMinutes)
        {
            super(eventHandler, spoolPath, flushEnabled, flushIntervalInSeconds, executor, syncType, syncBatchSize, rateWindowSizeMinutes);
            this.delegate = eventHandler;
        }

        /**
         * We need to override the flush method as we don't want to deal with files
         *
         * @throws IOException generic IOException
         */
        @Override
        public void flush()
        {
            delegate.handle(null, new CallbackHandler()
            {
                @Override
                public void onError(Throwable t, File event)
                {
                }

                @Override
                public void onSuccess(File event)
                {
                }
            });
        }
    }
}
