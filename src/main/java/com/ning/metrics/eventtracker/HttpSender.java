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
import com.ning.http.client.*;
import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.writer.CallbackHandler;
import org.joda.time.DateTime;

import java.io.IOException;

class HttpSender implements EventSender
{
    public static final String URI_PATH = "/rest/1.0/event";
    private static final int DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = 120000; // 2 minutes

    private final String collectorURI;
    private final String httpContentType;
    private final AsyncHttpClient client;

    @Inject
    public HttpSender(EventTrackerConfig config)
    {
        collectorURI = String.format("http://%s:%d%s", config.getCollectorHost(), config.getCollectorPort(), URI_PATH);
        // CAUTION: it is not enforced that the actual event encoding type on the wire matches what the config says it is
        // the event encoding type is determined by the Event's writeExternal() method.
        httpContentType = EventEncodingType.valueOf(config.getHttpEventEncodingType()).toString();
        AsyncHttpClientConfig clientConfig = new AsyncHttpClientConfig.Builder()
                .setIdleConnectionInPoolTimeoutInMs(DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS)
                .setConnectionTimeoutInMs(100)
                .setMaximumConnectionsPerHost(-1) // unlimited connections
                .build();
        client = new AsyncHttpClient(clientConfig);
    }

    @Override
    public void send(final Event event, final CallbackHandler handler)
    {
        // Submit the event
        try {
            client.executeRequest(createPostRequest(event),
                    new AsyncCompletionHandler<String>()
                    {
                        @Override
                        public String onCompleted(Response response) throws Exception
                        {
                            if (response.getStatusCode() == 202) {
                                handler.onSuccess(event);
                            }
                            else {
                                handler.onError(new Throwable(String.format("Received response %d: %s",response.getStatusCode(),response.getStatusText())), null);
                            }
                            return response.getResponseBody();
                        }

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
    }

    // TODO when should we close this?
    public void closeClient()
    {
        client.close();
    }

    private Request createPostRequest(Event event)
    {
        byte[] serializedEvent = event.getSerializedEvent();
        AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePost(collectorURI)
                .addHeader("Content-Length", String.valueOf(serializedEvent.length))
                .addHeader("Content-Type", httpContentType)
                .setBody(serializedEvent)
                .addQueryParameter("name", event.getName());

        if (event.getEventDateTime() != null) {
            requestBuilder.addQueryParameter("date", event.getEventDateTime().toString());
        }

        if (event.getGranularity() != null) {
            requestBuilder.addQueryParameter("granularity", event.getGranularity().toString());
        }

        return requestBuilder.build();
    }
}
