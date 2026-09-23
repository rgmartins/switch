package com.guzula.pswitch.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Gerencia conexões TCP outbound persistentes sem conhecer protocolos ou comandos de negócio. */
public final class TcpOutboundPool {

  private static final Logger LOGGER = Logger.getLogger(TcpOutboundPool.class.getName());
  private static final Duration DEFAULT_RECONNECT_DELAY = Duration.ofSeconds(5);

  private final EventLoopGroup workerGroup;
  private final Map<String, ManagedConnection> connections = new ConcurrentHashMap<>();
  private final AtomicBoolean stopping = new AtomicBoolean();

  public TcpOutboundPool() {
    this(new NioEventLoopGroup());
  }

  TcpOutboundPool(EventLoopGroup workerGroup) {
    this.workerGroup = Objects.requireNonNull(workerGroup);
  }

  public void connect(String connectionId, String host, int port, Duration reconnectDelay) {
    validate(connectionId, host, port, reconnectDelay);
    if (stopping.get()) {
      throw new IllegalStateException("O pool TCP outbound já foi encerrado");
    }

    ManagedConnection connection = new ManagedConnection(connectionId, host, port, reconnectDelay);
    if (connections.putIfAbsent(connectionId, connection) != null) {
      throw new IllegalArgumentException("Conexão TCP já configurada: " + connectionId);
    }
    connection.connect();
  }

  public void connect(String host, int port) {
    connect(host + ":" + port, host, port, DEFAULT_RECONNECT_DELAY);
  }

  public boolean isConnected(String connectionId) {
    ManagedConnection connection = connections.get(connectionId);
    return connection != null && connection.isConnected();
  }

  public synchronized void stop() {
    if (!stopping.compareAndSet(false, true)) {
      return;
    }

    connections.values().forEach(ManagedConnection::close);
    connections.clear();
    workerGroup.shutdownGracefully().syncUninterruptibly();
  }

  private void validate(String connectionId, String host, int port, Duration reconnectDelay) {
    if (connectionId == null || connectionId.isBlank()) {
      throw new IllegalArgumentException("Identificador da conexão TCP é obrigatório");
    }
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Host da conexão TCP é obrigatório");
    }
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Porta TCP inválida: " + port);
    }
    if (reconnectDelay == null || reconnectDelay.isNegative() || reconnectDelay.isZero()) {
      throw new IllegalArgumentException("Intervalo de reconexão deve ser maior que zero");
    }
  }

  private final class ManagedConnection {

    private final String connectionId;
    private final String host;
    private final int port;
    private final Duration reconnectDelay;
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private volatile Channel channel;

    private ManagedConnection(String connectionId, String host, int port, Duration reconnectDelay) {
      this.connectionId = connectionId;
      this.host = host;
      this.port = port;
      this.reconnectDelay = reconnectDelay;
    }

    private void connect() {
      if (stopping.get() || isConnected() || !connecting.compareAndSet(false, true)) {
        return;
      }

      LOGGER.info(() -> "Conectando " + connectionId + " em " + host + ":" + port);
      Bootstrap bootstrap =
          new Bootstrap()
              .group(workerGroup)
              .channel(NioSocketChannel.class)
              .option(ChannelOption.TCP_NODELAY, true)
              .handler(
                  new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                      socketChannel
                          .pipeline()
                          .addLast(
                              new LengthFieldFramerDecoder(),
                              new LengthFieldFramerEncoder(),
                              new ConnectionLifecycleHandler(ManagedConnection.this));
                    }
                  });

      ChannelFuture connectFuture = bootstrap.connect(host, port);
      connectFuture.addListener(
          future -> {
            connecting.set(false);
            if (future.isSuccess()) {
              channel = connectFuture.channel();
              LOGGER.info(() -> "Conexão " + connectionId + " estabelecida");
            } else {
              LOGGER.log(
                  Level.WARNING,
                  "Falha ao conectar " + connectionId + ": " + future.cause().getMessage());
              scheduleReconnect();
            }
          });
    }

    private void scheduleReconnect() {
      if (stopping.get() || !reconnectScheduled.compareAndSet(false, true)) {
        return;
      }

      workerGroup
          .next()
          .schedule(
              () -> {
                reconnectScheduled.set(false);
                connect();
              },
              reconnectDelay.toMillis(),
              TimeUnit.MILLISECONDS);
    }

    private boolean isConnected() {
      Channel currentChannel = channel;
      return currentChannel != null && currentChannel.isActive();
    }

    private void disconnected() {
      channel = null;
      if (!stopping.get()) {
        LOGGER.warning(() -> "Conexão " + connectionId + " encerrada; reconectando");
        scheduleReconnect();
      }
    }

    private void close() {
      Channel currentChannel = channel;
      channel = null;
      if (currentChannel != null) {
        currentChannel.close().syncUninterruptibly();
      }
    }
  }

  private static final class ConnectionLifecycleHandler
      extends SimpleChannelInboundHandler<ByteBuf> {

    private final ManagedConnection connection;

    private ConnectionLifecycleHandler(ManagedConnection connection) {
      this.connection = connection;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, ByteBuf message) {
      // Nenhum protocolo externo é tratado pela camada de transporte.
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
      connection.disconnected();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
      LOGGER.log(Level.WARNING, "Erro na conexão " + connection.connectionId, cause);
      context.close();
    }
  }
}
