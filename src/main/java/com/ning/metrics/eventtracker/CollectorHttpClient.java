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
import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.thrift.ThriftEnvelope;
import com.ning.metrics.serialization.thrift.ThriftField;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.VisibilityChecker;

import java.io.*;
import java.util.LinkedHashMap;

/**
 * Client class to post Events to the Event collector.
 */
public class CollectorHttpClient
{
    private static final Logger logger = Logger.getLogger(CollectorHttpClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final LinkedHashMap<Short, String> map = new LinkedHashMap<Short, String>();

    public static final String URI_PATH = "/rest/1.0/event";

    private final CollectorUriBuilder uriBuilder;
    private final HttpClient httpClient;
    private final EventTrackerConfig config;

    /**
     * Creates a new Collector Client that uses a fixed host and port as target.
     *
     * @param host   Collector hostname or SLB VIP
     * @param port   Collector port
     * @param client HttpClient to use
     */
    public CollectorHttpClient(String host, int port, HttpClient client, EventTrackerConfig config)
    {
        this(new SimpleUriBuilder(String.format("http://%s:%d%s", host, port, URI_PATH)), client, config);
    }

    /**
     * Creates a new Collector Client that uses an URI builder implementation to find
     * the host to talk to.
     *
     * @param uriBuilder CollectorUriBuilder to use
     * @param httpClient HttpClient to use
     */
    public CollectorHttpClient(final CollectorUriBuilder uriBuilder, final HttpClient httpClient, EventTrackerConfig config)
    {
        this.uriBuilder = uriBuilder;
        this.httpClient = httpClient;
        this.config = config;

        VisibilityChecker vc = new VisibilityChecker.Std(JsonAutoDetect.Visibility.PUBLIC_ONLY, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibilityChecker(vc);
    }

    /**
     * Posts event to the collector using the HttpEventEncodingType specified in config.
     * <p/>
     * USE CASE: event is a ThriftEnvelopeEvent but we want JSON/SMILE on the wire
     * then we convert the ThriftEnvelopeEvent to a SmileBucketEvent before posting
     * USE CASE (UNADDRESSED): TODO address this.
     * event is SMILE/JSON and we want to send THRIFT on the wire
     *
     * @param event Event to post
     */
    public boolean post(Event event)
    {
        try {
            switch (EventEncodingType.valueOf(config.getHttpEventEncodingType())) {
                case JSON:
                    return postJson(event);
                case SMILE:
                    return postSmile(event);
                case THRIFT:
                    return postThrift(event);
                default:
                    logger.warn("Bad event encoding: " + config.getHttpEventEncodingType());
                    return false;
            }
        }
        catch (IOException ex) {
            logger.warn("Error while sending message: " + ex.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Logs an event to the collector.
     *
     * @param event Event to send
     * @return true if event was accepted, false if it was rejected
     * @throws IOException for I/O exceptions
     */
    private boolean postThrift(Event event) throws IOException
    {
        return post(event, EventEncodingType.THRIFT);
    }

    /**
     * Logs an event to the collector.
     * If event is ThriftEnvelopeEvent, convert to SmileEnvelopeEvent then post.
     *
     * @param event Event to send
     * @return true if event was accepted, false if it was rejected
     * @throws IOException for I/O exceptions
     */
    private boolean postSmile(Event event) throws IOException
    {
        if (event instanceof ThriftEnvelopeEvent) {
            SmileEnvelopeEvent smileEvent = thriftEnvelopeEventToSmileEnvelopeEvent((ThriftEnvelopeEvent) event);
            smileEvent.setPlainJson(false);
            return post(smileEvent, EventEncodingType.SMILE);
        }
        else {
            return post(event, EventEncodingType.SMILE);
        }
    }

    /**
     * Logs an event to the collector.
     * If event is ThriftEnvelopeEvent, convert to SmileEnvelopeEvent then post.
     *
     * @param event Event to send
     * @return true if event was accepted, false if it was rejected
     * @throws IOException for I/O exceptions
     */
    private boolean postJson(Event event) throws IOException
    {
        if (event instanceof ThriftEnvelopeEvent) {
            SmileEnvelopeEvent smileEvent = thriftEnvelopeEventToSmileEnvelopeEvent((ThriftEnvelopeEvent) event);
            smileEvent.setPlainJson(true);
            return post(smileEvent, EventEncodingType.JSON);
        }
        else {
            return post(event, EventEncodingType.JSON);
        }
    }

    /**
     * Logs an event to the collector. The <code>eventDate</code> is used to figure out what path to store the event under, e.g., <code>/events/ning/Foo/2009/07/12/20</code>
     *
     * @param event             Event to send
     * @param eventEncodingType The EventEncodingType (THRIFT, JSON, SMILE)
     * @return true if event was accepted, false if it was rejected
     * @throws IOException for I/O exceptions
     */
    private boolean post(Event event, EventEncodingType eventEncodingType) throws IOException
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
        method.addRequestHeader(new Header("Content-type", eventEncodingType.toString()));

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

    private SmileEnvelopeEvent thriftEnvelopeEventToSmileEnvelopeEvent(ThriftEnvelopeEvent event)
    {
        map.clear();
        for (ThriftField field : ((ThriftEnvelope) (event).getData()).getPayload()) {
            map.put(field.getId(), field.getDataItem().getString());
        }
        return new SmileEnvelopeEvent(event.getName(), mapper.convertValue(map, JsonNode.class));
    }
}
