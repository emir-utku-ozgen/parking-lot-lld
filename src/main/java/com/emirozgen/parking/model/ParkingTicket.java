package com.emirozgen.parking.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class ParkingTicket {

    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final int floorNumber;
    private final LocalDateTime entryTime;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot, int floorNumber, LocalDateTime entryTime) {
        this.vehicle = Objects.requireNonNull(vehicle, "Araç boş olamaz!");
        this.spot = Objects.requireNonNull(spot, "Park yeri boş olamaz!");
        this.entryTime = Objects.requireNonNull(entryTime, "Giriş zamanı boş olamaz!");
        this.floorNumber = floorNumber;
        this.ticketId = UUID.randomUUID().toString();
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParkingTicket)) {
            return false;
        }
        ParkingTicket that = (ParkingTicket) o;
        return Objects.equals(ticketId, that.ticketId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ticketId);
    }

    @Override
    public String toString() {
        return "ParkingTicket[" + ticketId + ", " + vehicle + ", kat=" + floorNumber
                + ", " + spot.getId() + ", giriş=" + entryTime + "]";
    }
}
