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
import com.google.inject.Provider;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.SyncType;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

class DiskSpoolEventWriterProvider implements Provider<DiskSpoolEventWriter>
{
    private final EventTrackerConfig config;
    private final EventSender eventSender;
    private final ScheduledExecutorService executor;

    @Inject
    public DiskSpoolEventWriterProvider(
        final EventTrackerConfig config,
        final EventSender eventSender,
        final ScheduledExecutorService executor
    )
    {
        this.config = config;
        this.eventSender = eventSender;
        this.executor = executor;
    }

    /**
     * Provides an instance of a DiskSpoolEventWriter, which forwards local buffered events (in files) to the collector,
     * via an EventSender.
     *
     * @return instance of a DiskSpoolEventWriter
     */
    @Override
    public DiskSpoolEventWriter get()
    {
        return new DiskSpoolEventWriter(new EventHandler()
        {
            @Override
            public void handle(final File file, final CallbackHandler handler)
            {
                eventSender.send(file, handler);
            }
        }, config.getSpoolDirectoryName(), config.isFlushEnabled(), config.getFlushIntervalInSeconds(), executor,
            SyncType.valueOf(config.getSyncType()), config.getSyncBatchSize(), config.getRateWindowSizeMinutes(),
            config.getEventType().getSerializer());
    }
}
