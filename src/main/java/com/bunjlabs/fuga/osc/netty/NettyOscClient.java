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

import com.bunjlabs.fuga.inject.Inject;
import com.bunjlabs.fuga.network.ConnectionRegistry;
import com.bunjlabs.fuga.network.EventLoopGroupManager;
import com.bunjlabs.fuga.osc.OscClient;
import com.bunjlabs.fuga.osc.OscHandler;
import com.bunjlabs.fuga.osc.types.OscPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;

import java.net.SocketAddress;

public class NettyOscClient implements OscClient {

    private final Logger logger;
    private final EventLoopGroupManager loopGroupManager;
    private final ConnectionRegistry connectionRegistry;
    private Channel channel;

    @Inject
    public NettyOscClient(Logger logger, EventLoopGroupManager loopGroupManager, ConnectionRegistry connectionRegistry) {
        this.logger = logger;
        this.loopGroupManager = loopGroupManager;
        this.connectionRegistry = connectionRegistry;
    }

    @Override
    public void start(SocketAddress socketAddress, OscHandler handler) {
        var workerGroup = loopGroupManager.getWorkerEventLoopGroup();

        var b = new Bootstrap();
        b.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .handler(new NettyOscClientInitializer(connectionRegistry, handler));

        try {
            this.channel = b.connect(socketAddress).sync().channel();
        } catch (InterruptedException e) {
            logger.error("Unable to start netty websocket server", e);
        }
    }

    @Override
    public void send(OscPacket packet) {
        if (channel != null) {
            channel.writeAndFlush(packet);
        }
    }
}
