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

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An URI builder that is used internally if no other builder is supplied. This uses a fixed
 * base URI.
 */
final class SimpleUriBuilder implements CollectorUriBuilder
{

    private final String baseUri;
    private final Map<String, String> queryParams;

    public SimpleUriBuilder()
    {
        this("");
    }

    SimpleUriBuilder(final String baseUri)
    {
        this(baseUri, new LinkedHashMap<String, String>());
    }

    protected SimpleUriBuilder(final String baseUri, final Map<String, String> queryParams)
    {
        this.baseUri = baseUri;
        this.queryParams = queryParams;
    }

    @Override
    public final CollectorUriBuilder clone()
    {
        return new SimpleUriBuilder(baseUri, new LinkedHashMap<String, String>(queryParams));
    }

    @Override
    public final CollectorUriBuilder addQueryParam(final String key, final String value)
    {
        this.queryParams.put(key, value);
        return this;
    }

    @Override
    public final URI build()
    {
        final StringBuilder sb = new StringBuilder(baseUri);

        if (queryParams.size() > 0) {
            sb.append("?");
            for (Iterator<Map.Entry<String, String>> it = queryParams.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, String> entry = it.next();

                sb.append(entry.getKey()).append("=").append(entry.getValue());
                if (it.hasNext()) {
                    sb.append("&");
                }
            }
        }

        return URI.create(sb.toString());
    }
}



