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
import com.ning.metrics.serialization.event.EventSerializer;
import com.ning.metrics.serialization.smile.SmileEnvelopeEventSerializer;
import com.ning.metrics.serialization.writer.ObjectOutputEventSerializer;

/**
 * Wires all pieces related to talking to the Collector core.
 * See http://github.com/pierre/collector
 */
public class CollectorControllerSmileModule extends AbstractModule
{
    private final EventTrackerConfig eventTrackerConfig;

    @Inject
    public CollectorControllerSmileModule(final EventTrackerConfig eventTrackerConfig)
    {
        this.eventTrackerConfig = eventTrackerConfig;
    }

    @Override
    protected void configure()
    {
        switch (eventTrackerConfig.getEventType()) {
            case SMILE:
                bind(EventSerializer.class).toInstance(new SmileEnvelopeEventSerializer(false));
                break;
            case JSON:
                bind(EventSerializer.class).toInstance(new SmileEnvelopeEventSerializer(true));
                break;
            default:
                bind(EventSerializer.class).to(ObjectOutputEventSerializer.class);
                break;
        }

        install(new CollectorControllerHttpModule(eventTrackerConfig));
    }
}
