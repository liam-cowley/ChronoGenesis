package com.chronogenesis.time;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.player.Power;

import net.minecraft.world.level.ChunkPos;

/**
 * One chunk whose time runs at its own rate (Slow Time, Time Freeze; core-systems.md section 6).
 * <p>
 * The chunk keeps its own clock: every game tick it runs {@link #rate} of a tick, so it falls {@link #lag} ticks
 * behind the world. Each game tick, {@link #ticksNow} whole ticks are due inside it (0 while frozen, now and then 1
 * while slowed, several while catching up). When the power is let go, the chunk does not snap back: it speeds up
 * past the world's rate, then eases back down, landing exactly on the world's time (and its moon phase).
 */
public final class TimeZone {
    /** How fast the rate follows its target while held, per tick. */
    static final double RAMP = 0.08;
    /** Catching up: how fast the rate may change, per tick, and how far above the world's it may go. */
    static final double CATCH_UP_ACCEL = 0.02;
    static final double MAX_CATCH_UP = 8.0;
    /** Below this rate the chunk counts as stopped: its scheduled ticks wait instead of stretching. */
    public static final double STOPPED = 0.1;

    public final ChunkPos chunk;
    /** Who holds it (null on the client, and once it has been let go). */
    @Nullable
    public UUID owner;
    /** Which power holds it: Slow Time or Time Freeze (server only). */
    @Nullable
    public Power power;
    public double target;
    public double rate;
    public double lag;
    public boolean releasing;
    /** Fractional zone ticks carried over. */
    double carry;
    public int ticksNow = 1;

    public TimeZone(ChunkPos chunk, @Nullable UUID owner, double target) {
        this.chunk = chunk;
        this.owner = owner;
        this.target = target;
        this.rate = 1.0;
    }

    public boolean stopped() {
        return rate < STOPPED;
    }

    /** Advances one game tick. Returns false once a released zone has caught up with the world (remove it). */
    boolean advance() {
        if (releasing) {
            // Ease up to the fastest rate that still lets it slow back to 1 by the time the lag is gone.
            double desired = 1.0 + Math.min(MAX_CATCH_UP - 1.0, Math.sqrt(2.0 * CATCH_UP_ACCEL * Math.max(0.0, lag)));
            rate = rate < desired ? Math.min(desired, rate + CATCH_UP_ACCEL * 4) : Math.max(desired, rate - CATCH_UP_ACCEL * 4);
        } else if (rate != target) {
            rate = rate > target ? Math.max(target, rate - RAMP) : Math.min(target, rate + RAMP);
        }
        double step = rate;
        if (releasing && lag - (step - 1.0) <= 0.0) {
            // The last step lands exactly on the world's time.
            step = 1.0 + lag;
        }
        lag += 1.0 - step;
        carry += step;
        ticksNow = (int) Math.floor(carry);
        carry -= ticksNow;
        if (releasing && lag <= 1.0e-6) {
            lag = 0.0;
            return false;
        }
        return true;
    }

    /** The client copies the server's numbers each tick, and counts its own whole ticks. */
    void sync(double rate, double lag, boolean releasing) {
        this.rate = rate;
        this.lag = lag;
        this.releasing = releasing;
    }

    void clientAdvance() {
        carry += rate;
        ticksNow = (int) Math.floor(carry);
        carry -= ticksNow;
    }
}
