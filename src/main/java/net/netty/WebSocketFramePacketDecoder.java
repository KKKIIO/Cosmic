package net.netty;

import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import net.packet.ByteBufInPacket;

public class WebSocketFramePacketDecoder extends MessageToMessageDecoder<WebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
        var buf = msg.content();
        var bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        out.add(new ByteBufInPacket(Unpooled.wrappedBuffer(bytes)));
    }

}
