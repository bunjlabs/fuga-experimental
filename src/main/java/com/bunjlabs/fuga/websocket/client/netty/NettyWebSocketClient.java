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

import com.bunjlabs.fuga.inject.Inject;
import com.bunjlabs.fuga.network.ConnectionRegistry;
import com.bunjlabs.fuga.network.EventLoopGroupManager;
import com.bunjlabs.fuga.websocket.WebSocketHandler;
import com.bunjlabs.fuga.websocket.client.WebSocketClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;

import javax.net.ssl.SSLException;
import java.net.URI;

public class NettyWebSocketClient implements WebSocketClient {
    private final Logger logger;
    private final EventLoopGroupManager loopGroupManager;
    private final ConnectionRegistry connectionRegistry;

    @Inject
    public NettyWebSocketClient(Logger logger, EventLoopGroupManager loopGroupManager, ConnectionRegistry connectionRegistry) {
        this.logger = logger;
        this.loopGroupManager = loopGroupManager;
        this.connectionRegistry = connectionRegistry;
    }


    @Override
    public void connect(URI uri, WebSocketHandler handler, int timeout) {
        var workerGroup = loopGroupManager.getWorkerEventLoopGroup();

        var scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            logger.error("Only WS(S) is supported.");
            return;
        }

        SslContext sslContext = null;
        if ("wss".equalsIgnoreCase(scheme)) {
            try {
                sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } catch (SSLException e) {
                logger.warn("Exception while enabling SSL context", e);
            }
        }

        var b = new Bootstrap();
        b.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .handler(new NettyWebSocketClientInitializer(uri, sslContext, connectionRegistry, handler));

        b.connect(uri.getHost(), uri.getPort()).addListener(future -> {
            if (!future.isSuccess()) {
                handler.onClose(null, -1, "Unable to connect", false);
            }
        });
    }
}
