package com.prison.coinflip;

import java.util.UUID;

/**
 * CoinflipTicket — immutable snapshot of a single coinflip challenge.
 *
 * Mutable state (state field) is tracked separately in CoinflipManager.
 */
public class CoinflipTicket {

    private final int    id;
    private final UUID   creatorUuid;
    private final String creatorName;
    private final long   amount;
    private final long   createdAt;

    private volatile CoinflipState state;
    private volatile UUID   acceptorUuid;
    private volatile String acceptorName;
    private volatile UUID   winnerUuid;
    private volatile long   resolvedAt;

    public CoinflipTicket(int id, UUID creatorUuid, String creatorName, long amount) {
        this.id          = id;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.amount      = amount;
        this.state       = CoinflipState.OPEN;
        this.createdAt   = System.currentTimeMillis();
    }

    // ----------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------

    public int    getId()          { return id; }
    public UUID   getCreatorUuid() { return creatorUuid; }
    public String getCreatorName() { return creatorName; }
    public long   getAmount()      { return amount; }
    public long   getCreatedAt()   { return createdAt; }

    public CoinflipState getState()       { return state; }
    public UUID          getAcceptorUuid() { return acceptorUuid; }
    public String        getAcceptorName() { return acceptorName; }
    public UUID          getWinnerUuid()   { return winnerUuid; }
    public long          getResolvedAt()   { return resolvedAt; }

    // ----------------------------------------------------------------
    // State transitions (called by CoinflipManager only)
    // ----------------------------------------------------------------

    void accept(UUID acceptorUuid, String acceptorName) {
        this.acceptorUuid = acceptorUuid;
        this.acceptorName = acceptorName;
        this.state        = CoinflipState.ACCEPTED_PENDING;
    }

    void resolve(UUID winnerUuid) {
        this.winnerUuid = winnerUuid;
        this.resolvedAt = System.currentTimeMillis();
        this.state      = CoinflipState.RESOLVED;
    }

    void cancel() {
        this.state = CoinflipState.CANCELLED;
    }
}
