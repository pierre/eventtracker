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
import com.mogwee.executors.FailsafeScheduledExecutor;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventWriter;
import org.skife.config.ConfigurationObjectFactory;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Wires all pieces related to talking to the Collector core.
 * See http://github.com/pierre/collector
 * <p/>
 * Note that Guice injection is optional, you can directly instantiate a CollectorController
 * via the factories.
 */
class CollectorControllerModule extends AbstractModule
{
    EventTrackerConfig eventTrackerConfig;

    public static enum Type
    {
        SCRIBE,
        COLLECTOR,
        NO_LOGGING
    }

    @Override
    protected void configure()
    {
        eventTrackerConfig = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
        bind(EventTrackerConfig.class).toInstance(eventTrackerConfig);

        bind(ScheduledExecutorService.class).toInstance(new FailsafeScheduledExecutor(1, "EventtrackerFlusher"));

        bind(CollectorController.class).toProvider(CollectorControllerProvider.class).asEagerSingleton();

        bind(DiskSpoolEventWriter.class).toProvider(DiskSpoolEventWriterProvider.class).asEagerSingleton();
        bind(EventWriter.class).toProvider(ThresholdEventWriterProvider.class).asEagerSingleton();
    }
}
