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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.smile.SmileEnvelopeEventsToSmileBucketEvents;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.SyncType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class DiskSpoolEventWriterProvider implements Provider<DiskSpoolEventWriter>
{
    private final EventTrackerConfig config;
    private final EventSender eventSender;
    private final ScheduledExecutorService executor;

    @Inject
    public DiskSpoolEventWriterProvider(
        EventTrackerConfig config,
        EventSender eventSender,
        ScheduledExecutorService executor
    )
    {
        this.config = config;
        this.eventSender = eventSender;
        this.executor = executor;
    }

    /**
     * Provides an instance of {@code T}. Must never return {@code null}.
     *
     * @throws com.google.inject.OutOfScopeException
     *          when an attempt is made to access a scoped object while the scope
     *          in question is not currently active
     * @throws com.google.inject.ProvisionException
     *          if an instance cannot be provided. Such exceptions include messages
     *          and throwables to describe why provision failed.
     */
    @Override
    public DiskSpoolEventWriter get()
    {
        return new DiskSpoolEventWriter(new EventHandler()
        {
            @Override
            public void handle(ObjectInputStream objectInputStream, CallbackHandler handler) throws ClassNotFoundException, IOException
            {
                ArrayList<SmileEnvelopeEvent> events = new ArrayList<SmileEnvelopeEvent>();

                while (objectInputStream.read() != -1) {
                    Event event = (Event) objectInputStream.readObject();

                    // TODO This is suboptimal as it requires a dependency to com.ning:metrics-serialization
                    // How could we be smarter here? Specific Smile DiskSpoolEventWriter, manually configured by the user?
                    if (event instanceof SmileEnvelopeEvent) {
                        events.add((SmileEnvelopeEvent) event);
                    }
                    else {
                        // Not a SmileEnvelopeEvent: it is either already a SmileBucketEvent or a ThriftEnvelopeEvent.
                        // In both cases, don't buffer.
                        eventSender.send(event, handler);
                    }
                }

                objectInputStream.close();

                if (events.size() > 0) {
                    for (Event event : SmileEnvelopeEventsToSmileBucketEvents.extractEvents(events)) {
                        eventSender.send(event, handler);
                    }
                }
            }

            @Override
            public void rollback() throws IOException
            {
                // no-op
            }
        }, config.getSpoolDirectoryName(), config.isFlushEnabled(), config.getFlushIntervalInSeconds(), executor, SyncType.valueOf(config.getSyncType()), config.getSyncBatchSize(), config.getRateWindowSizeMinutes());
    }
}
