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
import com.ning.metrics.serialization.event.Events;
import com.ning.metrics.serialization.writer.CallbackHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.thrift.transport.TTransportException;
import org.weakref.jmx.Managed;
import scribe.thrift.LogEntry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScribeSender
 * <p/>
 * The class needs to be public for JMX.
 */
public class ScribeSender implements EventSender
{
    private static final Logger log = Logger.getLogger(ScribeSender.class);

    private static final Charset CHARSET = Charset.forName("ISO-8859-1");

    private final AtomicInteger connectionRetries = new AtomicInteger(0);
    private ScribeClient scribeClient;

    private final AtomicInteger messagesSuccessfullySent = new AtomicInteger(0);
    private final AtomicInteger messagesSuccessfullySentSinceLastReconnection = new AtomicInteger(0);
    private int messagesToSendBeforeReconnecting = 0;

    private final AtomicBoolean sleeping = new AtomicBoolean(true);
    private final AtomicBoolean isClosed = new AtomicBoolean(true);

    public ScribeSender(ScribeClient scribeClient, int messagesToSendBeforeReconnecting, int maxIdleTimeInMinutes)
    {
        this.scribeClient = scribeClient;
        this.messagesToSendBeforeReconnecting = messagesToSendBeforeReconnecting;

        // Setup a watchdog for the Scribe connection. We don't want to keep it open forever. For instance, SLB VIP
        // may trigger a RST if idle more than a few minutes.
        final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory());
        executor.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    if (sleeping.get()) {
                        log.info("Idle connection to Scribe, re-opening it");
                        createConnection();
                    }
                    sleeping.set(true);
                }
            }, maxIdleTimeInMinutes, maxIdleTimeInMinutes, TimeUnit.MINUTES);
    }

    /**
     * Re-initialize the connection with the Scribe endpoint.
     */
    public synchronized void createConnection()
    {
        if (scribeClient != null) {
            try {
                connectionRetries.incrementAndGet();
                scribeClient.closeLogger();
                scribeClient.openLogger();
                isClosed.set(false);

                log.info("Connection to Scribe established");
            }
            catch (TTransportException e) {
                log.warn(String.format("Unable to connect to Scribe: %s", e.getLocalizedMessage()));
                scribeClient.closeLogger();
            }
        }
        else {
            log.warn("Scribe client has not been set up correctly.");
        }
    }

    /**
     * Disconnect from Scribe for good.
     */
    @Override
    public synchronized void close()
    {
        if (scribeClient != null && !isClosed.get()) {
            scribeClient.closeLogger();
            isClosed.set(true);
        }
    }

    @Override
    public void send(final File file, final CallbackHandler handler)
    {
        if (scribeClient == null || isClosed.get()) {
            createConnection();
        }

        // Tell the watchdog that we are doing something
        sleeping.set(false);

        // Parse the underlying file and generate the payload for Scribe
        List<LogEntry> list = createScribePayload(file, handler);
        if (list == null) {
            // Something went wrong
            return;
        }

        try {
            scribeClient.log(list);
            // Get rid of the file. We do it early, because the reconnection may fail
            handler.onSuccess(file);

            messagesSuccessfullySent.addAndGet(list.size());
            messagesSuccessfullySentSinceLastReconnection.addAndGet(list.size());

            // For load balancing capabilities, we don't want to make sticky connections to Scribe.
            // After a certain threshold, force a refresh of the connection.
            if (messagesSuccessfullySentSinceLastReconnection.get() > messagesToSendBeforeReconnecting) {
                log.info("Recycling connection with Scribe");
                messagesSuccessfullySentSinceLastReconnection.set(0);
                createConnection();
            }
        }
        catch (org.apache.thrift.TException e) {
            // Connection flacky?
            log.warn(String.format("Error while sending message to Scribe: %s", e.getLocalizedMessage()));
            createConnection();
            handler.onError(new Throwable(e), file);
        }
    }

    /**
     * Give a file of events, generate LogEntry messages for Scribe
     *
     * @param file    File containing events
     * @param handler notifier for the serialization-writer library
     * @return list of Scirbe-ready events on success, null otherwise
     */
    private List<LogEntry> createScribePayload(File file, CallbackHandler handler)
    {
        try {
            List<Event> events = Events.fromFile(file);
            List<LogEntry> list = new ArrayList<LogEntry>(events.size());

            String logEntryMessage;
            for (Event event : events) {
                logEntryMessage = eventToLogEntryMessage(event);
                list.add(new LogEntry(event.getName(), logEntryMessage));
            }

            return list;
        }
        catch (ClassNotFoundException e) {
            handler.onError(new Throwable(e), file);
            return null;
        }
        catch (IOException e) {
            handler.onError(new Throwable(e), file);
            return null;
        }
    }

    protected static String eventToLogEntryMessage(Event event) throws IOException
    {
        // Has the sender specified how to send the data?
        byte[] payload = event.getSerializedEvent();

        // Nope, default to ObjectOutputStream
        if (payload == null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            event.writeExternal(new ObjectOutputStream(out));
            payload = out.toByteArray();

            // 64-bit encode the serialized object
            payload = new Base64().encode(payload);
        }

        String scribePayload = new String(payload, CHARSET);

        // To avoid costly Thrift deserialization on the collector side, we embed the
        // timestamp in the format, outside of the payload. We need it for HDFS routing.
        return String.format("%s:%s", event.getEventDateTime().getMillis(), scribePayload);
    }

    @Managed(description = "Get the number of messages successfully sent since startup to Scribe")
    public long getMessagesSuccessfullySent()
    {
        return messagesSuccessfullySent.get();
    }

    @Managed(description = "Get the number of messages successfully sent since last reconnection to Scribe")
    public long getMessagesSuccessfullySentSinceLastReconnection()
    {
        return messagesSuccessfullySentSinceLastReconnection.get();
    }

    @Managed(description = "Get the number of times we retried to connect to Scribe")
    public long getConnectionRetries()
    {
        return connectionRetries.get();
    }
}
