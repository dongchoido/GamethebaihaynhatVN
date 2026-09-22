package vn.coincard.server.net;

/** Closed set of serializable server-to-client payloads. */
public sealed interface OutboundEvent permits GameEvents.RoomCreated, GameEvents.PlayerJoined,
    GameEvents.RoomReady, GameEvents.LoadoutAccepted, GameEvents.GameStarted,
    GameEvents.GameStateUpdated, GameEvents.GameOver, GameEvents.PlayerDisconnected,
    GameEvents.ActionRejected {}
