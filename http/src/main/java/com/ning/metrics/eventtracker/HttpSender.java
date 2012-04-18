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
import com.ning.http.client.Response;
import com.ning.metrics.serialization.writer.CallbackHandler;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HttpSender implements EventSender
{
    private static final Logger log = LoggerFactory.getLogger(HttpSender.class);

    private final AtomicLong activeRequests = new AtomicLong(0);
    private final LocalQueueAndWorkers workers;
    private final ThreadSafeAsyncHttpClient client;
    private final long httpMaxWaitTimeInMillis;
    private final Timer sendTimer;

    public HttpSender(final String collectorHost, final int collectorPort, final EventType eventType,
                      final long httpMaxWaitTimeInMillis, final long httpMaxKeepAliveInMillis, final int httpWorkersPoolSize)
    {
        this(new ThreadSafeAsyncHttpClient(collectorHost, collectorPort, eventType, httpMaxKeepAliveInMillis),
             httpMaxWaitTimeInMillis,
             Metrics.newTimer(HttpSender.class, collectorHost.replace(":", "_"), TimeUnit.MILLISECONDS, TimeUnit.SECONDS),
             httpWorkersPoolSize);
    }

    // For testing
    HttpSender(final ThreadSafeAsyncHttpClient client, final long httpMaxWaitTimeInMillis, final Timer sendTimer, final int httpWorkersPoolSize)
    {
        this.client = client;
        this.httpMaxWaitTimeInMillis = httpMaxWaitTimeInMillis;
        this.sendTimer = sendTimer;
        this.workers = new LocalQueueAndWorkers(httpWorkersPoolSize);
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
        log.info("Sending local file to collector: {}", file.getAbsolutePath());
        final long startTime = System.nanoTime();

        final AsyncCompletionHandler<Response> asyncCompletionHandler = new AsyncCompletionHandler<Response>()
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

                sendTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                return response; // never read
            }

            @Override
            public void onThrowable(final Throwable t)
            {
                activeRequests.decrementAndGet();
                handler.onError(t, file);
            }
        };

        final HttpJob job = new HttpJob(client, file, asyncCompletionHandler);
        workers.offer(job);
    }

    @Override
    public synchronized void close()
    {
        client.close();

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

            workers.close();
        }
        catch (InterruptedException e) {
            log.warn("Interrupted while waiting for active queries to finish");
            Thread.currentThread().interrupt();
        }
    }
}
