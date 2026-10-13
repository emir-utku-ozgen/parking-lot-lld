package com.emirozgen.parking.factory;

import com.emirozgen.parking.model.Car;
import com.emirozgen.parking.model.Motorcycle;
import com.emirozgen.parking.model.Truck;
import com.emirozgen.parking.model.Vehicle;
import com.emirozgen.parking.model.VehicleType;

public final class VehicleFactory {

    private VehicleFactory() {
    }

    public static Vehicle create(VehicleType type, String licensePlate) {
        return switch (type) {
            case CAR -> new Car(licensePlate);
            case ELECTRIC_CAR -> new Car(licensePlate, true);
            case MOTORCYCLE -> new Motorcycle(licensePlate);
            case TRUCK -> new Truck(licensePlate);
        };
    }
}
