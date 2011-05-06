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
import com.ning.metrics.serialization.event.Granularity;
import com.ning.metrics.serialization.writer.CallbackHandler;
import org.eclipse.jetty.server.*;
import org.joda.time.DateTime;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.ServerSocket;

public class TestHttpSender
{
    private final static Logger logger = LoggerFactory.getLogger(TestHttpSender.class);

    private Server server;
    private Server errorServer;
    private HttpSender sender;
    private CallbackHandler failureCallbackHandler;
    private CallbackHandler successCallbackHandler;

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception
    {
        final int port = findFreePort();

        // Set up server
        server = new Server();
        Connector listener = new SelectChannelConnector();
        listener.setHost("127.0.0.1");
        listener.setPort(port);
        server.addConnector(listener);

        // Set up server that will return 404
        errorServer = new Server(){
            public void handle(HttpConnection connection) throws IOException, ServletException
            {
                final String target=connection.getRequest().getPathInfo();
                final Request request=connection.getRequest();
                final Response response=connection.getResponse();
                response.setStatus(404);

                handle(target, request, request, response);
            }
        };
        errorServer.addConnector(listener);

        System.setProperty("eventtracker.collector.port",Integer.toString(port));
        EventTrackerConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EventTrackerConfig.class);
        // Set up sender
        sender = new HttpSender(config);
        failureCallbackHandler = new CallbackHandler()
        {
            @Override
            public void onError(Throwable t, Event event)
            {
                logger.debug("Got error (good): " + t.getMessage());
            }

            @Override
            public void onSuccess(Event event)
            {
                Assert.fail("Got success when we were expecting failure. Uh oh.");
            }
        };
        successCallbackHandler = new CallbackHandler()
        {
            @Override
            public void onError(Throwable t, Event event)
            {
                Assert.fail("Got error (bad!): ", t);
            }

            @Override
            public void onSuccess(Event event)
            {
                logger.debug("Got success. Yay.");
            }
        };
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception
    {
        sender.close();
    }

    @Test
    public void testSend() throws Exception
    {
        // test send before server's initialized. hope for timeout failure
        logger.info("sending");
        sender.send(new DummyEvent(), failureCallbackHandler);
        Thread.sleep((long) 100); // 100 is long enough for it to timeout

        // initialize server and test again.
        server.start();
        logger.info("Started server");
        logger.info("sending");
        sender.send(new DummyEvent(), successCallbackHandler);
        Thread.sleep((long) 500);
        server.stop();
    }

    @Test
    public void test404() throws Exception
    {
        errorServer.start();
        logger.info("sending");
        sender.send(new DummyEvent(), failureCallbackHandler);
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

    public class DummyEvent implements Event
    {

        @Override
        public DateTime getEventDateTime()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return "dummy";
        }

        @Override
        public Granularity getGranularity()
        {
            return null;
        }

        @Override
        public String getVersion()
        {
            return null;
        }

        @Override
        public String getOutputDir(String prefix)
        {
            return null;
        }

        @Override
        public Object getData()
        {
            return null;
        }

        @Override
        public byte[] getSerializedEvent()
        {
            return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void writeExternal(ObjectOutput objectOutput) throws IOException
        {
            logger.debug("writing external");
        }

        @Override
        public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException
        {
            logger.debug("reading external");
        }
    }
}
