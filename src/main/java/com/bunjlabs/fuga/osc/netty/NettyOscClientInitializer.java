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

package com.bunjlabs.fuga.osc.netty;

import com.bunjlabs.fuga.network.ConnectionRegistry;
import com.bunjlabs.fuga.osc.OscHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;

public class NettyOscClientInitializer extends ChannelInitializer<DatagramChannel> {
    private final ConnectionRegistry connectionRegistry;
    private final OscHandler oscHandler;

    NettyOscClientInitializer(ConnectionRegistry connectionRegistry, OscHandler oscHandler) {
        this.connectionRegistry = connectionRegistry;
        this.oscHandler = oscHandler;
    }

    @Override
    protected void initChannel(DatagramChannel ch) {
        ch.pipeline().addLast(new NettyOscCodec());
        ch.pipeline().addLast(new NettyOscClientHandler(oscHandler));

        connectionRegistry.pushChannel(ch);
    }
}
