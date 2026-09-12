package vn.coincard.server.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the React build (same single-port behavior as the Node server). */
@Controller
public class SpaController {
  @Value("${coincard.client-dist:../client/dist}")
  private String clientDist;

  @GetMapping("/")
  public ResponseEntity<Resource> index() {
    Resource index = new FileSystemResource(clientDist + "/index.html");
    if (!index.exists()) {
      throw new IllegalStateException(
          "Không tìm thấy client build tại " + clientDist + ". Hãy chạy build client trước khi start server.");
    }
    return ResponseEntity.ok()
        .header("Cache-Control", "no-cache")
        .contentType(MediaType.TEXT_HTML)
        .body(index);
  }
}
