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
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.NoCompressionCodec;
import com.ning.metrics.serialization.writer.ObjectOutputEventSerializer;
import com.ning.metrics.serialization.writer.SyncType;
import com.ning.metrics.serialization.writer.ThresholdEventWriter;

import java.io.File;
import java.io.IOException;

public class ScribeCollectorFactory
{
    private static CollectorController singletonController;

    private final CollectorController controller;
    private static ScribeSender eventSender;

    public static synchronized CollectorController createScribeController(
        final String scribeHost,
        final int scribePort,
        final int scribeRefreshRate,
        final int scribeMaxIdleTimeInMinutes,
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
            singletonController = new ScribeCollectorFactory(
                scribeHost,
                scribePort,
                scribeRefreshRate,
                scribeMaxIdleTimeInMinutes,
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

    ScribeCollectorFactory(
        final String scribeHost,
        final int scribePort,
        final int scribeRefreshRate,
        final int scribeMaxIdleTimeInMinutes,
        final String spoolDirectoryName,
        final boolean isFlushEnabled,
        final int flushIntervalInSeconds,
        final SyncType syncType,
        final int syncBatchSize,
        final long maxUncommittedWriteCount,
        final int maxUncommittedPeriodInSeconds
    )
    {
        eventSender = new ScribeSender(new ScribeClientImpl(scribeHost, scribePort), scribeRefreshRate, scribeMaxIdleTimeInMinutes);
        eventSender.createConnection();

        final DiskSpoolEventWriter eventWriter = new DiskSpoolEventWriter(new EventHandler()
        {
            @Override
            public void handle(final File file, final CallbackHandler handler)
            {
                eventSender.send(file, handler);
            }
        }, spoolDirectoryName, isFlushEnabled, flushIntervalInSeconds, new FailsafeScheduledExecutor(1, "EventtrackerFlusher"),
            syncType, syncBatchSize, new NoCompressionCodec(), new ObjectOutputEventSerializer());

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
