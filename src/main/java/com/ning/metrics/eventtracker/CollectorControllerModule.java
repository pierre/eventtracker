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

import com.google.inject.AbstractModule;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventWriter;
import org.apache.log4j.Logger;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Wires all pieces related to talking to the Collector core.
 * See http://github.com/pierre/collector
 * <p/>
 * Note that Guice injection is optional, you can directly instantiate a CollectorController
 * via the factories.
 * <p/>
 * Note! MBeanModule expects an MBeanServer to be bound, e.g. make sure to:
 * binder().bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
 *
 * @see com.ning.metrics.eventtracker.ScribeCollectorFactory
 */
public class CollectorControllerModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(CollectorControllerModule.class);

    public static enum Type
    {
        SCRIBE,
        COLLECTOR,
        NO_LOGGING
    }

    protected Type type;

    @Override
    protected void configure()
    {
        install(new MBeanModule());
        ExportBuilder builder = MBeanModule.newExporter(binder());

        final EventTrackerConfig eventTrackerConfig = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
        bind(EventTrackerConfig.class).toInstance(eventTrackerConfig);

        type = Type.valueOf(eventTrackerConfig.getType());

        switch (type) {
            case COLLECTOR:
                bind(EventSender.class).to(HttpSender.class).asEagerSingleton();
                log.info("Enabled Collector Event Logging");
                break;
            case SCRIBE:
                bind(EventSender.class).toProvider(ScribeSenderProvider.class).asEagerSingleton();
                builder.export(ScribeSender.class).as("eventtracker:name=ScribeSender");
                log.info("Enabled Scribe Event Logging");
                break;
            case NO_LOGGING:
                bind(EventSender.class).to(NoLoggingSender.class).asEagerSingleton();
                log.info("Disabled Event Logging");
                break;
            default:
                throw new IllegalStateException("Unknown type " + type);
        }

        bind(ScheduledExecutorService.class).toInstance(new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory()));

        bind(CollectorController.class).asEagerSingleton();
        builder.export(CollectorController.class).as("eventtracker:name=CollectorController");

        bind(DiskSpoolEventWriter.class).toProvider(DiskSpoolEventWriterProvider.class).asEagerSingleton();
        bind(EventWriter.class).toProvider(ThresholdEventWriterProvider.class);
    }
}
