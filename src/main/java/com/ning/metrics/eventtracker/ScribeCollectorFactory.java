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

import com.ning.metrics.serialization.util.FixedManagedJmxExport;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.FileHandler;
import com.ning.metrics.serialization.writer.SyncType;
import com.ning.metrics.serialization.writer.ThresholdEventWriter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ScribeCollectorFactory
{
    private static CollectorController singletonController;

    private final CollectorController controller;
    private static ScribeSender eventSender;

    public static synchronized CollectorController createScribeController(EventTrackerConfig config) throws IOException
    {
        if (singletonController == null) {
            singletonController = new ScribeCollectorFactory(config).get();
        }

        return singletonController;
    }

    ScribeCollectorFactory(EventTrackerConfig config)
    {
        eventSender = new ScribeSender(new ScribeClientImpl(config.getScribeHost(), config.getScribePort()), config.getScribeRefreshRate(), config.getScribeMaxIdleTimeInMinutes());
        FixedManagedJmxExport.export("com.ning.metrics.eventtracker:name=ScribeSender", eventSender);
        eventSender.createConnection();

        DiskSpoolEventWriter eventWriter = new DiskSpoolEventWriter(new FileHandler()
        {
            @Override
            public void handle(File file, CallbackHandler handler) throws IOException
            {
                eventSender.send(file, handler);
            }
        }, config.getSpoolDirectoryName(), config.isFlushEnabled(), config.getFlushIntervalInSeconds(),
            new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory()), SyncType.valueOf(config.getSyncType()),
            config.getSyncBatchSize(), config.getRateWindowSizeMinutes());
        ThresholdEventWriter thresholdEventWriter = new ThresholdEventWriter(eventWriter, config.getFlushEventQueueSize(), config.getRefreshDelayInSeconds());

        controller = new CollectorController(thresholdEventWriter);
        FixedManagedJmxExport.export("eventtracker:name=CollectorController", controller);
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
