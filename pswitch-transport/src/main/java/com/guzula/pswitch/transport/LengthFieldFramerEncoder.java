package com.guzula.pswitch.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Codificador inverso do {@link LengthFieldFramerDecoder}: adiciona o prefixo
 * de comprimento binário de 2 bytes, em ordem de rede, antes do payload.
 * Referência: LengthFieldFramer em src/shared/transporters/length-field-framer.ts (guzula-switch).
 *
 * TODO: portar a lógica de escrita do prefixo binário.
 */
public class LengthFieldFramerEncoder extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        throw new UnsupportedOperationException("TODO: portar encode de length-field-framer.ts");
    }
}
