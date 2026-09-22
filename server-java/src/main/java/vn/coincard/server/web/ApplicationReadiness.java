package vn.coincard.server.web;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** Readiness flips only after the SQLite schema and catalog have been seeded. */
@Component
public final class ApplicationReadiness {
  private final AtomicBoolean ready = new AtomicBoolean();

  public boolean isReady() {
    return ready.get();
  }

  public void markReady() {
    ready.set(true);
  }
}
