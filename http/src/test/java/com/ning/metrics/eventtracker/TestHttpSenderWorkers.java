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

import com.ning.metrics.serialization.writer.CallbackHandler;

import com.yammer.metrics.core.Timer;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class TestHttpSenderWorkers
{
    @Test(groups = "slow")
    public void testSuccess() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);

        final AtomicInteger successes = new AtomicInteger(0);
        final AtomicInteger failures = new AtomicInteger(0);

        final File file = Mockito.mock(File.class);
        final CallbackHandler handler = new CallbackHandler()
        {
            @Override
            public void onError(final Throwable t, final File file)
            {
                failures.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onSuccess(final File obj)
            {
                successes.incrementAndGet();
                latch.countDown();
            }
        };
        final AtomicInteger recreations = new AtomicInteger(0);
        final ThreadSafeWithMockedAsyncHttpClient client = new ThreadSafeWithMockedAsyncHttpClient(recreations, false, false);
        final HttpSender sender = new HttpSender(client, System.currentTimeMillis(), Mockito.mock(Timer.class), 10);

        Assert.assertEquals(successes.get(), 0);
        Assert.assertEquals(failures.get(), 0);
        Assert.assertEquals(recreations.get(), 0);

        sender.send(file, handler);
        latch.await();

        Assert.assertEquals(successes.get(), 1);
        Assert.assertEquals(failures.get(), 0);
        // One (re)creation on setup
        Assert.assertEquals(recreations.get(), 1);
        Mockito.verify(client.getClient(), Mockito.times(0)).close();

        sender.close();
        Mockito.verify(client.getClient(), Mockito.times(1)).close();
    }

    @Test(groups = "slow")
    public void testFailureDuringCallback() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);

        final AtomicInteger successes = new AtomicInteger(0);
        final AtomicInteger failures = new AtomicInteger(0);

        final File file = Mockito.mock(File.class);
        final CallbackHandler handler = new CallbackHandler()
        {
            @Override
            public void onError(final Throwable t, final File file)
            {
                failures.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onSuccess(final File obj)
            {
                successes.incrementAndGet();
                latch.countDown();
            }
        };
        final AtomicInteger recreations = new AtomicInteger(0);
        final ThreadSafeWithMockedAsyncHttpClient client = new ThreadSafeWithMockedAsyncHttpClient(recreations, true, false);
        final HttpSender sender = new HttpSender(client, System.currentTimeMillis(), Mockito.mock(Timer.class), 10);

        Assert.assertEquals(successes.get(), 0);
        Assert.assertEquals(failures.get(), 0);
        Assert.assertEquals(recreations.get(), 0);

        sender.send(file, handler);
        latch.await();

        Assert.assertEquals(successes.get(), 0);
        Assert.assertEquals(failures.get(), 1);
        // One (re)creation on setup
        Assert.assertEquals(recreations.get(), 1);
        Mockito.verify(client.getClient(), Mockito.times(0)).close();

        sender.close();
        Mockito.verify(client.getClient(), Mockito.times(1)).close();
    }

    @Test(groups = "slow")
    public void testFailureWhenSubmittingRequest() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);

        final AtomicInteger successes = new AtomicInteger(0);
        final AtomicInteger failures = new AtomicInteger(0);

        final File file = Mockito.mock(File.class);
        final CallbackHandler handler = new CallbackHandler()
        {
            @Override
            public void onError(final Throwable t, final File file)
            {
                failures.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onSuccess(final File obj)
            {
                successes.incrementAndGet();
                latch.countDown();
            }
        };
        final AtomicInteger recreations = new AtomicInteger(0);
        final ThreadSafeWithMockedAsyncHttpClient client = new ThreadSafeWithMockedAsyncHttpClient(recreations, false, true);
        final HttpSender sender = new HttpSender(client, System.currentTimeMillis(), Mockito.mock(Timer.class), 10);

        Assert.assertEquals(successes.get(), 0);
        Assert.assertEquals(failures.get(), 0);
        Assert.assertEquals(recreations.get(), 0);

        sender.send(file, handler);
        latch.await();

        Assert.assertEquals(successes.get(), 0);
        Assert.assertEquals(failures.get(), 1);
        // One (re)creation on setup
        Assert.assertEquals(recreations.get(), 2);

        sender.close();
        Mockito.verify(client.getClient(), Mockito.times(1)).close();
    }
}