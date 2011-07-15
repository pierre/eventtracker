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
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.thrift.ThriftEnvelope;
import com.ning.metrics.serialization.thrift.ThriftField;
import org.apache.thrift.TException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


@Test(enabled = false)
public class TestPerformance
{
    private final File temporarySpoolDir = new File(System.getProperty("java.io.tmpdir"), "collector");
    private final File tmpDir = new File(temporarySpoolDir, "_tmp");
    private static final Logger logger = LoggerFactory.getLogger(TestPerformance.class);
    private int responsesLeft;
    private DateTime flushStart;
    private final int numTestEvents = 10000;

    @BeforeTest(alwaysRun = true)
    private void setupTmpDir() throws Exception
    {
        if (!temporarySpoolDir.exists() && !temporarySpoolDir.mkdirs()) {
            throw new RuntimeException("Failed to create: " + temporarySpoolDir);
        }

        if (!temporarySpoolDir.isDirectory()) {
            throw new RuntimeException("Path points to something that's not a directory: " + temporarySpoolDir);
        }
    }

    @AfterTest(alwaysRun = true)
    private void cleanupTmpDir()
    {
        temporarySpoolDir.delete();
    }

//    // TODO this is a mess. Figure out how to make it work while still using a CollectorController.
//    @Test(enabled = false)
//    public void testSmile() throws IOException, InterruptedException
//    {
//        // stall so that jconsole can connect
////        Thread.sleep(15000);
//
//        System.setProperty("eventtracker.type", "COLLECTOR");
//        System.setProperty("eventtracker.directory", temporarySpoolDir.getAbsolutePath());
////        System.setProperty("eventtracker.collector.host", "127.0.0.1");
////        System.setProperty("eventtracker.collector.host", "z12209f.ningops.com");
//        System.setProperty("eventtracker.collector.host", "10.18.81.135");
//        System.setProperty("eventtracker.collector.port", "8080");
//        System.setProperty("eventtracker.diskspool.flush-event-queue-size", "50");
//
//        EventTrackerConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
//
//        // set up a writer using a receiveResponse handler (hacky)
//        DiskSpoolEventWriter writer = new DiskSpoolEventWriterProvider(config,
//                new HttpSender(config)
//                {
//                    private CallbackHandler overrideHandler = new CallbackHandler()
//                    {
//
//                        @Override
//                        public void onSuccess(Event event)
//                        {
//                            receiveSmileResponse();
//                        }
//
//                        @Override
//                        public void onError(Throwable t, Event event)
//                        {
//                            Assert.fail();
//                        }
//                    };
//
//                    // UBERhack. Ignore the handler argument and use this one instead!
//                    @Override
//                    public void send(Event event, CallbackHandler handler)
//                    {
//                        super.send(event, overrideHandler);
//                    }
//                },
//                new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory())
//        ).get();
//
//        List<SmileEnvelopeEvent> events = makeSmileEvents(numTestEvents);
//
//        long writeCommitStart = new DateTime().getMillis();
//        // commit and flush and all that jazz
//
//        for (int i=0; i<1000; i++) {
//            for (Event event : events) {
//                writer.write(event);
//            }
//            writer.commit();
//            flushStart = new DateTime();
//            writer.flush();
//            long commitTimeDiff = flushStart.getMillis() - writeCommitStart;
//            logger.info(String.format("SMILE: wrote and committed all events in %d.%d seconds", commitTimeDiff / 1000, commitTimeDiff % 1000));
//            Thread.sleep(15000);
//        }
//    }

    private void receiveSmileResponse()
    {
        DateTime sendFinishTime = new DateTime();
        responsesLeft--;

        synchronized (this) {
            long timeDiff = sendFinishTime.getMillis() - flushStart.getMillis();
            logger.info("SMILE: event sent in {}.{} seconds", timeDiff / 1000, timeDiff % 1000);
            if (responsesLeft == 0) {
                logger.info("SMILE: all events sent within {}.{} seconds", timeDiff / 1000, timeDiff % 1000);
                Assert.assertEquals(tmpDir.listFiles().length, 0);
                // don't output this stuff again
                responsesLeft = -1;
            }
            Assert.assertTrue(responsesLeft >= -1, "Something is wrong with how this test was written. It receives more responses than it expects!");
        }
    }


//    @Test(enabled = false)
//    public void testThrift() throws Exception
//    {
//        // stall so that jconsole can connect
////        Thread.sleep(15000);
//
//        System.setProperty("eventtracker.type", "SCRIBE");
//        System.setProperty("eventtracker.directory", temporarySpoolDir.getAbsolutePath());
////        System.setProperty("eventtracker.scribe.host", "127.0.0.1");
//        System.setProperty("eventtracker.scribe.host", "10.18.81.135");
////        System.setProperty("eventtracker.scribe.host", "z12209f.ningops.com");
//        System.setProperty("eventtracker.scribe.port", "7911");
//
//        Injector injector = Guice.createInjector(new CollectorControllerModule());
//        CollectorController controller = injector.getInstance(CollectorController.class);
//        ScribeSender sender = (ScribeSender) injector.getInstance(EventSender.class);
//
//        sender.createConnection();
//
//        List<ThriftEnvelopeEvent> events = makeThriftEvents(numTestEvents);
//
//        sendThriftEvents(controller, events);
//
//        sender.shutdown();
//    }


    private LinkedList<ThriftEnvelopeEvent> makeThriftEvents(int numEvents) throws TException
    {
        LinkedList<ThriftEnvelopeEvent> events = new LinkedList<ThriftEnvelopeEvent>();
        long sampleTime = new DateTime().getMillis();
        DateTime startTime = new DateTime();
        ArrayList<ThriftField> data = new ArrayList<ThriftField>();
        String eventName;


        data.add(ThriftField.createThriftField(sampleTime, (short) 1));
        data.add(ThriftField.createThriftField("foo", (short) 2));
        data.add(ThriftField.createThriftField("bar", (short) 3));
        data.add(ThriftField.createThriftField(3.14159, (short) 4));
        data.add(ThriftField.createThriftField(10001000000L, (short) 5));

        for (int i = 0; i < numEvents; i++) {

            switch (i % 3) {
                case 0:
                    eventName = "A";
                    break;
                case 1:
                    eventName = "B";
                    break;
                default:
                    eventName = "C";
            }

            events.add(new ThriftEnvelopeEvent(startTime, new ThriftEnvelope(eventName, data)));
        }

        DateTime endTime = new DateTime();
        long timeTaken = endTime.getMillis() - startTime.getMillis();
        logger.info("THRIFT: %d events created in {}.{} seconds", new Object[] {numEvents, timeTaken/1000, timeTaken%1000}); // TODO output time in a friendly format

        return events;
    }

    private void sendThriftEvents(CollectorController controller, List<ThriftEnvelopeEvent> events) throws Exception
    {
        DateTime writeCommitStart = new DateTime();
        for(Event event : events) {
            controller.offerEvent(event);
        }
        controller.commit();
        flushStart = new DateTime();
        controller.flush();
        // thrift events are simpler. once flush completes, the events are sent! not complicated by async
        DateTime flushFinish = new DateTime();
        long commitTimeDiff = flushStart.getMillis() - writeCommitStart.getMillis();
        long timeDiff = flushFinish.getMillis() - flushStart.getMillis();
        logger.info("THRIFT: wrote and committed all events in {}.{} seconds", commitTimeDiff/1000, commitTimeDiff%1000);
        logger.info("THRIFT: events sent in {}.{} seconds", timeDiff/1000, timeDiff%1000);
    }
}
