package com.guzula.pswitch.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Decodificador de frames com prefixo de comprimento em BCD (2 dígitos por byte),
 * espelhando o framing usado pelo terminal POS / bandeira.
 * Referência: LengthFieldFramer em src/shared/transporters/length-field-framer.ts (guzula-switch).
 *
 * TODO: portar a lógica de leitura do prefixo BCD e extração do payload.
 */
public class LengthFieldFramerDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        throw new UnsupportedOperationException("TODO: portar decode de length-field-framer.ts");
    }
}
