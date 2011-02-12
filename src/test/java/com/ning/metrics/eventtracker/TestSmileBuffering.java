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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.event.Granularity;
import com.ning.metrics.serialization.event.SmileBucketEvent;
import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.event.ThriftToThriftEnvelopeEvent;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventWriter;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;
import org.codehaus.jackson.smile.SmileParser;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class TestSmileBuffering
{
    private CollectorController controller;
    private static MockCollectorSender sender;
    private static final int EVENTS_TO_SEND = 2;
    private static final String EVENT_NAME = "event-name";
    private static final DateTime EVENT_DATE_TIME = new DateTime();

    @BeforeTest(alwaysRun = true)
    private void setupController() throws IOException
    {
        final EventTrackerConfig config = new EventTrackerConfig();
        config.setSpoolDirectoryName("/tmp/eventtracker");
        sender = new MockCollectorSender();

        Injector injector = Guice.createInjector(new AbstractModule()
        {
            /**
             * Configures a {@link com.google.inject.Binder} via the exposed methods.
             */
            @Override
            protected void configure()
            {
                bind(EventTrackerConfig.class).toInstance(config);
                bind(EventSender.class).toInstance(sender);
                bind(ScheduledExecutorService.class).toInstance(new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory()));
                bind(DiskSpoolEventWriter.class).toProvider(DiskSpoolEventWriterProvider.class);
                bind(EventWriter.class).toProvider(ThresholdEventWriterProvider.class);
            }
        });

        controller = injector.getInstance(CollectorController.class);
    }

    @AfterTest(alwaysRun = true)
    private void shutdownController() throws IOException
    {
        // Cleanups
        controller.commit();
        controller.flush();
        sender.clear();
    }

    @Test(groups = "slow")
    public void testOfferSmile() throws IOException, InterruptedException
    {
        Event event = createSmileEvent();

        sendMultipleEvents(event);

        // It should have sent all events in one shot (all events are compressed at each flush)
        Assert.assertEquals(sender.getSuccessCount(), 1);

        // Check the correctness of events
        Assert.assertEquals(sender.getReceivedEvent().getClass(), SmileBucketEvent.class);
        SmileBucketEvent eventSent = (SmileBucketEvent) sender.getReceivedEvent();
        Assert.assertEquals(eventSent.getNumberOfEvent(), EVENTS_TO_SEND);
        Assert.assertEquals(eventSent.getName(), event.getName());
        for (JsonNode node : eventSent.getBucket()) {
            SmileEnvelopeEvent zeEvent = new SmileEnvelopeEvent(EVENT_NAME, node);
            Assert.assertEquals(zeEvent.getEventDateTime(), event.getEventDateTime());
            Assert.assertEquals(zeEvent.getGranularity(), event.getGranularity());
            Assert.assertEquals(zeEvent.getSerializedEvent(), event.getSerializedEvent());
        }
    }

    @Test(groups = "slow")
    // This test makes sure Smile buffering doesn't affect Thrift
    public void testOfferThrift() throws IOException, InterruptedException
    {
        Event event = createThriftEvent();

        sendMultipleEvents(event);

        // It should have sent all events
        Assert.assertEquals(sender.getSuccessCount(), EVENTS_TO_SEND);
        // Check the latest event to double check correctness
        Assert.assertEquals(sender.getReceivedEvent().getClass(), ThriftEnvelopeEvent.class);
        Assert.assertEquals(sender.getReceivedEvent().getName(), event.getName());
        Assert.assertEquals(sender.getReceivedEvent().getEventDateTime(), event.getEventDateTime());
        Assert.assertEquals(sender.getReceivedEvent().getData(), event.getData());
    }
    private void sendMultipleEvents(Event event) throws IOException, InterruptedException
    {
        // Not sure why that's needed but it is.
        sender.clear();

        for (int i = 0; i < EVENTS_TO_SEND; i++) {
            controller.offerEvent(event);
        }

        // Force a flush
        controller.commit();
        controller.flush();

        // Give the backend some chance to get scheduled
        Thread.sleep(600);
    }

    private SmileEnvelopeEvent createSmileEvent() throws IOException
    {
        // Use same configuration as SmileEnvelopeEvent
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        f.configure(SmileParser.Feature.REQUIRE_HEADER, false);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonGenerator g = f.createJsonGenerator(stream);

        g.writeStartObject();
        g.writeStringField(SmileEnvelopeEvent.SMILE_EVENT_GRANULARITY_TOKEN_NAME, Granularity.HOURLY.toString());
        g.writeObjectFieldStart("name");
        g.writeStringField("first", "Joe");
        g.writeStringField("last", "Sixpack");
        g.writeEndObject(); // for field 'name'
        g.writeStringField("gender", "MALE");
        g.writeNumberField(SmileEnvelopeEvent.SMILE_EVENT_DATETIME_TOKEN_NAME, EVENT_DATE_TIME.getMillis());
        g.writeBooleanField("verified", false);
        g.writeEndObject();
        g.close(); // important: will force flushing of output, close underlying output stream

        return new SmileEnvelopeEvent(EVENT_NAME, stream.toByteArray(), EVENT_DATE_TIME, Granularity.HOURLY);
    }

    private ThriftEnvelopeEvent createThriftEvent()
    {
        return ThriftToThriftEnvelopeEvent.extractEvent(EVENT_NAME, EVENT_DATE_TIME, new Click("thrift", 12, "some data"));
    }
}
