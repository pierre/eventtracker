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
import com.ning.metrics.serialization.writer.EventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CollectorControllerProvider implements Provider<CollectorController>
{
    private static final Logger log = LoggerFactory.getLogger(CollectorControllerProvider.class);

    private final EventWriter eventWriter;
    private final EventSender eventSender;

    @Inject
    public CollectorControllerProvider(final EventWriter eventWriter, final EventSender eventSender)
    {
        this.eventWriter = eventWriter;
        this.eventSender = eventSender;
    }

    @Override
    public CollectorController get()
    {
        final CollectorController controller = new CollectorController(eventWriter);

        // Make sure to flush all files on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                mainEventTrackerShutdownHook(eventSender, controller);
            }
        });

        return controller;
    }

    protected static void mainEventTrackerShutdownHook(
        final EventSender eventSender,
        final CollectorController controller)
    {
        log.info("Closing the collector controller");
        controller.close();

        log.info("Closing event sender");
        eventSender.close();
    }
}
