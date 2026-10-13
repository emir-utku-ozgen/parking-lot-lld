package com.emirozgen.parking.strategy.spot;

import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.Vehicle;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Best-fit: araca sığan en küçük yeri verir; elektrikli araçları şarjlı yere,
 * diğerlerini şarjsız yere yönlendirir. Eşitlikte alt kat önceliklidir.
 */
public class BestFitSpotStrategy implements ParkingStrategy {

    @Override
    public Optional<SpotAssignment> findSpot(List<ParkingFloor> floors, Vehicle vehicle) {
        Comparator<SpotAssignment> preference = Comparator
                .comparing((SpotAssignment a) -> a.spot().getSize())
                .thenComparing(a -> a.spot().hasChargingStation() != vehicle.prefersChargingSpot())
                .thenComparingInt(a -> a.floor().getFloorNumber());

        return floors.stream()
                .flatMap(floor -> floor.getSpots().stream()
                        .filter(spot -> spot.canFit(vehicle))
                        .map(spot -> new SpotAssignment(floor, spot)))
                .min(preference);
    }
}
