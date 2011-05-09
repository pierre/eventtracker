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

import com.ning.metrics.eventtracker.CollectorControllerModule.Type;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Properties;

@Test(groups = {"fast"})
public class TestConfigTypes
{
    Properties p = null;

    private final File tmpDir = new File(System.getProperty("java.io.tmpdir"), "collector");

    @BeforeMethod(alwaysRun = true)
    public void setUp()
    {
        p = new Properties();
        p.put("eventtracker.directory", tmpDir.getAbsolutePath());

        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new RuntimeException("Failed to create: " + tmpDir);
        }

        if (!tmpDir.isDirectory()) {
            throw new RuntimeException("Path points to something that's not a directory: " + tmpDir);
        }

        tmpDir.deleteOnExit();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown()
    {
        p = null;
    }

    public void testEmpty()
    {
        final EventTrackerConfig eventTrackerConfig = new ConfigurationObjectFactory(p).build(EventTrackerConfig.class);
        Assert.assertEquals(Type.valueOf(eventTrackerConfig.getType()), Type.COLLECTOR);
    }

    public void testCollector()
    {
        p.put("eventtracker.type", "COLLECTOR");

        final EventTrackerConfig eventTrackerConfig = new ConfigurationObjectFactory(p).build(EventTrackerConfig.class);
        Assert.assertEquals(Type.valueOf(eventTrackerConfig.getType()), Type.COLLECTOR);
    }

    public void testScribe()
    {
        p.put("eventtracker.type", "SCRIBE");
        final EventTrackerConfig eventTrackerConfig = new ConfigurationObjectFactory(p).build(EventTrackerConfig.class);
        Assert.assertEquals(Type.valueOf(eventTrackerConfig.getType()), Type.SCRIBE);
    }

    public void testNoLogging()
    {
        p.put("eventtracker.type", "NO_LOGGING");
        final EventTrackerConfig eventTrackerConfig = new ConfigurationObjectFactory(p).build(EventTrackerConfig.class);
        Assert.assertEquals(Type.valueOf(eventTrackerConfig.getType()), Type.NO_LOGGING);
    }
}
