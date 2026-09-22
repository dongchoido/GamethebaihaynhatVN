package vn.coincard.server.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final ApplicationReadiness readiness;

  public HealthController(ApplicationReadiness readiness) {
    this.readiness = readiness;
  }

  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    if (!readiness.isReady()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new HealthResponse("starting", "coincard-server"));
    }
    return ResponseEntity.ok(new HealthResponse("ok", "coincard-server"));
  }

  public record HealthResponse(String status, String service) {}
}
