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

import com.bunjlabs.fuga.websocket.WebSocketHandler;
import com.bunjlabs.fuga.websocket.netty.NettyWebSocket;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.net.URI;

public class NettyWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final URI uri;
    private final WebSocketClientHandshaker handshaker;
    private final WebSocketHandler handler;
    private ChannelPromise handshakeFuture;

    private NettyWebSocket webSocket;

    public NettyWebSocketClientHandler(URI uri, WebSocketClientHandshaker handshaker, WebSocketHandler handler) {
        this.uri = uri;
        this.handshaker = handshaker;
        this.handler = handler;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
        handshakeFuture = ctx.newPromise();

        this.handler.onStart();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        handler.onClose(webSocket, -1, "no reason", true);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        var ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            handshakeFuture.setSuccess();

            handler.onOpen(webSocket = new NettyWebSocket(ctx, uri));
            return;
        }

        if (msg instanceof FullHttpResponse) {
            var response = (FullHttpResponse) msg;
            throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content="
                    + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        var frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            var textFrame = (TextWebSocketFrame) frame;
            handler.onMessage(webSocket, textFrame.text());
        } else if (frame instanceof BinaryWebSocketFrame) {
            var binaryFrame = (BinaryWebSocketFrame) frame;
            handler.onMessage(webSocket, binaryFrame.content().nioBuffer());
        } else if (frame instanceof PongWebSocketFrame) {
        } else if (frame instanceof CloseWebSocketFrame) {
            var closeFrame = (CloseWebSocketFrame) frame;
            handler.onClose(webSocket, closeFrame.statusCode(), closeFrame.reasonText(), true);
            ch.close();
        }

    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }

        handler.onError(webSocket, cause);
        ctx.close();
    }
}
