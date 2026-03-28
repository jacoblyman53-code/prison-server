package com.prison.gangs;

import java.time.LocalDateTime;

/**
 * GangMember — one member entry in a gang.
 */
public record GangMember(long gangId, String playerUuid, GangRole role, LocalDateTime joinedAt) {}
