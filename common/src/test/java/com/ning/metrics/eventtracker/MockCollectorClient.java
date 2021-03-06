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

import com.ning.metrics.serialization.event.Event;

import java.util.concurrent.atomic.AtomicLong;

class MockCollectorClient
{
    private final AtomicLong successCount = new AtomicLong();
    private boolean fail = false;

    public MockCollectorClient()
    {
    }

    public void setFail(final boolean fail)
    {
        this.fail = fail;
    }

    public void clear()
    {
        successCount.set(0);
    }

    public boolean postThrift(final Event event)
    {
        if (fail) {
            return false;
        }

        successCount.incrementAndGet();

        return true;
    }

    public long getSuccessCount()
    {
        return successCount.get();
    }
}
