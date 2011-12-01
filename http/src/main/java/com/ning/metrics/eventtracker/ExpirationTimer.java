package com.ning.metrics.eventtracker;

/**
 * Simple helper class that encapsulates details of how to keep track
 * of relinquishing of time-bound things: and specifically that of
 * closing of HTTP persistent connections.
 */
public class ExpirationTimer
{
    /**
     * How long does it take for a resource to expire?
     */
    private final long maxKeepAliveMsecs;

    /**
     * When is resource going to expire next time; either
     * undefined (0L), or timestamp of expiration.
     */
    private long expirationTime = 0L;
    
    public ExpirationTimer(long max) {
        maxKeepAliveMsecs = max;
    }

    public final boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }
    
    public synchronized boolean isExpired(long now)
    {
        if (expirationTime == 0L) { // just starting, set start time, no expiry
            expirationTime = now  + maxKeepAliveMsecs;
            return false;
        }
        if (now >= maxKeepAliveMsecs) { // yup, expired
            expirationTime = 0L;
            return true;
        }
        // no, still valid
        return false;
    }
}
