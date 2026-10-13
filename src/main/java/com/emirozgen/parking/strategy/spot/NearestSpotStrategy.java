package com.emirozgen.parking.strategy.spot;

import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.Vehicle;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * First-fit: en alt kattan başlayarak araca sığan ilk boş yeri verir.
 */
public class NearestSpotStrategy implements ParkingStrategy {

    @Override
    public Optional<SpotAssignment> findSpot(List<ParkingFloor> floors, Vehicle vehicle) {
        return floors.stream()
                .sorted(Comparator.comparingInt(ParkingFloor::getFloorNumber))
                .flatMap(floor -> floor.getSpots().stream()
                        .filter(spot -> spot.canFit(vehicle))
                        .map(spot -> new SpotAssignment(floor, spot)))
                .findFirst();
    }
}
