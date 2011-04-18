package com.ning.metrics.eventtracker;

import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.event.SmileBucketEvent;
import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.thrift.ThriftEnvelope;
import com.ning.metrics.serialization.thrift.ThriftField;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class TestDiskSpoolEventWriterProvider
{
    private static final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector");
    private static final String EVENT_NAME = "myEnvelope";
    private static final DateTime EVENT_DATE_TIME = new DateTime();

    private EventTrackerConfig config;

    @BeforeTest(alwaysRun = true)
    public void setUp() throws Exception
    {
        config = new EventTrackerConfig();
        config.setSpoolDirectoryName(tmpDir.getAbsolutePath());
    }

    @AfterTest
    public void tearDown() throws Exception
    {
        tmpDir.delete();
    }

    @Test
    public void testMultipleSmileEnvelopeEvents() throws Exception
    {
        // Create a SmileEnvelope event
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("first", "hello");
        map.put("second", "world");
        Event event = new SmileEnvelopeEvent(EVENT_NAME, EVENT_DATE_TIME, map);

        final int numberOfSmileEventsToSend = 3;

        // Send 3 events, the writer library should bufferize them
        final AtomicInteger sendCalls = new AtomicInteger(0);
        DiskSpoolEventWriter diskSpoolEventWriter = diskWriterProvider(new EventSender()
        {
            @Override
            public void send(Event event, CallbackHandler handler)
            {
                Assert.assertTrue(event instanceof SmileBucketEvent);
                Assert.assertEquals(event.getName(), EVENT_NAME);
                Assert.assertEquals(((SmileBucketEvent) event).getNumberOfEvent(), numberOfSmileEventsToSend);
                sendCalls.incrementAndGet();
            }
        });

        // Flush 3 events, but we should get only one send call (buffering)
        for (int i = 0; i < numberOfSmileEventsToSend; i++) {
            diskSpoolEventWriter.write(event);
        }
        diskSpoolEventWriter.commit();
        diskSpoolEventWriter.flush();

        Assert.assertEquals(sendCalls.get(), 1);

        // Flush another series
        for (int i = 0; i < numberOfSmileEventsToSend; i++) {
            diskSpoolEventWriter.write(event);
        }
        diskSpoolEventWriter.commit();
        diskSpoolEventWriter.flush();

        Assert.assertEquals(sendCalls.get(), 2);
    }

    @Test
    public void testThriftEnvelopeEvent() throws Exception
    {
        // Create a Thrift event
        List<ThriftField> thriftFieldList = new ArrayList<ThriftField>();
        thriftFieldList.add(ThriftField.createThriftField("hello", (short) 1));
        thriftFieldList.add(ThriftField.createThriftField("world", (short) 12));
        ThriftEnvelope envelope = new ThriftEnvelope(EVENT_NAME, thriftFieldList);
        Event event = new ThriftEnvelopeEvent(EVENT_DATE_TIME, envelope);

        final int numberOfThriftEventsToSend = 3;

        // Create the DiskSpoolEventWriter
        final AtomicInteger sendCalls = new AtomicInteger(0);
        DiskSpoolEventWriter diskSpoolEventWriter = diskWriterProvider(new EventSender()
        {
            @Override
            public void send(Event event, CallbackHandler handler)
            {
                Assert.assertTrue(event instanceof ThriftEnvelopeEvent);
                Assert.assertEquals(event.getName(), EVENT_NAME);
                //TODO Assert.assertEquals(event.getEventDateTime().getMillis(), EVENT_DATE_TIME.getMillis());
                Assert.assertEquals(((ThriftEnvelope) event.getData()).getPayload().size(), 2);
                sendCalls.incrementAndGet();
            }
        });

        // Flush 3 events
        for (int i = 0; i < numberOfThriftEventsToSend; i++) {
            diskSpoolEventWriter.write(event);
        }
        diskSpoolEventWriter.commit();
        diskSpoolEventWriter.flush();

        // One call per event, no buffering
        Assert.assertEquals(sendCalls.get(), numberOfThriftEventsToSend);

        // Flush another series
        for (int i = 0; i < numberOfThriftEventsToSend; i++) {
            diskSpoolEventWriter.write(event);
        }
        diskSpoolEventWriter.commit();
        diskSpoolEventWriter.flush();

        // One call per event, no buffering
        Assert.assertEquals(sendCalls.get(), numberOfThriftEventsToSend + numberOfThriftEventsToSend);
    }

    /**
     * Create a DiskSpoolEventWriter with the specified EventSender
     *
     * @param sender EventSender to use
     * @return DiskSpoolEventWriter create via the DiskSpoolEventWriterProvider
     * @throws IOException generic serialization exception
     */
    private DiskSpoolEventWriter diskWriterProvider(EventSender sender) throws IOException
    {
        return new DiskSpoolEventWriterProvider(
            config,
            sender,
            new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory())
        ).get();
    }
}
