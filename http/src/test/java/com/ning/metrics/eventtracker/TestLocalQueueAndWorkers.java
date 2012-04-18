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

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.MetricName;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;

public class TestLocalQueueAndWorkers
{
    private static final MetricName JOBS_ENQUEUED = new MetricName(LocalQueueAndWorkers.class, "jobsEnqueued");
    private static final MetricName JOBS_DROPPED = new MetricName(LocalQueueAndWorkers.class, "jobsDropped");

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception
    {
        // The default registry is static :(
        Metrics.defaultRegistry().removeMetric(JOBS_ENQUEUED);
        Metrics.defaultRegistry().removeMetric(JOBS_DROPPED);
    }

    @Test(groups = "fast", expectedExceptions = IllegalArgumentException.class)
    public void testSetup()
    {
        new LocalQueueAndWorkers(-1);
        // We should never come here
        Assert.fail();
    }

    @Test(groups = "fast")
    public void testSubmitJobsWithNoWorker() throws Exception
    {
        final HttpJob job = Mockito.mock(HttpJob.class);
        final LocalQueueAndWorkers workers = new LocalQueueAndWorkers(0);
        final Counter jobsEnqueued = (Counter) Metrics.defaultRegistry().allMetrics().get(JOBS_ENQUEUED);
        final Counter jobsDropped = (Counter) Metrics.defaultRegistry().allMetrics().get(JOBS_DROPPED);

        // Initial state
        Assert.assertEquals(workers.queueSize(), 0);
        Assert.assertEquals(workers.isShutdown(), false);
        Assert.assertEquals(jobsEnqueued.count(), 0);
        Assert.assertEquals(jobsDropped.count(), 0);

        for (int i = 1; i < 10; i++) {
            workers.offer(job);
            Assert.assertEquals(workers.queueSize(), i);
            Assert.assertEquals(workers.isShutdown(), false);
            Assert.assertEquals(jobsEnqueued.count(), i);
            Assert.assertEquals(jobsDropped.count(), 0);
        }

        // Close and verify final state
        workers.close();
        Assert.assertEquals(workers.queueSize(), 0);
        Assert.assertEquals(workers.isShutdown(), true);

        // Verify no worker was ever spawned
        Mockito.verify(job, Mockito.times(0)).submitRequest();
    }

    @Test(groups = "fast")
    public void testSubmitJobsWithWorker() throws Exception
    {
        // Latch to make this thread block until the worker has done his job
        final CountDownLatch latch = new CountDownLatch(1);
        final HttpJob job = Mockito.mock(HttpJob.class);
        Mockito.doAnswer(new Answer()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                return null;
            }
        }).when(job).submitRequest();
        final LocalQueueAndWorkers workers = new LocalQueueAndWorkers(1);
        final Counter jobsEnqueued = (Counter) Metrics.defaultRegistry().allMetrics().get(JOBS_ENQUEUED);
        final Counter jobsDropped = (Counter) Metrics.defaultRegistry().allMetrics().get(JOBS_DROPPED);

        // Initial state
        Assert.assertEquals(workers.queueSize(), 0);
        Assert.assertEquals(workers.isShutdown(), false);
        Assert.assertEquals(jobsEnqueued.count(), 0);
        Assert.assertEquals(jobsDropped.count(), 0);

        workers.offer(job);

        // Wait for the worker to do its job
        latch.await();
        Mockito.verify(job, Mockito.times(1)).submitRequest();
        Assert.assertEquals(workers.queueSize(), 0);
        Assert.assertEquals(workers.isShutdown(), false);
        Assert.assertEquals(jobsEnqueued.count(), 1);
        Assert.assertEquals(jobsDropped.count(), 0);

        // Close and verify final state
        workers.close();
        Assert.assertEquals(workers.queueSize(), 0);
        Assert.assertEquals(workers.isShutdown(), true);
    }
}