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
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadSafeWithMockedAsyncHttpClient extends ThreadSafeAsyncHttpClient
{
    private final AtomicInteger recreations;
    private final AtomicBoolean shouldFailDuringCallback = new AtomicBoolean(false);
    private final AtomicBoolean shouldFailWhenSubmittingRequest = new AtomicBoolean(false);
    private final AtomicReference<AsyncHttpClient> clientAtomicReference = new AtomicReference<AsyncHttpClient>();

    public ThreadSafeWithMockedAsyncHttpClient()
    {
        this(new AtomicInteger(0), false, false);
    }

    public ThreadSafeWithMockedAsyncHttpClient(final AtomicInteger recreations, final boolean shouldFailDuringCallback, final boolean shouldFailWhenSubmittingRequest)
    {
        super(UUID.randomUUID().toString(), Integer.MAX_VALUE, EventType.DEFAULT, System.currentTimeMillis());

        this.recreations = recreations;
        this.shouldFailDuringCallback.set(shouldFailDuringCallback);
        this.shouldFailWhenSubmittingRequest.set(shouldFailWhenSubmittingRequest);
    }

    @Override
    synchronized AsyncHttpClient createClient()
    {
        recreations.incrementAndGet();

        final AsyncHttpClient client = Mockito.mock(AsyncHttpClient.class);
        try {
            if (shouldFailWhenSubmittingRequest.get()) {
                Mockito.doThrow(IOException.class).when(client).executeRequest(Mockito.<Request>any(), Mockito.<AsyncHandler<Object>>any());
            }
            else {
                Mockito.doAnswer(new Answer()
                {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable
                    {
                        final AsyncCompletionHandler completionHandler = (AsyncCompletionHandler) invocation.getArguments()[1];

                        if (shouldFailDuringCallback.get()) {
                            completionHandler.onThrowable(new RuntimeException("Triggered exception for tests"));
                        }
                        else {
                            final Response response = Mockito.mock(Response.class);
                            Mockito.when(response.getStatusCode()).thenReturn(202);
                            completionHandler.onCompleted(response);
                        }

                        return null;
                    }
                }).when(client).executeRequest(Mockito.<Request>any(), Mockito.<AsyncHandler<Object>>any());
            }
        }
        catch (IOException e) {
            Assert.fail();
        }

        clientAtomicReference.set(client);
        return client;
    }

    @Override
    Request createPostRequest(final File file)
    {
        return Mockito.mock(Request.class);
    }

    public AsyncHttpClient getClient()
    {
        return clientAtomicReference.get();
    }
}

