package com.emirozgen.parking.strategy.spot;

import com.emirozgen.parking.factory.VehicleFactory;
import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.VehicleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestFitSpotStrategyTest {

    private final ParkingStrategy strategy = new BestFitSpotStrategy();

    @ParameterizedTest
    @CsvSource({
            "MOTORCYCLE,   F0-S1",
            "CAR,          F1-M1",
            "ELECTRIC_CAR, F0-M1",
            "TRUCK,        F0-L1"
    })
    void picksSmallestFittingSpotWithChargingPreference(VehicleType type, String expectedSpotId) {
        SpotAssignment result = strategy.findSpot(SpotStrategyFixture.floors(),
                VehicleFactory.create(type, "34X1")).orElseThrow();

        assertEquals(expectedSpotId, result.spot().getId());
    }

    @Test
    void returnsEmptyWhenNoFittingSpotIsFree() {
        List<ParkingFloor> floors = SpotStrategyFixture.floors();
        floors.stream()
                .flatMap(floor -> floor.getSpots().stream())
                .filter(spot -> spot.getId().contains("-L"))
                .forEach(spot -> spot.park(VehicleFactory.create(VehicleType.TRUCK, spot.getId())));

        assertTrue(strategy.findSpot(floors, VehicleFactory.create(VehicleType.TRUCK, "34T9")).isEmpty());
    }

    @Test
    void skipsOccupiedSpots() {
        List<ParkingFloor> floors = SpotStrategyFixture.floors();
        ParkingSpot small = floors.get(1).getSpots().get(2);
        small.park(VehicleFactory.create(VehicleType.MOTORCYCLE, "34M0"));

        SpotAssignment result = strategy.findSpot(floors,
                VehicleFactory.create(VehicleType.MOTORCYCLE, "34M1")).orElseThrow();

        assertEquals("F1-M1", result.spot().getId());
    }
}
