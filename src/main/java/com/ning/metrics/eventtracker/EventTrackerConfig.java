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

import com.ning.metrics.serialization.util.Managed;
import org.skife.config.Config;

public class EventTrackerConfig
{
    private String type = "COLLECTOR";
    private String httpEventEncodingType = "THRIFT";
    private long flushEventQueueSize = 10000;
    private int refreshDelayInSeconds = 60;
    private String spoolDirectoryName = "/tmp/eventtracker/diskspool";
    private boolean flushEnabled = true;
    private int flushIntervalInSeconds = 60;
    private String syncType = "NONE";
    private int syncBatchSize = 50;
    private int rateWindowSizeMinutes = 5;
    private int scribePort = 1463;
    private String scribeHost = "127.0.0.1";
    private int scribeRefreshRate = 1000000;
    private int scribeMaxIdleTimeInMinutes = 4;

    /**
     * Configure the type of the eventtracker. Valid values are:
     * <ul>
     * <li>SCRIBE: Thrift (RPC) protocol (Thrift payload)
     * <li>COLLECTOR: HTTP protocol (Thrift or Json/Smile payload)
     * <li>DUMMY: no-op
     * </ul>
     *
     * @return the type of eventtracker to use
     */
    @Config(value = "eventtracker.type")
    public String getType()
    {
        // config-magic doesn't support enums :(
        return type;
    }

    /**
     * Configure the type of the events passed "over the wire". Valid values are:
     * <ul>
     * <li>THRIFT: Thrift
     * <li>JSON: JSON
     * <li>SMILE: Smile
     * <li>DUMMY: no-op
     * </ul>
     *
     * @return the type of eventtracker to use
     */
    @Config(value = "eventtracker.http.eventEncoding")
    public String getHttpEventEncodingType()
    {
        return httpEventEncodingType;
    }

    @Managed(description = "Set the event encoding type")
    public void setHttpEventEncodingType(String httpEventEncodingType)
    {
        // test if this throws an error
        EventEncodingType.valueOf(httpEventEncodingType);
        this.httpEventEncodingType = httpEventEncodingType;
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
        return flushEventQueueSize;
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
        return refreshDelayInSeconds;
    }

    /**
     * Directory for the Event Tracker to store events it can not send immediately
     *
     * @return the directory path
     */
    @Config(value = "eventtracker.diskspool.path")
    public String getSpoolDirectoryName()
    {
        return spoolDirectoryName;
    }

    /**
     * If false, events will not be periodically sent
     *
     * @return whether to send events buffered locally
     */
    @Config(value = "eventtracker.diskspool.enabled")
    public boolean isFlushEnabled()
    {
        return flushEnabled;
    }

    /**
     * Delay between flushes (in seconds)
     *
     * @return delay between flushes to the remote server
     */
    @Config(value = "eventtracker.diskspool.flush-interval-seconds")
    public int getFlushIntervalInSeconds()
    {
        return flushIntervalInSeconds;
    }

    /**
     * Type of outputter to use when spooling: NONE, FLUSH, or SYNC
     *
     * @return the String representation of the SyncType
     */
    @Config(value = "eventtracker.diskspool.synctype")
    public String getSyncType()
    {
        return syncType;
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
        return syncBatchSize;
    }

    @Config(value = "eventtracker.event-end-point.rate-window-size-minutes")
    public int getRateWindowSizeMinutes()
    {
        return rateWindowSizeMinutes;
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
        return scribePort;
    }

    /**
     * Scribe host
     *
     * @return the hostname or IP of the Scribe host to use
     */
    @Config(value = "eventtracker.scribe.host")
    public String getScribeHost()
    {
        return scribeHost;
    }

    /**
     * Number of messages to send to Scribe before refreshing the connection
     * This is for load balancing purposes.
     *
     * @return the threshold before reconnecting to Scribe
     */
    @Config(value = "eventtracker.scribe.refresh_rate")
    public int getScribeRefreshRate()
    {
        return scribeRefreshRate; // 1 Million
    }

    /**
     * Number of minutes allowed for the connection to be idle before re-opening it
     * We don't want to keep it open forever. For instance, SLB VIP may trigger a RST if idle more than a few minutes.
     *
     * @return the number of minutes before reconnecting to Scribe
     */
    @Config(value = "eventtracker.scribe.max-idle-minutes")
    public int getScribeMaxIdleTimeInMinutes()
    {
        return scribeMaxIdleTimeInMinutes;
    }

    public void setFlushEnabled(boolean flushEnabled)
    {
        this.flushEnabled = flushEnabled;
    }

    public void setFlushEventQueueSize(long flushEventQueueSize)
    {
        this.flushEventQueueSize = flushEventQueueSize;
    }

    public void setFlushIntervalInSeconds(int flushIntervalInSeconds)
    {
        this.flushIntervalInSeconds = flushIntervalInSeconds;
    }

    public void setRateWindowSizeMinutes(int rateWindowSizeMinutes)
    {
        this.rateWindowSizeMinutes = rateWindowSizeMinutes;
    }

    public void setRefreshDelayInSeconds(int refreshDelayInSeconds)
    {
        this.refreshDelayInSeconds = refreshDelayInSeconds;
    }

    public void setScribeHost(String scribeHost)
    {
        this.scribeHost = scribeHost;
    }

    public void setScribePort(int scribePort)
    {
        this.scribePort = scribePort;
    }

    public void setScribeRefreshRate(int scribeRefreshRate)
    {
        this.scribeRefreshRate = scribeRefreshRate;
    }

    public void setSpoolDirectoryName(String spoolDirectoryName)
    {
        this.spoolDirectoryName = spoolDirectoryName;
    }

    public void setSyncBatchSize(int syncBatchSize)
    {
        this.syncBatchSize = syncBatchSize;
    }

    public void setSyncType(String syncType)
    {
        this.syncType = syncType;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setScribeMaxIdleTimeInMinutes(int scribeMaxIdleTimeInMinutes)
    {
        this.scribeMaxIdleTimeInMinutes = scribeMaxIdleTimeInMinutes;
    }
}
