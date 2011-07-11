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

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;
import scribe.thrift.scribe;

import java.util.List;

class ScribeClientImpl implements ScribeClient
{
    private String host;
    private int port;
    private TFramedTransport transport;
    private scribe.Client client;

    private static final Logger logger = Logger.getLogger(ScribeClientImpl.class);

    public ScribeClientImpl(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public void openLogger() throws TTransportException
    {
        TSocket sock = new TSocket(host, port);
        transport = new TFramedTransport(sock);

        TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
        client = new scribe.Client(protocol, protocol);
        transport.open();
    }

    public ResultCode log(List<LogEntry> messages) throws TException
    {
        if (messages == null || messages.size() == 0) {
            return ResultCode.OK;
        }

        ResultCode rescode = client.Log(messages);
        switch (rescode) {
            case OK:
                logger.debug(String.format("%d Messages sent successfully.", messages.size()));
                break;
            case TRY_LATER:
                // Push the error back to the caller
                logger.warn("Try later");
                break;
            default:
                logger.warn(String.format("Unknown error: %d", rescode.getValue()));
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
