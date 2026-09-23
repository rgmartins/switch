package com.guzula.pswitch.app.config;

import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.transport.InboundMessageConsumer;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Resolve o handler configurado e entrega a ele os payloads sem acoplar o transporte ao Spring. */
@Component
public class TcpMessageDispatcher {

  private static final Logger LOGGER = Logger.getLogger(TcpMessageDispatcher.class.getName());

  private final Map<String, InboundPayloadHandler> handlers;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public TcpMessageDispatcher(List<InboundPayloadHandler> handlers) {
    this.handlers =
        handlers.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    handler -> normalize(handler.handlerName()), Function.identity()));
  }

  public InboundMessageConsumer route(String handlerName) {
    InboundPayloadHandler handler = handlers.get(normalize(handlerName));
    if (handler == null) {
      throw new IllegalStateException("Nenhum handler TCP encontrado para " + handlerName);
    }

    return (connectionId, payload) ->
        executor.execute(
            () -> {
              try {
                handler.handleInbound(connectionId, payload);
              } catch (RuntimeException exception) {
                LOGGER.log(
                    Level.SEVERE,
                    "Falha no handler TCP " + handlerName + " para a conexão " + connectionId,
                    exception);
              }
            });
  }

  @PreDestroy
  public void stop() {
    executor.shutdownNow();
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Nome do handler TCP é obrigatório");
    }
    return value.toUpperCase(Locale.ROOT);
  }
}
