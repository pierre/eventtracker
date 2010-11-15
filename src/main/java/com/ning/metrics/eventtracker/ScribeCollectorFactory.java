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

import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.util.FixedManagedJmxExport;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.SyncType;
import com.ning.metrics.serialization.writer.ThresholdEventWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ScribeCollectorFactory
{
    private static CollectorController singletonController;

    private final CollectorController controller;
    private static ScribeSender eventSender;

    /**
     * Initialize the Scribe controller without Guice
     *
     * @param scribeHost             Scribe hostname or IP
     * @param scribePort             Scribe port
     * @param scribeRefreshRate      Number of messages to send to Scribe before refreshing the connection
     * @param spoolDirectoryName     Directory name for the spool queue
     * @param isFlushEnabled         Whether to send events to remote agent
     * @param flushIntervalInSeconds Delay between flushes (in seconds) to remote agent
     * @param syncType               type of Sync (NONE, FLUSH or SYNC)
     * @param rateWindowSizeMinutes  event rate window size
     * @param flushEventQueueSize    Maximum queue size in the temporary spooling area
     * @param refreshDelayInSeconds  Number of seconds before promoting events from tmp to final spool queue
     * @return Scribe controller
     * @throws java.io.IOException if an Exception occurs while trying to create the directory
     * @see com.ning.metrics.eventtracker.CollectorController
     */
    @SuppressWarnings("unused")
    public static synchronized CollectorController createScribeController(
        String scribeHost,
        int scribePort,
        int scribeRefreshRate,
        String spoolDirectoryName,
        boolean isFlushEnabled,
        long flushIntervalInSeconds,
        String syncType,
        int rateWindowSizeMinutes,
        long flushEventQueueSize,
        long refreshDelayInSeconds
    ) throws IOException
    {
        if (singletonController == null) {
            singletonController = new ScribeCollectorFactory(scribeHost, scribePort, scribeRefreshRate, spoolDirectoryName, isFlushEnabled, flushIntervalInSeconds, syncType, rateWindowSizeMinutes, flushEventQueueSize, refreshDelayInSeconds).get();
        }

        return singletonController;
    }

    ScribeCollectorFactory(
        String scribeHost,
        int scribePort,
        int scribeRefreshRate,
        String spoolDirectoryName,
        boolean isFlushEnabled,
        long flushIntervalInSeconds,
        String syncType,
        int rateWindowSizeMinutes,
        long flushEventQueueSize,
        long refreshDelayInSeconds
    ) throws IOException
    {
        eventSender = new ScribeSender(new ScribeClientImpl(scribeHost, scribePort), scribeRefreshRate);
        FixedManagedJmxExport.export("eventtracker:name=ScribeSender", eventSender);
        eventSender.createConnection();

        DiskSpoolEventWriter eventWriter = new DiskSpoolEventWriter(new EventHandler()
        {
            @Override
            public void handle(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException
            {
                while (objectInputStream.read() != -1) {
                    Event event = (Event) objectInputStream.readObject();
                    eventSender.send(event);
                }

                objectInputStream.close();
            }

            @Override
            public void rollback() throws IOException
            {
                // no-op
            }
        }, spoolDirectoryName, isFlushEnabled, flushIntervalInSeconds, new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory()), SyncType.valueOf(syncType), rateWindowSizeMinutes);
        ThresholdEventWriter thresholdEventWriter = new ThresholdEventWriter(eventWriter, flushEventQueueSize, refreshDelayInSeconds);

        controller = new CollectorController(thresholdEventWriter);
        FixedManagedJmxExport.export("eventtracker:name=CollectorController", controller);
    }

    private CollectorController get()
    {
        return controller;
    }

    public static void shutdown()
    {
        eventSender.shutdown();
    }
}
