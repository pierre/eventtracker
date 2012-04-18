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

import com.ning.metrics.serialization.writer.CallbackHandler;

import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;

public class TestHttpSender
{
    private static final Logger logger = LoggerFactory.getLogger(TestHttpSender.class);
    private static final File eventsFile = new File(System.getProperty("java.io.tmpdir"), "TestHttpSender-" + System.currentTimeMillis());

    private Server server;
    private Server errorServer;
    private HttpSender sender;

    @SuppressWarnings("unused")
    private CallbackHandler failureCallbackHandler;
    @SuppressWarnings("unused")
    private CallbackHandler successCallbackHandler;

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception
    {
        final int port = findFreePort();

        // Set up server
        server = new Server(port);

        // Set up server that will return 404
        errorServer = new Server(port)
        {
            @Override
            public void handle(final AbstractHttpConnection connection) throws IOException, ServletException
            {
                final String target = connection.getRequest().getPathInfo();
                final Request request = connection.getRequest();
                final Response response = connection.getResponse();
                response.setStatus(404);

                handle(target, request, request, response);
            }
        };

        System.setProperty("eventtracker.collector.port", Integer.toString(port));
        final EventTrackerConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
        // Set up sender
        sender = new HttpSender(config.getCollectorHost(), config.getCollectorPort(), config.getEventType(),
                                config.getHttpMaxWaitTimeInMillis(), config.getHttpMaxKeepAlive().getMillis(), 10);
        failureCallbackHandler = new CallbackHandler()
        {
            @Override
            public void onError(final Throwable t, final File event)
            {
                logger.debug("Got error (good): " + t.getMessage());
            }

            @Override
            public void onSuccess(final File event)
            {
                Assert.fail("Got success when we were expecting failure. Uh oh.");
            }
        };
        successCallbackHandler = new CallbackHandler()
        {
            @Override
            public void onError(final Throwable t, final File event)
            {
                Assert.fail("Got error (bad!): ", t);
            }

            @Override
            public void onSuccess(final File event)
            {
                logger.debug("Got success. Yay.");
            }
        };

        // Populate the file
        Assert.assertTrue(eventsFile.createNewFile());
        final FileWriter writer = new FileWriter(eventsFile);
        writer.write("{ \"eventName\":\"Hello\", \"payload\": { \"dontcare\": \"World\" } }");
        writer.close();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception
    {
        sender.close();
    }

    @Test(groups = "slow")
    public void testSend() throws Exception
    {
        // test send before server's initialized. hope for timeout failure
        logger.info("sending");
        sender.send(eventsFile, failureCallbackHandler);
        Thread.sleep((long) 100); // 100 is long enough for it to timeout

        // initialize server and test again.
        server.start();
        logger.info("Started server");
        logger.info("sending");
        sender.send(eventsFile, successCallbackHandler);
        Thread.sleep((long) 500);
        server.stop();
    }

    @Test(groups = "slow")
    public void test404() throws Exception
    {
        errorServer.start();
        logger.info("sending");
        sender.send(eventsFile, failureCallbackHandler);
        Thread.sleep((long) 500);
        errorServer.stop();
    }

    private int findFreePort() throws IOException
    {
        ServerSocket socket = null;

        try {
            socket = new ServerSocket(0);

            return socket.getLocalPort();
        }
        finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
