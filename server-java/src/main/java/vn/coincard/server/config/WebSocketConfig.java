package vn.coincard.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import vn.coincard.server.ws.GameWebSocketHandler;

/** Transport-only WebSocket configuration, kept separate from domain bean factories. */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public final class WebSocketConfig implements WebSocketConfigurer {
  private final GameWebSocketHandler gameWebSocketHandler;

  public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
    this.gameWebSocketHandler = gameWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(gameWebSocketHandler, "/ws").setAllowedOrigins("*");
  }
}
