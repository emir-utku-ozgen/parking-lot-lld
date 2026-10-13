package com.emirozgen.parking.strategy.spot;

import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;

import java.util.Objects;

public record SpotAssignment(ParkingFloor floor, ParkingSpot spot) {

    public SpotAssignment {
        Objects.requireNonNull(floor, "Kat boş olamaz!");
        Objects.requireNonNull(spot, "Park yeri boş olamaz!");
    }
}
