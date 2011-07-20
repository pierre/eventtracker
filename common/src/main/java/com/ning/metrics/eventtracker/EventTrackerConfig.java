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
    CollectorControllerModule.Type getType();

    //------------------- Spooling -------------------//

    /**
     * Local buffering directory
     *
     * @return the directory path
     */
    @Config(value = "eventtracker.diskspool.path")
    @Default(value = ".diskspool")
    String getSpoolDirectoryName();

    /**
     * If false, events will not be periodically sent
     *
     * @return whether to send events buffered locally
     */
    @Config(value = "eventtracker.diskspool.enabled")
    @Default(value = "true")
    boolean isFlushEnabled();

    /**
     * Delay between flushes (in seconds)
     *
     * @return delay between flushes to the remote server
     */
    @Config(value = "eventtracker.diskspool.flush-interval-seconds")
    @Default(value = "60")
    int getFlushIntervalInSeconds();

    /**
     * Type of outputter to use when spooling: NONE, FLUSH, or SYNC
     *
     * @return the String representation of the SyncType
     */
    @Config(value = "eventtracker.diskspool.synctype")
    @Default(value = "NONE")
    String getSyncType();

    /**
     * Batch size to use for the outputter.
     * A flush or sync is triggered after this amount of events have been written.
     *
     * @return the batch size for writes
     */
    @Config(value = "eventtracker.diskspool.batch-size")
    @Default(value = "50")
    int getSyncBatchSize();

    /**
     * Maximum number of events in the file being written (_tmp directory).
     * <p/>
     * Maximum number of events per file in the temporary spooling area. Past this threshold,
     * buffered events are promoted to the final spool queue (where they are picked up by the final sender).
     *
     * @return the maximum number of events per file
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config(value = "eventtracker.diskspool.max-uncommitted-write-count")
    @Default(value = "10000")
    long getMaxUncommittedWriteCount();

    /**
     * Maximum age of events in the file being written (_tmp directory).
     * <p/>
     * Maxixum number of seconds before events are promoted from the temporary spooling area to the final spool queue.
     *
     * @return maxixmum age of events in seconds in the temporary spool queue
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config(value = "eventtracker.diskspool.max-uncommitted-period-seconds")
    @Default(value = "60")
    int getMaxUncommittedPeriodInSeconds();

    //------------------- HTTP Sender -------------------//

    /**
     * Collector host
     *
     * @return the hostname or IP of the collector host to use
     */
    @Config(value = "eventtracker.collector.host")
    @Default(value = "127.0.0.1")
    String getCollectorHost();

    /**
     * Collector port
     *
     * @return the collector port to use
     */
    @Config(value = "eventtracker.collector.port")
    @Default(value = "8080")
    int getCollectorPort();

    /**
     * Type of payload, valid only for HTTP protocol
     *
     * @return type of serialization to use (THRIFT, SMILE, JSON)
     */
    @Config(value = "eventtracker.event-type")
    @Default(value = "SMILE")
    EventType getEventType();

    /**
     * Max busy wait time for Http requests to finish when shutting down the eventtracker
     *
     * @return number of milliseconds to wait on shutdown, 8 seconds by default
     */
    @Config("eventtracker.collector.max-wait-time-millis")
    @Default(value = "8000")
    long getHttpMaxWaitTimeInMillis();

    //------------------- Scribe Sender -------------------//

    /**
     * Scribe host
     *
     * @return the hostname or IP of the Scribe host to use
     */
    @Config(value = "eventtracker.scribe.host")
    @Default(value = "127.0.0.1")
    String getScribeHost();

    /**
     * Scribe port
     *
     * @return the Scribe port to use
     */
    @Config(value = "eventtracker.scribe.port")
    @Default(value = "1463")
    int getScribePort();

    /**
     * Number of messages to send to Scribe before refreshing the connection
     * This is for load balancing purposes.
     *
     * @return the threshold before reconnecting to Scribe
     */
    @Config(value = "eventtracker.scribe.refresh_rate")
    @Default(value = "1000000")
    int getScribeRefreshRate();

    /**
     * Number of minutes allowed for the connection to be idle before re-opening it
     * We don't want to keep it open forever. For instance, SLB VIP may trigger a RST if idle more than a few minutes.
     *
     * @return the number of minutes before reconnecting to Scribe
     */
    @Config(value = "eventtracker.scribe.max-idle-minutes")
    @Default(value = "4")
    int getScribeMaxIdleTimeInMinutes();
}
