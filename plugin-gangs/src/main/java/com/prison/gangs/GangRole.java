package com.prison.gangs;

/**
 * GangRole — the three roles a gang member can hold.
 */
public enum GangRole {
    LEADER, OFFICER, MEMBER;

    public boolean canInvite()   { return this != MEMBER; }
    public boolean canKick()     { return this != MEMBER; }
    public boolean canWithdraw() { return this != MEMBER; }
    public boolean canDisband()  { return this == LEADER; }
    public boolean canPromote()  { return this == LEADER; }
    public boolean canDemote()   { return this == LEADER; }
    public boolean canTransfer() { return this == LEADER; }

    /** MiniMessage string for display, no trailing space. */
    public String display() {
        return switch (this) {
            case LEADER  -> "<gold>[Leader]</gold>";
            case OFFICER -> "<yellow>[Officer]</yellow>";
            case MEMBER  -> "<gray>[Member]</gray>";
        };
    }
}
