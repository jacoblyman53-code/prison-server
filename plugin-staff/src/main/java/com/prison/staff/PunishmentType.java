package com.prison.staff;

public enum PunishmentType {
    BAN,
    TEMPBAN,
    IPBAN,
    MUTE,
    TEMPMUTE,
    KICK;

    public boolean isBan() {
        return this == BAN || this == TEMPBAN || this == IPBAN;
    }

    public boolean isMute() {
        return this == MUTE || this == TEMPMUTE;
    }

    public boolean isTemporary() {
        return this == TEMPBAN || this == TEMPMUTE;
    }
}
