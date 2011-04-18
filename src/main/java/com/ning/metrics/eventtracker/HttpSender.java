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
import com.ning.http.client.SimpleAsyncHttpClient;
import com.ning.http.client.ThrowableHandler;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.writer.CallbackHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

class HttpSender implements EventSender
{
    public static final String URI_PATH = "/rest/1.0/event";
    private static final int DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = 120000; // 2 minutes

    private final String collectorURI;
    private final String httpContentType;

    @Inject
    public HttpSender(EventTrackerConfig config)
    {
        collectorURI = String.format("http://%s:%d%s", config.getCollectorHost(), config.getCollectorPort(), URI_PATH);
        httpContentType = EventEncodingType.valueOf(config.getHttpEventEncodingType()).toString();
    }

    @Override
    public void send(final Event event, final CallbackHandler handler)
    {
        // Construct client with appropriate query parameters
        SimpleAsyncHttpClient client = getHttpClient(event);

        // Serialize the event
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            event.writeExternal(new ObjectOutputStream(out));
        }
        catch (IOException e) {
            handler.onError(new Throwable(e), event);
            return;
        }

        // Submit the event
        try {
            client.post(
                new InputStreamBodyGenerator(new ByteArrayInputStream(out.toByteArray())),
                new ThrowableHandler()
                {
                    @Override
                    public void onThrowable(Throwable t)
                    {
                        handler.onError(t, event);
                    }
                });
        }
        catch (IOException e) {
            handler.onError(new Throwable(e), event);
        }

        handler.onSuccess(event);
    }

    private SimpleAsyncHttpClient getHttpClient(Event event)
    {
        final SimpleAsyncHttpClient.Builder clientBuilder = new SimpleAsyncHttpClient.Builder()
            .setIdleConnectionInPoolTimeoutInMs(DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS)
            .setHeader("Content-Type", httpContentType)
            .setUrl(collectorURI);

        clientBuilder.addParameter("name", event.getName());

        if (event.getEventDateTime() != null) {
            clientBuilder.addParameter("date", event.getEventDateTime().toString());
        }

        if (event.getGranularity() != null) {
            clientBuilder.addParameter("granularity", event.getGranularity().toString());
        }

        return clientBuilder.build();
    }
}
