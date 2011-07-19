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

import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.writer.EventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.Managed;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main entry point of the library. The CollectorController is responsible for forwarding events to the persister layer.
 * <p/>
 * The class needs to be public for JMX.
 */
public class CollectorController
{
    private static final Logger log = LoggerFactory.getLogger(CollectorController.class);

    private final EventWriter eventWriter;

    private final AtomicLong eventsReceived = new AtomicLong(0);
    private final AtomicLong eventsLost = new AtomicLong(0);
    private final AtomicBoolean acceptEvents = new AtomicBoolean(true);

    public CollectorController(final EventWriter eventWriter)
    {
        this.eventWriter = eventWriter;
        log.debug("Initialized Collector Controller with file manager [{}]", eventWriter);
    }

    /**
     * Offer an event to the queue.
     *
     * @param event an event to collect
     * @throws IOException if a serialization exception (to disk) occurs
     */
    public void offerEvent(final Event event) throws IOException
    {
        if (!acceptEvents.get()) {
            // TODO shouldn't we increment eventsLost here?
            return;
        }

        eventsReceived.incrementAndGet();

        try {
            log.debug("Writing event: {}", event);
            eventWriter.write(event);
        }
        catch (IOException e) {
            log.error(String.format("Failed to write event: %s", event), e);
            eventsLost.incrementAndGet();

            throw e;
        }
    }

    public void close()
    {
        setAcceptEvents(false);
        try {
            commit();
            flush();
            eventWriter.close();
        }
        catch (IOException e) {
            log.warn("Got I/O exception closing the eventtracker library: " + e);
        }
    }

    public void setAcceptEvents(final boolean accept)
    {
        acceptEvents.set(accept);
    }

    @Managed(description = "Whether the eventtracker library accepts events")
    public boolean isAcceptEvents()
    {
        return acceptEvents.get();
    }

    @Managed(description = "Number of events received")
    public AtomicLong getEventsReceived()
    {
        return eventsReceived;
    }

    @Managed(description = "Number of events lost (unable to serialize them to disk)")
    public AtomicLong getEventsLost()
    {
        return eventsLost;
    }

    @Managed(description = "Promote events to final spool area")
    public void commit() throws IOException
    {
        eventWriter.forceCommit();
    }

    @Managed(description = "Flush events to remote agent")
    public void flush() throws IOException
    {
        eventWriter.flush();
    }
}
