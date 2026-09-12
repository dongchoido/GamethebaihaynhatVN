package vn.coincard.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import vn.coincard.server.ws.GameWebSocketHandler;

/** Wiring: /ws endpoint. DataSource lives in db.DataConfig (avoids a bean cycle). */
@Configuration
@EnableWebSocket
public class AppConfig implements WebSocketConfigurer {
  private final GameWebSocketHandler gameWebSocketHandler;

  public AppConfig(GameWebSocketHandler gameWebSocketHandler) {
    this.gameWebSocketHandler = gameWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(gameWebSocketHandler, "/ws").setAllowedOrigins("*");
  }
}
