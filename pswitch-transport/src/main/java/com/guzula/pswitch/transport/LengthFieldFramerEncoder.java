package com.guzula.pswitch.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Codificador inverso do {@link LengthFieldFramerDecoder}: adiciona o prefixo
 * de comprimento binário de 2 bytes, em ordem de rede, antes do payload.
 * Referência: LengthFieldFramer em src/shared/transporters/length-field-framer.ts (guzula-switch).
 *
 */
public class LengthFieldFramerEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final int MAX_PAYLOAD_SIZE = 0xFFFF;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int payloadLength = msg.readableBytes();
        if (payloadLength > MAX_PAYLOAD_SIZE) {
            throw new EncoderException("Payload excede o limite de 65535 bytes");
        }

        out.writeShort(payloadLength);
        out.writeBytes(msg, msg.readerIndex(), payloadLength);
    }
}
