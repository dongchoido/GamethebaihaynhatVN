package vn.coincard.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import vn.coincard.server.application.ActiveGameRegistry;
import vn.coincard.server.game.EffectResolver;
import vn.coincard.server.game.GameCommandHandler;
import vn.coincard.server.game.GameEngine;
import vn.coincard.server.game.GameFactory;
import vn.coincard.server.game.GameSessionRegistry;
import vn.coincard.server.game.HeroPower;
import vn.coincard.server.game.HeroPowerRegistry;
import vn.coincard.server.game.RandomSource;

/** Domain and application bean wiring. DataSource lives in db.DataConfig. */
@Configuration
public class AppConfig {

  @Bean
  GameSessionRegistry gameSessionRegistry() {
    return new GameSessionRegistry();
  }

  @Bean
  ActiveGameRegistry activeGameRegistry() {
    return new ActiveGameRegistry();
  }

  @Bean
  GameEngine gameEngine(RandomSource randomSource) {
    return new GameEngine(new EffectResolver(randomSource), new HeroPowerRegistry(HeroPower.defaults()));
  }

  @Bean
  GameFactory gameFactory() {
    return new GameFactory();
  }

  @Bean
  GameCommandHandler gameCommandHandler(GameEngine engine, GameSessionRegistry sessions) {
    return new GameCommandHandler(engine, sessions);
  }
}
