package com.emirozgen.parking.model;

public class Motorcycle extends Vehicle{
    public Motorcycle(String licensePlate){
        super(licensePlate,VehicleType.MOTORCYCLE);
    }
    @Override
    public boolean canFitSize(SpotSize size){
        return true;
    }

}
