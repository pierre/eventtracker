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

import com.mogwee.executors.FailsafeScheduledExecutor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LocalQueueAndWorkers
{
    private final Counter jobsEnqueued = Metrics.newCounter(LocalQueueAndWorkers.class, "jobsEnqueued");
    private final Counter jobsDropped = Metrics.newCounter(LocalQueueAndWorkers.class, "jobsDropped");

    private final BlockingQueue<HttpJob> queue = new LinkedBlockingQueue<HttpJob>();
    private final ExecutorService executor;

    public LocalQueueAndWorkers(final int senderCount)
    {
        this.executor = new FailsafeScheduledExecutor(senderCount, "http-SenderWorkers");

        for (int idx = 0; idx < senderCount; idx++) {
            executor.submit(new SenderWorker(queue));
        }
    }

    public void close()
    {
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        executor.shutdownNow();
        queue.clear();
    }

    public void offer(final HttpJob job)
    {
        if (queue.offer(job)) {
            jobsEnqueued.inc();
        }
        else {
            jobsDropped.inc();
        }
    }

    int queueSize()
    {
        return queue.size();
    }

    boolean isShutdown()
    {
        return executor.isShutdown();
    }
}