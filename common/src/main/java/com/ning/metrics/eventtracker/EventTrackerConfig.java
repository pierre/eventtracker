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
import org.skife.config.TimeSpan;

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
    @Config("eventtracker.type")
    @Default("COLLECTOR")
    CollectorControllerModule.Type getType();

    //------------------- Spooling -------------------//

    /**
     * Local buffering directory
     *
     * @return the directory path
     */
    @Config("eventtracker.diskspool.path")
    @Default(".diskspool")
    String getSpoolDirectoryName();

    /**
     * If false, events will not be periodically sent
     *
     * @return whether to send events buffered locally
     */
    @Config("eventtracker.diskspool.enabled")
    @Default("true")
    boolean isFlushEnabled();

    /**
     * Delay between flushes (in seconds)
     *
     * @return delay between flushes to the remote server
     */
    @Config("eventtracker.diskspool.flush-interval-seconds")
    @Default("60")
    int getFlushIntervalInSeconds();

    /**
     * Type of outputter to use when spooling: NONE, FLUSH, or SYNC
     *
     * @return the String representation of the SyncType
     */
    @Config("eventtracker.diskspool.synctype")
    @Default("NONE")
    String getSyncType();

    /**
     * Batch size to use for the outputter.
     * A flush or sync is triggered after this amount of events have been written.
     *
     * @return the batch size for writes
     */
    @Config("eventtracker.diskspool.batch-size")
    @Default("50")
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
    @Config("eventtracker.diskspool.max-uncommitted-write-count")
    @Default("10000")
    long getMaxUncommittedWriteCount();

    /**
     * Maximum age of events in the file being written (_tmp directory).
     * <p/>
     * Maximum number of seconds before events are promoted from the temporary spooling area to the final spool queue.
     *
     * @return maxixmum age of events in seconds in the temporary spool queue
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config("eventtracker.diskspool.max-uncommitted-period-seconds")
    @Default("60")
    int getMaxUncommittedPeriodInSeconds();

    //------------------- HTTP Sender -------------------//

    /**
     * Collector host
     *
     * @return the hostname or IP of the collector host to use
     */
    @Config("eventtracker.collector.host")
    @Default("127.0.0.1")
    String getCollectorHost();

    /**
     * Collector port
     *
     * @return the collector port to use
     */
    @Config("eventtracker.collector.port")
    @Default("8080")
    int getCollectorPort();

    /**
     * Type of payload, valid only for HTTP protocol
     *
     * @return type of serialization to use (THRIFT, SMILE, JSON)
     */
    @Config("eventtracker.event-type")
    @Default("SMILE")
    EventType getEventType();

    /**
     * Max busy wait time for Http requests to finish when shutting down the eventtracker
     *
     * @return number of milliseconds to wait on shutdown, 8 seconds by default
     */
    @Config("eventtracker.collector.max-wait-time-millis")
    @Default("8000")
    long getHttpMaxWaitTimeInMillis();

    //------------------- Scribe Sender -------------------//

    /**
     * Scribe host
     *
     * @return the hostname or IP of the Scribe host to use
     */
    @Config("eventtracker.scribe.host")
    @Default("127.0.0.1")
    String getScribeHost();

    /**
     * Scribe port
     *
     * @return the Scribe port to use
     */
    @Config("eventtracker.scribe.port")
    @Default("1463")
    int getScribePort();

    /**
     * Number of messages to send to Scribe before refreshing the connection
     * This is for load balancing purposes.
     *
     * @return the threshold before reconnecting to Scribe
     */
    @Config("eventtracker.scribe.refresh_rate")
    @Default("1000000")
    int getScribeRefreshRate();

    /**
     * Number of minutes allowed for the connection to be idle before re-opening it
     * We don't want to keep it open forever. For instance, SLB VIP may trigger a RST if idle more than a few minutes.
     *
     * @return the number of minutes before reconnecting to Scribe
     */
    @Config("eventtracker.scribe.max-idle-minutes")
    @Default("4")
    int getScribeMaxIdleTimeInMinutes();

    /**
     * How long can we keep on using the same HTTP persistent connection?
     * Default is 2 minutes, to balance efficiency (longer) and load-balancing
     * (shorter) constraints.
     */
    @Config("eventtracker.http.connection.maxKeepAlive")
    @Default("120s")
    TimeSpan getHttpMaxKeepAlive();
}
