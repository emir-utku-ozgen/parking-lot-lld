package com.emirozgen.parking.model;

import java.util.Objects;

public class ParkingSpot {

    private final String id;
    private final SpotSize size;
    private final boolean hasChargingStation;
    private Vehicle parkedVehicle;

    public ParkingSpot(String id, SpotSize size, boolean hasChargingStation) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Spot id boş olamaz!");
        }
        if (size == null) {
            throw new IllegalArgumentException("SpotSize boş olamaz!");
        }
        this.id = id;
        this.size = size;
        this.hasChargingStation = hasChargingStation;
    }

    public String getId() {
        return id;
    }

    public SpotSize getSize() {
        return size;
    }

    public boolean hasChargingStation() {
        return hasChargingStation;
    }

    public synchronized boolean isOccupied() {
        return parkedVehicle != null;
    }

    public synchronized boolean canFit(Vehicle vehicle) {
        return !isOccupied() && vehicle.canFitSize(size);
    }

    public synchronized Vehicle getParkedVehicle() {
        return parkedVehicle;
    }

    public synchronized void park(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Araç boş olamaz!");
        if (isOccupied()) {
            throw new IllegalStateException(id + " yeri zaten dolu!");
        }
        if (!vehicle.canFitSize(size)) {
            throw new IllegalArgumentException(vehicle + " aracı " + id + " yerine sığmıyor!");
        }
        this.parkedVehicle = vehicle;
    }

    /**
     * Kontrol ve yerleştirmeyi tek kilit altında yapar; yer bu arada başka thread'e
     * gittiyse exception yerine false döner, böylece çağıran yeni yer arayabilir.
     */
    public synchronized boolean tryPark(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Araç boş olamaz!");
        if (!canFit(vehicle)) {
            return false;
        }
        this.parkedVehicle = vehicle;
        return true;
    }

    public synchronized Vehicle release() {
        if (!isOccupied()) {
            throw new IllegalStateException(id + " yeri zaten boş!");
        }
        Vehicle leaving = parkedVehicle;
        this.parkedVehicle = null;
        return leaving;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParkingSpot)) {
            return false;
        }
        ParkingSpot that = (ParkingSpot) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ParkingSpot[" + id + ", " + size + (hasChargingStation ? ", charging" : "") + "]";
    }
}
