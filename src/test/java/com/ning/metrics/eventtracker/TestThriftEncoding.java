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
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.event.ThriftToThriftEnvelopeEvent;
import com.ning.metrics.serialization.thrift.ThriftEnvelope;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;

public class TestThriftEncoding
{
    private Event thriftEvent;

    @BeforeTest(alwaysRun = false)
    public void setUp() throws TTransportException
    {
        this.thriftEvent = ThriftToThriftEnvelopeEvent.extractEvent("thrift", new DateTime(), new Click("impression", 12, "Mozilla-foo"));
    }

    @Test(groups = "fast", enabled = false)
    public void testCollectorDecoding() throws Exception
    {
        String eventString = ScribeSender.eventToLogEntryMessage(thriftEvent);
        ThriftEnvelopeEvent eventFromString = (ThriftEnvelopeEvent) extractEvent("thrift", eventString);
        Assert.assertEquals(eventFromString.getEventDateTime(), thriftEvent.getEventDateTime());

        ThriftEnvelope thriftEnvelope = (ThriftEnvelope) thriftEvent.getData();
        ThriftEnvelope envelopeFromString = (ThriftEnvelope) eventFromString.getData();
        Assert.assertEquals(envelopeFromString.getPayload().size(), thriftEnvelope.getPayload().size());
        for (int i = 0; i < envelopeFromString.getPayload().size(); i++) {
            Assert.assertEquals(envelopeFromString.getPayload().get(i).toByteArray(), thriftEnvelope.getPayload().get(i).toByteArray());
        }
    }

    private Event extractEvent(String category, String message) throws TException, IOException
    {
        Event event;
        Long eventDateTime;

        String[] payload = StringUtils.split(message, ":");

        byte[] thrift = new Base64().decode(payload[1].getBytes());
        try {
            eventDateTime = Long.parseLong(payload[0]);
            event = ThriftToThriftEnvelopeEvent.extractEvent(category, new DateTime(eventDateTime), thrift);
        }
        catch (RuntimeException e) {
            event = ThriftToThriftEnvelopeEvent.extractEvent(category, thrift);
        }

        return event;
    }
}
