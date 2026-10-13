package com.emirozgen.parking.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record Receipt(ParkingTicket ticket, Instant exitTime, BigDecimal fee) {

    public Receipt {
        Objects.requireNonNull(ticket, "Bilet boş olamaz!");
        Objects.requireNonNull(exitTime, "Çıkış zamanı boş olamaz!");
        Objects.requireNonNull(fee, "Ücret boş olamaz!");
    }

    public Duration parkedDuration() {
        return Duration.between(ticket.getEntryTime(), exitTime);
    }
}
