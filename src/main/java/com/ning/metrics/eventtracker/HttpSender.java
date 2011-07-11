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
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.metrics.serialization.writer.CallbackHandler;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class HttpSender implements EventSender
{
    private static final Logger log = Logger.getLogger(HttpSender.class);
    private static final Map<EventType, String> headers = new HashMap<EventType, String>();

    private final AtomicLong activeRequests = new AtomicLong(0);

    static {
        headers.put(EventType.SMILE, "application/json+smile");
        headers.put(EventType.JSON, "application/json");
        headers.put(EventType.THRIFT, "ning/thrift");
        headers.put(EventType.DEFAULT, "ning/1.0");
    }

    public static final String URI_PATH = "/rest/1.0/event";
    private static final int DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = 120000; // 2 minutes

    private final EventTrackerConfig config;
    private final String collectorURI;
    private final AsyncHttpClientConfig clientConfig;

    private AsyncHttpClient client;

    @Inject
    public HttpSender(final EventTrackerConfig config)
    {
        this.config = config;
        collectorURI = String.format("http://%s:%d%s", config.getCollectorHost(), config.getCollectorPort(), URI_PATH);
        // CAUTION: it is not enforced that the actual event encoding type on the wire matches what the config says it is
        // the event encoding type is determined by the Event's writeExternal() method.
        clientConfig = new AsyncHttpClientConfig.Builder()
            .setIdleConnectionInPoolTimeoutInMs(DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS)
            .setConnectionTimeoutInMs(100)
            .setMaximumConnectionsPerHost(-1) // unlimited connections
            .build();
    }

    /**
     * Send a file full of events to the collector. This does zero-bytes-copy by default (the async-http-client does
     * it for us).
     *
     * @param file    File to send
     * @param handler callback handler for the serialization-writer library
     */
    @Override
    public void send(final File file, final CallbackHandler handler)
    {
        if (client == null || client.isClosed()) {
            client = new AsyncHttpClient(clientConfig);
        }

        try {
            client.executeRequest(createPostRequest(file),
                new AsyncCompletionHandler<Response>()
                {
                    @Override
                    public Response onCompleted(final Response response)
                    {
                        activeRequests.decrementAndGet();

                        if (response.getStatusCode() == 202) {
                            handler.onSuccess(file);
                        }
                        else {
                            handler.onError(new IOException(String.format("Received response %d: %s",
                                response.getStatusCode(), response.getStatusText())), file);
                        }

                        return response; // never read
                    }

                    @Override
                    public void onThrowable(final Throwable t)
                    {
                        activeRequests.decrementAndGet();
                        handler.onError(t, file);
                    }
                });

            activeRequests.incrementAndGet();
        }
        catch (IOException e) {
            // Recycle the client
            client.close();
            handler.onError(e, file);
        }
    }

    @Override
    public synchronized void close()
    {
        if (client != null && !client.isClosed()) {
            try {
                if (activeRequests.get() > 0) {
                    log.info(String.format("%d HTTP request(s) in progress, giving them some time to finish...", activeRequests.get()));
                }

                long sleptInMillins = config.getHttpMaxWaitTimeInMillis();
                while (activeRequests.get() > 0 && sleptInMillins >= 0) {
                    Thread.sleep(200);
                    sleptInMillins -= 200;
                }

                if (activeRequests.get() > 0) {
                    log.warn("Giving up on pending HTTP requests, shutting down NOW!");
                }
            }
            catch (InterruptedException e) {
                log.warn("Interrupted while waiting for active queries to finish");
                Thread.currentThread().interrupt();
            }
            finally {
                client.close();
            }
        }
    }

    private Request createPostRequest(final File file)
    {
        final AsyncHttpClient.BoundRequestBuilder requestBuilder = client
            .preparePost(collectorURI).setBody(file)
            .setHeader("Content-Type", headers.get(config.getEventType())); // zero-bytes-copy
        return requestBuilder.build();
    }
}
