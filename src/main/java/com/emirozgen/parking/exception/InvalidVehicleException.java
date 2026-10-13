package com.emirozgen.parking.exception;

public class InvalidVehicleException extends RuntimeException{
    public InvalidVehicleException(String message){
        super(message);
    }
}
