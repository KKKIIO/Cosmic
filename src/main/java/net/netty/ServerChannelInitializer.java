package net.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.Client;
import config.YamlConfig;
import constants.net.ServerConstants;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;
import net.encryption.InitializationVector;
import net.packet.logging.InPacketLogger;
import net.packet.logging.OutPacketLogger;
import tools.PacketCreator;

public abstract class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final Logger log = LoggerFactory.getLogger(ServerChannelInitializer.class);
    private static final int IDLE_TIME_SECONDS = 30;
    private static final boolean LOG_PACKETS = YamlConfig.config.server.USE_DEBUG_SHOW_PACKET;
    private static final ChannelHandler sendPacketLogger = new OutPacketLogger();
    private static final ChannelHandler receivePacketLogger = new InPacketLogger();

    static final AtomicLong sessionId = new AtomicLong(7777);

    String getRemoteAddress(Channel channel) {
        String remoteAddress = "null";
        try {
            remoteAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
        } catch (NullPointerException npe) {
            log.warn("Unable to get remote address from netty Channel: {}", channel, npe);
        }

        return remoteAddress;
    }

    void initPipeline(SocketChannel socketChannel, Client client) {
        // final InitializationVector sendIv = InitializationVector.generateSend();
        // final InitializationVector recvIv = InitializationVector.generateReceive();
        // writeInitialUnencryptedHelloPacket(socketChannel, sendIv, recvIv);
        setUpHandlers(socketChannel.pipeline(), client);
    }

    private void writeInitialUnencryptedHelloPacket(SocketChannel socketChannel, InitializationVector sendIv,
            InitializationVector recvIv) {
        socketChannel.writeAndFlush(
                Unpooled.wrappedBuffer(PacketCreator.getHello(ServerConstants.VERSION, sendIv, recvIv).getBytes()));
    }

    private void setUpHandlers(ChannelPipeline pipeline,
            Client client) {
        pipeline.addLast("IdleStateHandler", new IdleStateHandler(0, 0, IDLE_TIME_SECONDS));
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerCompressionHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
                .websocketPath("/")
                .checkStartsWith(true)
                .allowExtensions(true)
                .subprotocols("binary")
                .build()));
        pipeline.addLast(new CombinedChannelDuplexHandler<>(
                new WebSocketFramePacketDecoder(), new WebSocketFramePacketEncoder()));
        // pipeline.addLast("PacketCodec", new PacketCodec(ClientCyphers.of(sendIv,
        // recvIv)));
        pipeline.addLast("Client", client);

        if (LOG_PACKETS) {
            pipeline.addBefore("Client", "SendPacketLogger", sendPacketLogger);
            pipeline.addBefore("Client", "ReceivePacketLogger", receivePacketLogger);
        }
    }
}
