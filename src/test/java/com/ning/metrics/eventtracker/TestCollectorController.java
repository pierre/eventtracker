package com.ning.metrics.eventtracker;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class TestCollectorController
{
    private final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector");
    private Event event;
    private CollectorController controller;
    private DiskSpoolEventWriter diskWriter;

    @BeforeTest
    public void setUp() throws IOException
    {
        System.setProperty("eventtracker.type", "NO_LOGGING");
        System.setProperty("eventtracker.diskspool.path", tmpDir.getAbsolutePath());

        HashMap<String, Object> eventMap = new HashMap<String, Object>();
        eventMap.put("foo", "bar");
        eventMap.put("bleh", 12);
        event = new SmileEnvelopeEvent("myEvent", new DateTime(), eventMap);

        Injector injector = Guice.createInjector(new CollectorControllerModule());
        controller = injector.getInstance(CollectorController.class);
        diskWriter = injector.getInstance(DiskSpoolEventWriter.class); // We have only one
    }

    @AfterTest
    public void cleanupTmpDir()
    {
        tmpDir.delete();
    }

    @Test(groups = "fast")
    public void testOfferEvent() throws Exception
    {
        controller.offerEvent(event);
        Assert.assertEquals(controller.getEventsReceived().get(), 1);
        diskWriter.commit();
        Assert.assertEquals(controller.getEventsLost().get(), 0);
    }
}
