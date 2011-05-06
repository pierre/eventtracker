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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import org.apache.thrift.transport.TTransportException;

/**
 * We only have a provider here to avoid Guice dependencies in the ScribeSender class
 */
class ScribeSenderProvider implements Provider<ScribeSender>
{
    private final EventTrackerConfig config;

    @Inject
    public ScribeSenderProvider(EventTrackerConfig config)
    {
        this.config = config;
    }

    /**
     * Provides an instance of {@code T}. Must never return {@code null}.
     *
     * @throws com.google.inject.OutOfScopeException
     *          when an attempt is made to access a scoped object while the scope
     *          in question is not currently active
     * @throws com.google.inject.ProvisionException
     *          if an instance cannot be provided. Such exceptions include messages
     *          and throwables to describe why provision failed.
     */
    @Override
    public ScribeSender get()
    {
        final ScribeClientImpl scribeClient = new ScribeClientImpl(config.getScribeHost(), config.getScribePort());
        try {
            scribeClient.openLogger();
        }
        catch (TTransportException e) {
            throw new ProvisionException("Unable to create Scribe client", e);
        }

        return new ScribeSender(scribeClient, config.getScribeRefreshRate(), config.getScribeMaxIdleTimeInMinutes());
    }
}