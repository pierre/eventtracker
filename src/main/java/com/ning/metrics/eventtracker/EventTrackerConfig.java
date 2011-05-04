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
import org.skife.config.Default;

public interface EventTrackerConfig
{

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
    @Default(value = "COLLECTOR")
    public String getType();
    // config-magic doesn't support enums :(

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
    @Default(value = "SMILE")
    public String getHttpEventEncodingType();

    //------------------- Spooling -------------------//

    /**
     * Maximum number of events per file in the temporary spooling area. Past this threshold,
     * buffered events are promoted to the final spool queue (where they are picked up by the final sender).
     *
     * @return the maximum number of events per file
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config(value = "eventtracker.diskspool.flush-event-queue-size")
    @Default(value = "10000")
    public long getFlushEventQueueSize();

    /**
     * Maxixum number of seconds before events are promoted from the temporary spooling area to the final spool queue.
     *
     * @return maxixmum age of events in seconds in the temporary spool queue
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config(value = "eventtracker.diskspool.refresh-delay-seconds")
    @Default(value = "60")
    public int getRefreshDelayInSeconds();

    /**
     * Directory for the Event Tracker to store events it can not send immediately
     *
     * @return the directory path
     */
    @Config(value = "eventtracker.diskspool.path")
    @Default(value = "/tmp/eventtracker/diskspool")
    public String getSpoolDirectoryName();

    /**
     * If false, events will not be periodically sent
     *
     * @return whether to send events buffered locally
     */
    @Config(value = "eventtracker.diskspool.enabled")
    @Default(value = "true")
    public boolean isFlushEnabled();

    /**
     * Delay between flushes (in seconds)
     *
     * @return delay between flushes to the remote server
     */
    @Config(value = "eventtracker.diskspool.flush-interval-seconds")
    @Default(value = "60")
    public int getFlushIntervalInSeconds();

    /**
     * Type of outputter to use when spooling: NONE, FLUSH, or SYNC
     *
     * @return the String representation of the SyncType
     */
    @Config(value = "eventtracker.diskspool.synctype")
    @Default(value = "NONE")
    public String getSyncType();

    /**
     * Batch size to use for the outputter.
     * A flush or sync is triggered after this amount of events have been written.
     *
     * @return the batch size for writes
     */
    @Config(value = "eventtracker.diskspool.batch-size")
    @Default(value = "50")
    public int getSyncBatchSize();

    @Config(value = "eventtracker.event-end-point.rate-window-size-minutes")
    @Default(value = "5")
    public int getRateWindowSizeMinutes();

    @Config(value = "eventtracker.collector.host")
    @Default(value = "127.0.0.1")
    public String getCollectorHost();

    @Config(value = "eventtracker.collector.port")
    @Default(value = "8080")
    public int getCollectorPort();

    //------------------- Scribe Sender -------------------//

    /**
     * Scribe port
     *
     * @return the Scribe port to use
     */
    @Config(value = "eventtracker.scribe.port")
    @Default(value = "1463")
    public int getScribePort();

    /**
     * Scribe host
     *
     * @return the hostname or IP of the Scribe host to use
     */
    @Config(value = "eventtracker.scribe.host")
    @Default(value = "127.0.0.1")
    public String getScribeHost();

    /**
     * Number of messages to send to Scribe before refreshing the connection
     * This is for load balancing purposes.
     *
     * @return the threshold before reconnecting to Scribe
     */
    @Config(value = "eventtracker.scribe.refresh_rate")
    @Default(value = "1000000")
    public int getScribeRefreshRate();

    /**
     * Number of minutes allowed for the connection to be idle before re-opening it
     * We don't want to keep it open forever. For instance, SLB VIP may trigger a RST if idle more than a few minutes.
     *
     * @return the number of minutes before reconnecting to Scribe
     */
    @Config(value = "eventtracker.scribe.max-idle-minutes")
    @Default(value = "4")
    public int getScribeMaxIdleTimeInMinutes();
}
