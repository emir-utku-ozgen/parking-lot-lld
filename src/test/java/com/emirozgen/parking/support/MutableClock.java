package com.emirozgen.parking.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Testlerde zamanı elle ileri sarmak için. */
public final class MutableClock extends Clock {

    private volatile Instant now;

    public MutableClock(Instant start) {
        this.now = start;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    public void setInstant(Instant instant) {
        now = instant;
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }
}
