package vn.coincard.server.room;

import java.util.ArrayList;
import java.util.List;
import vn.coincard.server.game.HeroClass;

/** Mutable member record (internal). Snapshots are copied on read. */
public final class RoomPlayer {
  private final String playerId;
  private final String name;
  private final String sessionToken;
  private String socketId;
  private HeroClass heroClass;
  private List<String> cardSlugs;
  private boolean ready;

  public RoomPlayer(String playerId, String name, String sessionToken,
      String socketId, HeroClass heroClass, boolean ready) {
    this(playerId, name, sessionToken, socketId, heroClass, List.of(), ready);
  }

  public RoomPlayer(String playerId, String name, String sessionToken,
      String socketId, HeroClass heroClass, List<String> cardSlugs, boolean ready) {
    this.playerId = playerId;
    this.name = name;
    this.sessionToken = sessionToken;
    this.socketId = socketId;
    this.heroClass = heroClass;
    this.cardSlugs = new ArrayList<>(cardSlugs);
    this.ready = ready;
  }

  public RoomPlayer copy() {
    return new RoomPlayer(playerId, name, sessionToken, socketId, heroClass, cardSlugs, ready);
  }

  public String playerId() { return playerId; }
  public String name() { return name; }
  public String sessionToken() { return sessionToken; }
  public String socketId() { return socketId; }
  public HeroClass heroClass() { return heroClass; }
  public List<String> cardSlugs() { return List.copyOf(cardSlugs); }
  public boolean ready() { return ready; }
  public boolean deckReady() { return ready && cardSlugs.size() == 30; }

  void selectLoadout(HeroClass selectedHeroClass, List<String> selectedCardSlugs) {
    heroClass = selectedHeroClass;
    cardSlugs = new ArrayList<>(selectedCardSlugs);
    ready = true;
  }

  void clearReady() { ready = false; }
  void bindSocket(String newSocketId) { socketId = newSocketId; }
  void clearSocket() { socketId = null; }
}
