/*
 * Copyright 2010-2012 Ning, Inc.
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
import com.ning.http.client.Request;
import com.ning.http.client.Response;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe wrapper class around the AsyncHttpClient which has weak synchronization contracts
 * between the client itself and the underlying providers (e.g. on close).
 */
public class ThreadSafeAsyncHttpClient
{
    private static final String URI_PATH = "/rest/1.0/event";
    private static final int DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = 120000; // 2 minutes
    private static final Map<EventType, String> headers = new HashMap<EventType, String>();

    static {
        headers.put(EventType.SMILE, "application/json+smile");
        headers.put(EventType.JSON, "application/json");
        headers.put(EventType.THRIFT, "ning/thrift");
        headers.put(EventType.DEFAULT, "ning/1.0");
    }

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final String collectorURI;
    private final EventType eventType;
    private final AsyncHttpClientConfig clientConfig;
    /**
     * Timer used for ensuring we close persistent HTTP connections every now
     * and then.
     */
    private final ExpirationTimer httpConnectionExpiration;

    private AsyncHttpClient client = null;

    public ThreadSafeAsyncHttpClient(final String collectorHost, final int collectorPort, final EventType eventType, final long httpMaxKeepAliveInMillis)
    {
        this.collectorURI = String.format("http://%s:%d%s", collectorHost, collectorPort, URI_PATH);
        this.eventType = eventType;
        this.httpConnectionExpiration = new ExpirationTimer(httpMaxKeepAliveInMillis);

        // CAUTION: it is not enforced that the actual event encoding type on the wire matches what the config says it is
        // the event encoding type is determined by the Event's writeExternal() method.
        this.clientConfig = new AsyncHttpClientConfig.Builder()
                .setIdleConnectionInPoolTimeoutInMs(DEFAULT_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS)
                .setConnectionTimeoutInMs(100)
                .setMaximumConnectionsPerHost(-1) // unlimited connections
                .build();
    }

    public synchronized void executeRequest(final File file, final AsyncCompletionHandler<Response> completionHandler)
    {
        if (isClosed.get()) {
            return;
        }

        if (client == null || client.isClosed()) {
            client = createClient();
        }

        final Request request = createPostRequest(file);
        try {
            client.executeRequest(request, completionHandler);
        }
        catch (Exception e) {
            // Recycle the client on IOException and RuntimeExceptions
            client.close();
            client = createClient();
            completionHandler.onThrowable(e);
        }
    }

    public synchronized void close()
    {
        isClosed.set(true);
        if (client != null) {
            client.close();
        }
    }

    synchronized AsyncHttpClient createClient()
    {
        return new AsyncHttpClient(clientConfig);
    }

    Request createPostRequest(final File file)
    {
        AsyncHttpClient.BoundRequestBuilder requestBuilder = client
                .preparePost(collectorURI).setBody(file)
                .setHeader("Content-Type", headers.get(eventType)); // zero-bytes-copy

        /*
         * Need to ensure we won't be using a single connection indefinitely,
         * to ensure load balancing works.
         */
        if (httpConnectionExpiration.isExpired()) {
            requestBuilder = requestBuilder.setHeader("Connection", "close");
        }

        return requestBuilder.build();
    }
}