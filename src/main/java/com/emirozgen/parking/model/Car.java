package com.emirozgen.parking.model;

public class Car extends Vehicle{
    public Car(String licensePlate){
        this(licensePlate,false);
    }
    public Car(String licensePlate,boolean electric){
        super(licensePlate,electric ? VehicleType.ELECTRIC_CAR: VehicleType.CAR);
    }
    public boolean isElectric(){
        return getType()==VehicleType.ELECTRIC_CAR;
    }
    @Override
    public boolean canFitSize(SpotSize size) {
        return size==SpotSize.MEDIUM || size==SpotSize.LARGE;
    }
    @Override
    public boolean prefersChargingSpot(){
        return isElectric();
    }
}
