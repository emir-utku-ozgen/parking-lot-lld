package com.emirozgen.parking.model;

import com.emirozgen.parking.factory.VehicleFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingSpotTest {

    private final Vehicle car = VehicleFactory.create(VehicleType.CAR, "34 ABC 1");

    @Test
    void tryParkSucceedsOnEmptyFittingSpot() {
        ParkingSpot spot = new ParkingSpot("M1", SpotSize.MEDIUM, false);

        assertTrue(spot.tryPark(car));
        assertEquals(car, spot.getParkedVehicle());
    }

    @Test
    void tryParkReturnsFalseWhenOccupied() {
        ParkingSpot spot = new ParkingSpot("M1", SpotSize.MEDIUM, false);
        spot.park(car);

        assertFalse(spot.tryPark(VehicleFactory.create(VehicleType.CAR, "34 ABC 2")));
        assertEquals(car, spot.getParkedVehicle());
    }

    @Test
    void tryParkReturnsFalseWhenVehicleDoesNotFit() {
        ParkingSpot spot = new ParkingSpot("S1", SpotSize.SMALL, false);

        assertFalse(spot.tryPark(car));
        assertFalse(spot.isOccupied());
    }
}
