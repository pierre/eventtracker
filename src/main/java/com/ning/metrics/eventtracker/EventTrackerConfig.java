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

import org.skife.config.Config;

abstract class EventTrackerConfig
{
    /**
     * Configure the type of the eventtracker. Valid values are SCRIBE, COLLECTOR, DUMMY
     *
     * @return the type of eventtracker to use
     */
    @Config(value = "eventtracker.type")
    public String getType()
    {
        // config-magic doesn't support enums :(
        return "COLLECTOR";
    }

    //------------------- Spooling -------------------//

    /**
     * Maximum number of events per file in the temporary spooling area. Past this threshold,
     * buffered events are promoted to the final spool queue (where they are picked up by the final sender).
     *
     * @return the maximum number of events per file
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config(value = "eventtracker.diskspool.flush-event-queue-size")
    public long getFlushEventQueueSize()
    {
        return 10000;
    }

    /**
     * Maxixum number of seconds before events are promoted from the temporary spooling area to the final spool queue.
     *
     * @return maxixmum age of events in seconds in the temporary spool queue
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config(value = "eventtracker.diskspool.refresh-delay-seconds")
    public int getRefreshDelayInSeconds()
    {
        return 60;
    }

    /**
     * Directory for the Event Tracker to store events it can not send immediately
     *
     * @return the directory path
     */
    @Config(value = "eventtracker.diskspool.path")
    public String getSpoolDirectoryName()
    {
        return "/tmp/eventtracker/diskspool";
    }

    /**
     * If false, events will not be periodically sent
     *
     * @return whether to send events buffered locally
     */
    @Config(value = "eventtracker.diskspool.enabled")
    public boolean isFlushEnabled()
    {
        return true;
    }

    /**
     * Delay between flushes (in seconds)
     *
     * @return delay between flushes to the remote server
     */
    @Config(value = "eventtracker.diskspool.flush-interval-seconds")
    public int getFlushIntervalInSeconds()
    {
        return 60;
    }

    /**
     * Type of outputter to use when spooling: NONE, FLUSH, or SYNC
     *
     * @return the String representation of the SyncType
     */
    @Config(value = "eventtracker.diskspool.synctype")
    public String getSyncType()
    {
        return "NONE";
    }

    /**
     * Batch size to use for the outputter.
     * A flush or sync is triggered after this amount of events have been written.
     *
     * @return the batch size for writes
     */
    @Config(value = "eventtracker.diskspool.batch-size")
    public int getSyncBatchSize()
    {
        return 50;
    }

    @Config(value = "eventtracker.event-end-point.rate-window-size-minutes")
    public int getRateWindowSizeMinutes()
    {
        return 5;
    }

    //------------------- Scribe Sender -------------------//

    /**
     * Scribe port
     *
     * @return the Scribe port to use
     */
    @Config(value = "eventtracker.scribe.port")
    public int getScribePort()
    {
        return 1463;
    }

    /**
     * Scribe host
     *
     * @return the hostname or IP of the Scribe host to use
     */
    @Config(value = "eventtracker.scribe.host")
    public String getScribeHost()
    {
        return "127.0.0.1";
    }

    /**
     * Number of messages to send to Scribe before refreshing the connection
     *
     * @return the threshold before reconnecting to Scribe
     */
    @Config(value = "eventtracker.scribe.refresh_rate")
    public int getMessagesToSendBeforeReconnectingToScribe()
    {
        return 1000000; // 1 Million
    }
}
