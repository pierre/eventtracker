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

import com.mogwee.executors.FailsafeScheduledExecutor;
import com.ning.metrics.serialization.event.EventSerializer;
import com.ning.metrics.serialization.smile.SmileEnvelopeEventSerializer;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.ObjectOutputEventSerializer;
import com.ning.metrics.serialization.writer.SyncType;
import com.ning.metrics.serialization.writer.ThresholdEventWriter;

import java.io.File;
import java.io.IOException;

public class HttpCollectorFactory
{
    private static CollectorController singletonController;

    private final CollectorController controller;
    private static HttpSender eventSender;

    public static synchronized CollectorController createHttpController(
        final String collectorHost,
        final int collectorPort,
        final EventType eventType,
        final long httpMaxWaitTimeInMillis,
        final String spoolDirectoryName,
        final boolean isFlushEnabled,
        final int flushIntervalInSeconds,
        final SyncType syncType,
        final int syncBatchSize,
        final long maxUncommittedWriteCount,
        final int maxUncommittedPeriodInSeconds
    ) throws IOException
    {
        if (singletonController == null) {
            singletonController = new HttpCollectorFactory(
                collectorHost,
                collectorPort,
                eventType,
                httpMaxWaitTimeInMillis,
                spoolDirectoryName,
                isFlushEnabled,
                flushIntervalInSeconds,
                syncType,
                syncBatchSize,
                maxUncommittedWriteCount,
                maxUncommittedPeriodInSeconds
            ).get();
        }

        return singletonController;
    }

    HttpCollectorFactory(
        final String collectorHost,
        final int collectorPort,
        final EventType eventType,
        final long httpMaxWaitTimeInMillis,
        final String spoolDirectoryName,
        final boolean isFlushEnabled,
        final int flushIntervalInSeconds,
        final SyncType syncType,
        final int syncBatchSize,
        final long maxUncommittedWriteCount,
        final int maxUncommittedPeriodInSeconds
    )
    {
        eventSender = new HttpSender(collectorHost, collectorPort, eventType, httpMaxWaitTimeInMillis);

        EventSerializer serializer = new ObjectOutputEventSerializer();
        switch (eventType) {
            case SMILE:
                serializer = new SmileEnvelopeEventSerializer(false);
                break;
            case JSON:
                serializer = new SmileEnvelopeEventSerializer(true);
                break;
        }

        final DiskSpoolEventWriter eventWriter = new DiskSpoolEventWriter(new EventHandler()
        {
            @Override
            public void handle(final File file, final CallbackHandler handler)
            {
                eventSender.send(file, handler);
            }
        }, spoolDirectoryName, isFlushEnabled, flushIntervalInSeconds, new FailsafeScheduledExecutor(1, "EventtrackerFlusher"),
            syncType, syncBatchSize, serializer);

        final ThresholdEventWriter thresholdEventWriter = new ThresholdEventWriter(eventWriter, maxUncommittedWriteCount, maxUncommittedPeriodInSeconds);
        controller = new CollectorController(thresholdEventWriter);
    }

    private CollectorController get()
    {
        return controller;
    }

    public static void shutdown()
    {
        eventSender.close();
    }
}
