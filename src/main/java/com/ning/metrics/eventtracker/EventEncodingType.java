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

/*
 * Used to specify how we encode events on the wire
 * when using HTTP POST
 */

public enum EventEncodingType
{
    THRIFT("ning/thrift"),
    JSON("application/json"), // MediaType.APPLICATION_JSON = "application/json"
    SMILE("application/json+smile");

    private String strVal;

    private EventEncodingType(String strVal)
    {
        this.strVal = strVal;
    }

    @Override
    public String toString()
    {
        return strVal;
    }
}
