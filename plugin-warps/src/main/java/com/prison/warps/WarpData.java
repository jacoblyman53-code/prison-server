package com.prison.warps;

/**
 * WarpData — an admin-defined warp point stored in the warps table.
 * permissionNode is null for public warps (anyone can use them).
 */
public record WarpData(
    long   id,
    String name,
    String world,
    double x, double y, double z,
    float  yaw, float pitch,
    String permissionNode,   // null = public warp
    String createdByUuid
) {}
