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

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestEventBuilder
{
    @Test(groups = "fast")
    public void testToString() throws Exception
    {
        final EventBuilder builder = new EventBuilder("EventName");
        builder.append(1L);
        builder.append(2L);
        builder.append(127L);
        builder.append(-128L);
        builder.append(32767);
        builder.append(-32768L);
        builder.append(2147483647L);
        builder.append(-2147483648L);
        builder.append(10000000000L);
        builder.append(-20000000000L);

        Assert.assertEquals(builder.toString(), "EventName,81,82,8127,8-128,432767,8-32768,82147483647,8-2147483648,810000000000,8-20000000000");
    }

    @Test(groups = "fast")
    public void testEncoding() throws Exception
    {
        final EventBuilder builder = new EventBuilder("SomeEvent");
        builder.append("text/plain; charset=iso-8859-1");
        Assert.assertEquals(builder.toString(), "SomeEvent,stext%2Fplain%3B%2Bcharset%3Diso-8859-1");
    }
}
