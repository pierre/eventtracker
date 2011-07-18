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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class EventBuilder
{
    private final StringBuilder builder = new StringBuilder(25);

    public EventBuilder(final String eventName)
    {
        builder.append(eventName);
    }

    public EventBuilder append(final boolean field)
    {
        builder.append(",b").append(field ? 1 : 0);
        return this;
    }

    public EventBuilder append(final byte field)
    {
        builder.append(",1").append(field);
        return this;
    }

    public EventBuilder append(final short field)
    {
        builder.append(",2").append(field);
        return this;
    }

    public EventBuilder append(final int field)
    {
        builder.append(",4").append(field);
        return this;
    }

    public EventBuilder append(final long field)
    {
        builder.append(",8").append(field);
        return this;
    }

    public EventBuilder append(final double field)
    {
        builder.append(",d").append(field);
        return this;
    }

    public EventBuilder append(final String field)
    {
        try {
            // URLEncoder doesn't really do RFC2396
            builder.append(",s").append(URLEncoder.encode(field.replace(' ', '+'), "UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            builder.append(",s").append(field);
        }
        return this;
    }

    // TODO annotations

    @Override
    public String toString()
    {
        return builder.toString();
    }
}
