package com.guzula.pswitch.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Decodificador de frames com prefixo binário de comprimento de 2 bytes,
 * em ordem de rede (big-endian).
 * Referência: LengthFieldFramer em src/shared/transporters/length-field-framer.ts (guzula-switch).
 *
 * O prefixo representa o tamanho do payload, sem incluir os próprios 2 bytes.
 * Exemplo: 00 0A representa um payload de 10 bytes.
 */
public class LengthFieldFramerDecoder extends ByteToMessageDecoder {

    private static final int LENGTH_FIELD_SIZE = 2;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < LENGTH_FIELD_SIZE) {
            return;
        }

        in.markReaderIndex();
        int payloadLength = in.readUnsignedShort();

        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.readRetainedSlice(payloadLength));
    }
}
