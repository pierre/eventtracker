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

import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.smile.SmileEnvelopeEventSerializer;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.NoCompressionCodec;
import com.ning.metrics.serialization.writer.SyncType;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.dataformat.smile.SmileParser;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class TestInvalidPayload
{
    protected static final SmileFactory smileFactory = new SmileFactory();

    static {
        // yes, full 'compression' by checking for repeating names, short string values:
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        // and for now let's not mandate header for input
        smileFactory.configure(SmileParser.Feature.REQUIRE_HEADER, false);
    }

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private String spoolPath;
    private File spoolDir;
    private File tmpDir;
    private File quarantineDir;
    private File lockDir;

    private class TestEventHandler implements EventHandler
    {
        @Override
        public void handle(File file, CallbackHandler handler)
        {
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        spoolPath = System.getProperty("java.io.tmpdir") + "/diskspooleventwriter-" + System.currentTimeMillis();
        spoolDir = new File(spoolPath);
        tmpDir = new File(spoolDir, "_tmp");
        quarantineDir = new File(spoolDir, "_quarantine");
        lockDir = new File(spoolDir, "_lock");
        prepareSpoolDirs();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception
    {
        prepareSpoolDirs();
    }

    @Test(groups = "fast")
    public void testWriteBadSmile() throws Exception
    {
        Assert.assertNull(listBinFiles(spoolDir));
        final DiskSpoolEventWriter writer = new DiskSpoolEventWriter(new TestEventHandler(), spoolPath, true, 1, executor,
            SyncType.NONE, 1, new NoCompressionCodec(), new SmileEnvelopeEventSerializer(false));

        try {
            writer.write(makeFailingEvent());
            Assert.fail();
        }
        catch (IOException e) {
            Assert.assertEquals(e.getCause().getClass(), JsonMappingException.class);
            // No file is currently being written (since the file was just moved aside)
            Assert.assertEquals(listBinFiles(tmpDir).length, 0);
            Assert.assertEquals(listBinFiles(spoolDir).length, 1);
        }

        writer.write(makeValidEvent());
        // One file being written (containing the valid event)
        Assert.assertEquals(listBinFiles(tmpDir).length, 1);
        // One file set aside (containing the invalid stream)
        Assert.assertEquals(listBinFiles(spoolDir).length, 1);

        // Make sure it still works
        writer.write(makeValidEvent());
        // One file being written (containing the valid event)
        Assert.assertEquals(listBinFiles(tmpDir).length, 1);
        // One file set aside (containing the invalid stream)
        Assert.assertEquals(listBinFiles(spoolDir).length, 1);
    }

    private void prepareSpoolDirs()
    {
        cleanDirectory(spoolDir);
        cleanDirectory(tmpDir);
        cleanDirectory(quarantineDir);
        cleanDirectory(lockDir);
    }

    private File[] listBinFiles(final File dir)
    {
        return dir.listFiles(new FileFilter()
        {
            @Override
            public boolean accept(final File pathname)
            {
                return pathname.getName().endsWith(".bin");
            }
        });
    }

    private void cleanDirectory(final File quarantineDir)
    {
        if (quarantineDir.exists()) {
            for (final File file : quarantineDir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    private SmileEnvelopeEvent makeFailingEvent() throws IOException
    {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("invlidUtf8", "\uD800");
        map.put("theNumberFive", 5);
        return new SmileEnvelopeEvent("sample", new DateTime(), map);
    }

    private SmileEnvelopeEvent makeValidEvent() throws IOException
    {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("theNumberFive", 5);
        return new SmileEnvelopeEvent("sample", new DateTime(), map);
    }
}
