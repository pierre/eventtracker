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

/**
 * Simple helper class that encapsulates details of how to keep track
 * of relinquishing of time-bound things: and specifically that of
 * closing of HTTP persistent connections.
 */
public class ExpirationTimer
{
    /**
     * How long does it take for a resource to expire?
     */
    private final long maxKeepAliveMsecs;

    /**
     * When is resource going to expire next time; either
     * undefined (0L), or timestamp of expiration.
     */
    private long expirationTime = 0L;

    public ExpirationTimer(final long max)
    {
        maxKeepAliveMsecs = max;
    }

    public final boolean isExpired()
    {
        return isExpired(System.currentTimeMillis());
    }

    public synchronized boolean isExpired(final long now)
    {
        if (expirationTime == 0L) { // just starting, set start time, no expiry
            expirationTime = now + maxKeepAliveMsecs;
            return false;
        }
        if (now >= maxKeepAliveMsecs) { // yup, expired
            expirationTime = 0L;
            return true;
        }
        // no, still valid
        return false;
    }
}
