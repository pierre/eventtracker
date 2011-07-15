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

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;
import scribe.thrift.scribe;

import java.util.List;

class ScribeClientImpl implements ScribeClient
{
    private final String host;
    private final int port;
    private TFramedTransport transport;
    private scribe.Client client;

    private static final Logger logger = LoggerFactory.getLogger(ScribeClientImpl.class);

    public ScribeClientImpl(final String host, final int port)
    {
        this.host = host;
        this.port = port;
    }

    public void openLogger() throws TTransportException
    {
        final TSocket sock = new TSocket(host, port);
        transport = new TFramedTransport(sock);

        final TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
        client = new scribe.Client(protocol, protocol);
        transport.open();
    }

    public ResultCode log(final List<LogEntry> messages) throws TException
    {
        if (messages == null || messages.size() == 0) {
            return ResultCode.OK;
        }

        final ResultCode rescode = client.Log(messages);
        switch (rescode) {
            case OK:
                logger.debug("{} Messages sent successfully", messages.size());
                break;
            case TRY_LATER:
                // Push the error back to the caller
                logger.warn("Try later");
                break;
            default:
                logger.warn("Unknown error: {}", rescode.getValue());
        }
        return rescode;
    }

    public void closeLogger()
    {
        if (transport != null) {
            transport.close();
        }
    }

}
