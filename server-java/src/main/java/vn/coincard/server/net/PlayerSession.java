package vn.coincard.server.net;

/** Mutable connection identity owned by the transport adapter. */
public final class PlayerSession {
  private String sessionToken;

  public String sessionToken() {
    return sessionToken;
  }

  public void bindSessionToken(String token) {
    sessionToken = token;
  }
}
