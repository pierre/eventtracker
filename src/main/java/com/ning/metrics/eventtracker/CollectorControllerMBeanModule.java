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
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

/**
 * Expose JMX properties for the eventtracker library.
 * <p/>
 * To enable them, install this module and configure jmxutils:
 * install(new MBeanModule());
 * // MBeanModule expects an MBeanServer to be bound, e.g. make sure to:
 * binder().bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
 */
public class CollectorControllerMBeanModule extends AbstractModule
{
    /**
     * Configures a {@link com.google.inject.Binder} via the exposed methods.
     */
    @Override
    protected void configure()
    {
        ExportBuilder builder = MBeanModule.newExporter(binder());

        final EventTrackerConfig eventTrackerConfig = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
        final CollectorControllerModule.Type type = eventTrackerConfig.getType();

        switch (type) {
            case SCRIBE:
                builder.export(ScribeSender.class).as("eventtracker:name=ScribeSender");
                break;
            default:
                break;
        }

        builder.export(CollectorController.class).as("eventtracker:name=CollectorController");
    }
}
