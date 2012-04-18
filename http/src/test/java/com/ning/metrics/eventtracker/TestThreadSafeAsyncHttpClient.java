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

import com.ning.http.client.Request;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.File;

public class TestThreadSafeAsyncHttpClient
{
    @Test(groups = "fast")
    public void testExecuteRequestWithClosedClient() throws Exception
    {
        final ThreadSafeWithMockedAsyncHttpClient client = new ThreadSafeWithMockedAsyncHttpClient();

        client.executeRequest(Mockito.mock(File.class), Mockito.mock(AsyncResponseCompletionHandler.class));
        Mockito.verify(client.getClient(), Mockito.times(0)).close();
        Mockito.verify(client.getClient(), Mockito.times(1)).executeRequest(Mockito.<Request>any(), Mockito.<AsyncResponseCompletionHandler>any());

        // Close the client
        client.close();
        Mockito.verify(client.getClient(), Mockito.times(1)).close();
        Mockito.verify(client.getClient(), Mockito.times(1)).executeRequest(Mockito.<Request>any(), Mockito.<AsyncResponseCompletionHandler>any());

        // Make sure no more request is accepted
        client.executeRequest(Mockito.mock(File.class), Mockito.mock(AsyncResponseCompletionHandler.class));
        Mockito.verify(client.getClient(), Mockito.times(1)).close();
        Mockito.verify(client.getClient(), Mockito.times(1)).executeRequest(Mockito.<Request>any(), Mockito.<AsyncResponseCompletionHandler>any());
    }
}