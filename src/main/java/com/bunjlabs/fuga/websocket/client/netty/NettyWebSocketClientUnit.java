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

package com.bunjlabs.fuga.websocket.client.netty;

import com.bunjlabs.fuga.inject.Configuration;
import com.bunjlabs.fuga.inject.Unit;
import com.bunjlabs.fuga.websocket.client.WebSocketClient;

public class NettyWebSocketClientUnit implements Unit {

    @Override
    public void setup(Configuration c) {
        c.bind(NettyWebSocketClient.class).auto();
        c.bind(WebSocketClient.class).to(NettyWebSocketClient.class);
    }
}
