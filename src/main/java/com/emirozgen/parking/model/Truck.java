package com.emirozgen.parking.model;

public class Truck extends Vehicle{
    public Truck(String licensePlate){
        super(licensePlate,VehicleType.TRUCK);
    }
    @Override
    public boolean canFitSize(SpotSize size){
        return size==SpotSize.LARGE;
    }
}
