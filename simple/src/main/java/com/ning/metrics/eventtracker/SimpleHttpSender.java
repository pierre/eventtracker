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

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleHttpSender
{
    private static final Logger log = LoggerFactory.getLogger(SimpleHttpSender.class);

    private final AtomicLong activeRequests = new AtomicLong(0);

    public static final String URI_PATH = "/1?v=";
    private static final int DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = 120000; // 2 minutes

    private final long httpMaxWaitTimeInMillis;
    private final String collectorURI;
    private final AsyncHttpClientConfig clientConfig;

    private AsyncHttpClient client;

    public SimpleHttpSender(final String collectorHost, final int collectorPort, final long httpMaxWaitTimeInMillis)
    {
        this.httpMaxWaitTimeInMillis = httpMaxWaitTimeInMillis;
        collectorURI = String.format("http://%s:%d%s", collectorHost, collectorPort, URI_PATH);
        clientConfig = new AsyncHttpClientConfig.Builder()
            .setIdleConnectionInPoolTimeoutInMs(DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS)
            .setConnectionTimeoutInMs(100)
            .setMaximumConnectionsPerHost(-1) // unlimited connections
            .build();
    }

    /**
     * Send a single event to the collector
     *
     * @param eventPayload Event to sent, created by the EventBuilder
     * @return true on success (collector got the event), false otherwise (event was lost)
     * @throws InterruptedException if interrupted during the HTTP call
     * @throws java.util.concurrent.ExecutionException
     *                              generic ExecutionException
     */
    public boolean send(final String eventPayload) throws ExecutionException, InterruptedException
    {
        if (client == null || client.isClosed()) {
            client = new AsyncHttpClient(clientConfig);
        }

        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder = client
                .prepareGet(collectorURI + eventPayload);
            log.debug("Sending event to collector: {}", eventPayload);

            activeRequests.incrementAndGet();
            return client.executeRequest(requestBuilder.build(),
                new AsyncCompletionHandler<Boolean>()
                {
                    @Override
                    public Boolean onCompleted(final Response response)
                    {
                        activeRequests.decrementAndGet();

                        if (response.getStatusCode() == 202) {
                            return true;
                        }
                        else {
                            log.warn("Received response from collector {}: {}", response.getStatusCode(), response.getStatusText());
                            return false;
                        }
                    }

                    @Override
                    public void onThrowable(final Throwable t)
                    {
                        activeRequests.decrementAndGet();
                    }
                }).get();
        }
        catch (IOException e) {
            // Recycle the client
            client.close();
            return false;
        }
    }

    public synchronized void close()
    {
        if (client != null && !client.isClosed()) {
            try {
                if (activeRequests.get() > 0) {
                    log.info(String.format("%d HTTP request(s) in progress, giving them some time to finish...", activeRequests.get()));
                }

                long sleptInMillins = httpMaxWaitTimeInMillis;
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
}
