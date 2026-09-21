package com.guzula.pswitch.transport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Servidor TCP inbound (recebe conexões de terminais POS / origem).
 * Referência: TcpRawServer em src/shared/transporters/tcp-raw.server.ts (guzula-switch).
 *
 * O framing de transporte é resolvido antes da entrega: os dois bytes binários de
 * tamanho são consumidos e apenas o payload completo chega ao consumidor.
 */
public class TcpRawServer {

    private final BiConsumer<byte[], Consumer<byte[]>> messageConsumer;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public TcpRawServer(BiConsumer<byte[], Consumer<byte[]>> messageConsumer) {
        this.messageConsumer = Objects.requireNonNull(messageConsumer);
    }

    public synchronized void start(String host, int port) {
        if (serverChannel != null) {
            return;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(
                                    new LengthFieldFramerDecoder(),
                                    new LengthFieldFramerEncoder(),
                                    new InboundMessageHandler(messageConsumer));
                        }
                    })
                    .childOption(ChannelOption.TCP_NODELAY, true);

            serverChannel = bootstrap.bind(host, port).sync().channel();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            stop();
            throw new IllegalStateException("Inicialização do servidor TCP interrompida", exception);
        } catch (RuntimeException exception) {
            stop();
            throw exception;
        }
    }

    public synchronized void stop() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
    }

    private static final class InboundMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final BiConsumer<byte[], Consumer<byte[]>> messageConsumer;

        private InboundMessageHandler(BiConsumer<byte[], Consumer<byte[]>> messageConsumer) {
            this.messageConsumer = messageConsumer;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, ByteBuf message) {
            byte[] bytes = new byte[message.readableBytes()];
            message.readBytes(bytes);
            messageConsumer.accept(
                    bytes,
                    response -> context.writeAndFlush(Unpooled.wrappedBuffer(response)));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            cause.printStackTrace();
            context.close();
        }
    }
}
