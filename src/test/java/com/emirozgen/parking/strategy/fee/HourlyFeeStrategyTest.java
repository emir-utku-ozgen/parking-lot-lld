package com.emirozgen.parking.strategy.fee;

import com.emirozgen.parking.factory.VehicleFactory;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.SpotSize;
import com.emirozgen.parking.model.VehicleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HourlyFeeStrategyTest {

    private static final Instant ENTRY = Instant.parse("2026-10-13T10:00:00Z");

    private final FeeStrategy strategy = new HourlyFeeStrategy(rates(), Duration.ofMinutes(15));
    private final ParkingTicket carTicket = new ParkingTicket(
            VehicleFactory.create(VehicleType.CAR, "34C1"),
            new ParkingSpot("M1", SpotSize.MEDIUM, false), 0, ENTRY);

    static Map<VehicleType, BigDecimal> rates() {
        Map<VehicleType, BigDecimal> rates = new EnumMap<>(VehicleType.class);
        rates.put(VehicleType.MOTORCYCLE, new BigDecimal("20"));
        rates.put(VehicleType.CAR, new BigDecimal("50"));
        rates.put(VehicleType.ELECTRIC_CAR, new BigDecimal("40"));
        rates.put(VehicleType.TRUCK, new BigDecimal("100"));
        return rates;
    }

    @ParameterizedTest(name = "{0} sn -> {1} TL")
    @CsvSource({
            "0,     0",
            "900,   0",    // ücretsiz süre sınırı dahil
            "901,   50",
            "3600,  50",
            "7200,  100",
            "7201,  150",
            "90000, 1250"  // 25 saat: gece yarısını geçen park
    })
    void roundsUpStartedHours(long seconds, String expectedFee) {
        BigDecimal fee = strategy.calculateFee(carTicket, ENTRY.plusSeconds(seconds));

        assertEquals(0, new BigDecimal(expectedFee).compareTo(fee));
    }

    @Test
    void countsNanosecondsAsStartedHour() {
        BigDecimal fee = strategy.calculateFee(carTicket, ENTRY.plus(Duration.ofHours(2).plusNanos(1)));

        assertEquals(0, new BigDecimal("150").compareTo(fee));
    }

    @Test
    void rejectsExitBeforeEntry() {
        assertThrows(IllegalArgumentException.class,
                () -> strategy.calculateFee(carTicket, ENTRY.minusSeconds(1)));
    }

    @Test
    void rejectsMissingRateAtConstruction() {
        Map<VehicleType, BigDecimal> rates = rates();
        rates.remove(VehicleType.TRUCK);

        assertThrows(IllegalArgumentException.class, () -> new HourlyFeeStrategy(rates, Duration.ZERO));
    }

    @Test
    void isNotAffectedByLaterChangesToRateMap() {
        Map<VehicleType, BigDecimal> rates = rates();
        FeeStrategy isolated = new HourlyFeeStrategy(rates, Duration.ZERO);
        rates.put(VehicleType.CAR, new BigDecimal("999"));

        BigDecimal fee = isolated.calculateFee(carTicket, ENTRY.plusSeconds(3600));

        assertEquals(0, new BigDecimal("50").compareTo(fee));
    }
}
