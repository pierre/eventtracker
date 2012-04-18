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
import com.ning.http.client.Response;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.File;

public class TestHttpJob
{
    @Test(groups = "fast")
    public void testSubmitRequest() throws Exception
    {
        final ThreadSafeAsyncHttpClient client = Mockito.mock(ThreadSafeAsyncHttpClient.class);
        final File file = Mockito.mock(File.class);
        final AsyncCompletionHandler<Response> completionHandler = Mockito.mock(AsyncResponseCompletionHandler.class);
        final HttpJob job = new HttpJob(client, file, completionHandler);

        Mockito.verify(client, Mockito.times(0)).executeRequest(file, completionHandler);

        job.submitRequest();

        Mockito.verify(client, Mockito.times(1)).executeRequest(file, completionHandler);
    }
}
