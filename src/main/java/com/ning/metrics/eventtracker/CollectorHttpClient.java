/*
 * Copyright 2010 Ning, Inc.
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

import com.ning.metrics.serialization.event.Event;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Client class to post Events to the Event collector.
 */
public class CollectorHttpClient
{
    public static final String URI_PATH = "/rest/1.0/event";

    private final CollectorUriBuilder uriBuilder;
    private final HttpClient httpClient;
    private final Header header = new Header("Content-type", "ning/thrift");

    /**
     * Creates a new Collector Client that uses a fixed host and port as target.
     *
     * @param host   Collector hostname or SLB VIP
     * @param port   Collector port
     * @param client HttpClient to use
     */
    public CollectorHttpClient(String host, int port, HttpClient client)
    {
        this(new SimpleUriBuilder(String.format("http://%s:%d%s", host, port, URI_PATH)), client);
    }

    /**
     * Creates a new Collector Client that uses an URI builder implementation to find
     * the host to talk to.
     *
     * @param uriBuilder CollectorUriBuilder to use
     * @param httpClient HttpClient to use
     */
    public CollectorHttpClient(final CollectorUriBuilder uriBuilder, final HttpClient httpClient)
    {
        this.uriBuilder = uriBuilder;
        this.httpClient = httpClient;
    }

    /**
     * Logs an event to the collector. The <code>eventDate</code> is used to figure out what path to store the event under, e.g., <code>/events/ning/Foo/2009/07/12/20</code>
     *
     * @param event Event to send
     * @return true if event was accepted, false if it was rejected
     * @throws IOException for I/O exceptions
     */
    public boolean postThrift(Event event) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        event.writeExternal(new ObjectOutputStream(out));
        byte[] payload = out.toByteArray();

        // Post to collector

        final CollectorUriBuilder requestUriBuilder = uriBuilder.clone()
            .addQueryParam("name", event.getName());

        if (event.getEventDateTime() != null) {
            requestUriBuilder.addQueryParam("date", event.getEventDateTime().toString());
        }

        if (event.getGranularity() != null) {
            requestUriBuilder.addQueryParam("granularity", event.getGranularity().toString());
        }

        PostMethod method = new PostMethod(requestUriBuilder.build().toString());
        method.setRequestEntity(new ByteArrayRequestEntity(payload));
        method.addRequestHeader(header);

        boolean success = false;
        try {
            int result = httpClient.executeMethod(method);
            success = (result == 202); //accepted;
        }
        finally {
            method.releaseConnection();
        }

        return success;
    }

}