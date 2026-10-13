package com.emirozgen.parking.strategy.spot;

import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface ParkingStrategy {

    Optional<SpotAssignment> findSpot(List<ParkingFloor> floors, Vehicle vehicle);
}
