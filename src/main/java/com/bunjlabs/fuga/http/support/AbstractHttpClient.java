/*
 * Copyright 2019 Bunjlabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bunjlabs.fuga.http.support;

import com.bunjlabs.fuga.http.HttpClient;
import com.bunjlabs.fuga.http.HttpHandler;
import com.bunjlabs.fuga.http.HttpRequestBuilder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.net.URL;

public abstract class AbstractHttpClient implements HttpClient {
    @Override
    public HttpRequestBuilder get(URL url) {
        return createBuilder(HttpMethod.GET, url);
    }

    @Override
    public HttpRequestBuilder head(URL url) {
        return createBuilder(HttpMethod.HEAD, url);
    }

    @Override
    public HttpRequestBuilder put(URL url) {
        return createBuilder(HttpMethod.PUT, url);
    }

    @Override
    public HttpRequestBuilder post(URL url) {
        return createBuilder(HttpMethod.POST, url);
    }

    @Override
    public HttpRequestBuilder delete(URL url) {
        return createBuilder(HttpMethod.DELETE, url);
    }

    @Override
    public HttpRequestBuilder options(URL url) {
        return createBuilder(HttpMethod.OPTIONS, url);
    }

    abstract void execute(ConnectionPoint connectionPoint, FullHttpRequest request, HttpHandler handler);

    private HttpRequestBuilder createBuilder(HttpMethod method, URL url) {
        return new NettyHttpRequestBuilder(method, url, this);
    }
}
