package com.emirozgen.parking.strategy.fee;

import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Başlayan her saat tam saat sayılır; ücretsiz süre içinde çıkan araçtan ücret alınmaz.
 */
public class HourlyFeeStrategy implements FeeStrategy {

    private static final long SECONDS_PER_HOUR = 3600;

    private final Map<VehicleType, BigDecimal> hourlyRates;
    private final Duration gracePeriod;

    public HourlyFeeStrategy(Map<VehicleType, BigDecimal> hourlyRates, Duration gracePeriod) {
        Objects.requireNonNull(hourlyRates, "Ücret tablosu boş olamaz!");
        Objects.requireNonNull(gracePeriod, "Ücretsiz süre boş olamaz!");
        if (gracePeriod.isNegative()) {
            throw new IllegalArgumentException("Ücretsiz süre negatif olamaz!");
        }
        for (VehicleType type : VehicleType.values()) {
            BigDecimal rate = hourlyRates.get(type);
            if (rate == null) {
                throw new IllegalArgumentException(type + " için saatlik ücret tanımlanmamış!");
            }
            if (rate.signum() < 0) {
                throw new IllegalArgumentException(type + " için saatlik ücret negatif olamaz!");
            }
        }
        this.hourlyRates = new EnumMap<>(hourlyRates);
        this.gracePeriod = gracePeriod;
    }

    @Override
    public BigDecimal calculateFee(ParkingTicket ticket, Instant exitTime) {
        Objects.requireNonNull(ticket, "Bilet boş olamaz!");
        Objects.requireNonNull(exitTime, "Çıkış zamanı boş olamaz!");
        if (exitTime.isBefore(ticket.getEntryTime())) {
            throw new IllegalArgumentException("Çıkış zamanı giriş zamanından önce olamaz!");
        }

        Duration parked = Duration.between(ticket.getEntryTime(), exitTime);
        if (parked.compareTo(gracePeriod) <= 0) {
            return BigDecimal.ZERO;
        }

        long seconds = parked.getSeconds() + (parked.getNano() > 0 ? 1 : 0);
        long billableHours = (seconds + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR;
        BigDecimal rate = hourlyRates.get(ticket.getVehicle().getType());
        return rate.multiply(BigDecimal.valueOf(billableHours));
    }
}
