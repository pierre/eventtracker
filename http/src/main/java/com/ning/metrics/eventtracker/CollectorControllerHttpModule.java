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
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires all pieces related to talking to the Collector core.
 * See http://github.com/pierre/collector
 * <p/>
 * Note that Guice injection is optional, you can directly instantiate a CollectorController
 * via the factories.
 */
public class CollectorControllerHttpModule extends AbstractModule
{
    private static final Logger log = LoggerFactory.getLogger(CollectorControllerHttpModule.class);

    private final EventTrackerConfig eventTrackerConfig;

    @Inject
    public CollectorControllerHttpModule(final EventTrackerConfig eventTrackerConfig)
    {
        this.eventTrackerConfig = eventTrackerConfig;
    }

    @Override
    protected void configure()
    {
        switch (eventTrackerConfig.getType()) {
            case COLLECTOR:
                bind(EventSender.class).to(HttpSender.class).asEagerSingleton();
                log.info("Enabled HTTP Event Logging");
                break;
            case NO_LOGGING:
                bind(EventSender.class).to(NoLoggingSender.class).asEagerSingleton();
                log.info("Disabled Event Logging");
                break;
            default:
                throw new IllegalStateException("Unknown type " + eventTrackerConfig.getType());
        }

        install(new CollectorControllerModule());
    }
}
