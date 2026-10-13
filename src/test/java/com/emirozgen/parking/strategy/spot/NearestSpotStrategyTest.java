package com.emirozgen.parking.strategy.spot;

import com.emirozgen.parking.factory.VehicleFactory;
import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NearestSpotStrategyTest {

    private final ParkingStrategy strategy = new NearestSpotStrategy();

    @Test
    void picksFirstFittingSpotOnLowestFloor() {
        List<ParkingFloor> floors = SpotStrategyFixture.floors();

        SpotAssignment result = strategy.findSpot(floors,
                VehicleFactory.create(VehicleType.MOTORCYCLE, "34M1")).orElseThrow();

        // First-fit'in zayıflığı: motosiklet LARGE yere düşüyor.
        assertEquals("F0-L1", result.spot().getId());
        assertEquals(0, result.floor().getFloorNumber());
    }
}
