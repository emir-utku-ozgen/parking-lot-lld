package com.emirozgen.parking.model;

import java.util.List;

public class ParkingFloor {

    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int floorNumber, List<ParkingSpot> spots) {
        if (spots == null || spots.isEmpty()) {
            throw new IllegalArgumentException("Bir katta en az bir park yeri olmalı!");
        }
        this.floorNumber = floorNumber;
        this.spots = List.copyOf(spots);
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }

    public long availableSpotCount(SpotSize size) {
        return spots.stream()
                .filter(spot -> spot.getSize() == size)
                .filter(spot -> !spot.isOccupied())
                .count();
    }

    @Override
    public String toString() {
        return "ParkingFloor[kat=" + floorNumber + ", toplamYer=" + spots.size() + "]";
    }
}
