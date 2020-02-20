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

import com.bunjlabs.fuga.websocket.WebSocketHandler;
import com.bunjlabs.fuga.websocket.netty.NettyWebSocket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.net.URI;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyWebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private static final String WEBSOCKET_PATH = "/";
    private final WebSocketHandler handler;
    private WebSocketServerHandshaker handshaker;

    private NettyWebSocket webSocket;

    NettyWebSocketServerHandler(WebSocketHandler handler) {
        this.handler = handler;
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse res) {
        if (res.status().code() != 200) {
            var buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            res.headers().add(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
        }

        var f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(HttpRequest req) {
        var location = req.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
        return "ws://" + location;
    }

    private static boolean isKeepAlive(HttpMessage message) {
        var connection = message.headers().get(HttpHeaderNames.CONNECTION);
        if (connection != null && AsciiString.contentEqualsIgnoreCase(HttpHeaderValues.CLOSE, connection)) {
            return false;
        }

        if (message.protocolVersion().isKeepAliveDefault()) {
            return !AsciiString.contentEqualsIgnoreCase(HttpHeaderValues.CLOSE, connection);
        } else {
            return AsciiString.contentEqualsIgnoreCase(HttpHeaderValues.KEEP_ALIVE, connection);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        if (req.method() != HttpMethod.GET
                || !"Upgrade".equalsIgnoreCase(req.headers().get(HttpHeaderNames.CONNECTION))
                || !"WebSocket".equalsIgnoreCase(req.headers().get(HttpHeaderNames.UPGRADE))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        if (!WEBSOCKET_PATH.equals(req.uri())) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            return;
        }

        var wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req),
                null,
                true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
            handler.onOpen(webSocket = new NettyWebSocket(ctx,
                    URI.create(String.format("ws://%s:%d/", socketAddress.getHostName(), socketAddress.getPort()))));
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            var closeFrame = (CloseWebSocketFrame) frame;
            handler.onClose(webSocket, closeFrame.statusCode(), closeFrame.reasonText(), true);
            handshaker.close(ctx.channel(), closeFrame.retain());
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        } else if (frame instanceof PongWebSocketFrame) {

        } else if (frame instanceof TextWebSocketFrame) {
            handler.onMessage(webSocket, ((TextWebSocketFrame) frame).text());
        } else {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handler.onError(webSocket, cause);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handler.onStart();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {

    }
}