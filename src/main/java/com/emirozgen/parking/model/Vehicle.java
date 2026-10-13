package com.emirozgen.parking.model;

import com.emirozgen.parking.exception.InvalidVehicleException;

import java.util.Locale;
import java.util.Objects;

public abstract class Vehicle {

    private final String licensePlate;
    private final VehicleType type;

    protected Vehicle(String licensePlate, VehicleType type) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            throw new InvalidVehicleException("Plaka boş veya null olamaz!");
        }
        if (type == null) {
            throw new InvalidVehicleException("Araç türü boş olamaz!");
        }
        this.licensePlate = licensePlate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        this.type = type;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getType() {
        return type;
    }

    public abstract boolean canFitSize(SpotSize size);

    public boolean prefersChargingSpot() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(licensePlate, vehicle.licensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(licensePlate);
    }

    @Override
    public String toString() {
        return type + "[" + licensePlate + "]";
    }
}