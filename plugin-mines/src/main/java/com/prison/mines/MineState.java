package com.prison.mines;

import java.util.concurrent.atomic.AtomicLong;

/**
 * MineState — mutable runtime state for one mine.
 * Lives in memory only; resets to zero on server start.
 */
public class MineState {

    private final AtomicLong blocksBroken = new AtomicLong(0);
    private volatile boolean resetting    = false;
    private volatile long    lastResetMs  = System.currentTimeMillis();

    public long incrementBlocksBroken()  { return blocksBroken.incrementAndGet(); }
    public long getBlocksBroken()        { return blocksBroken.get(); }
    public void resetBlocksBroken()      { blocksBroken.set(0); }

    public boolean isResetting()         { return resetting; }
    public void setResetting(boolean v)  { resetting = v; }

    public long getLastResetMs()         { return lastResetMs; }
    public void markReset() {
        blocksBroken.set(0);
        lastResetMs = System.currentTimeMillis();
    }
}
