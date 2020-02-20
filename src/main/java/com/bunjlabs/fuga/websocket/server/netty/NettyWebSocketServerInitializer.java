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

package com.bunjlabs.fuga.websocket.server.netty;

import com.bunjlabs.fuga.network.ConnectionRegistry;
import com.bunjlabs.fuga.websocket.server.WebSocketHandlerFactory;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

public class NettyWebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final WebSocketHandlerFactory handlerFactory;
    private final ConnectionRegistry connectionRegistry;

    NettyWebSocketServerInitializer(WebSocketHandlerFactory handlerFactory, ConnectionRegistry connectionRegistry) {
        this.handlerFactory = handlerFactory;
        this.connectionRegistry = connectionRegistry;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        var pipeline = ch.pipeline();

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerCompressionHandler());
        pipeline.addLast(new NettyWebSocketServerHandler(handlerFactory.createHandler()));

        connectionRegistry.pushChannel(ch);
    }
}