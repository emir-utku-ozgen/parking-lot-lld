package com.emirozgen.parking.service;

import com.emirozgen.parking.exception.InvalidTicketException;
import com.emirozgen.parking.exception.ParkingFullException;
import com.emirozgen.parking.exception.VehicleAlreadyParkedException;
import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.Receipt;
import com.emirozgen.parking.model.SpotSize;
import com.emirozgen.parking.model.Vehicle;
import com.emirozgen.parking.strategy.fee.FeeStrategy;
import com.emirozgen.parking.strategy.spot.ParkingStrategy;
import com.emirozgen.parking.strategy.spot.SpotAssignment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Otoparkın dış dünyaya açılan tek kapısı (Facade): kat, yer, strateji ve
 * bilet yönetimini park/unpark çağrılarının arkasına saklar.
 */
public class ParkingLot {

    private final List<ParkingFloor> floors;
    private final ParkingStrategy parkingStrategy;
    private final FeeStrategy feeStrategy;
    private final Clock clock;

    private final Map<String, ParkingTicket> activeTicketsById = new ConcurrentHashMap<>();
    private final Map<String, ParkingTicket> activeTicketsByPlate = new ConcurrentHashMap<>();

    public ParkingLot(List<ParkingFloor> floors, ParkingStrategy parkingStrategy,
                      FeeStrategy feeStrategy, Clock clock) {
        if (floors == null || floors.isEmpty()) {
            throw new IllegalArgumentException("Otoparkta en az bir kat olmalı!");
        }
        this.floors = List.copyOf(floors);
        this.parkingStrategy = Objects.requireNonNull(parkingStrategy, "Park stratejisi boş olamaz!");
        this.feeStrategy = Objects.requireNonNull(feeStrategy, "Ücret stratejisi boş olamaz!");
        this.clock = Objects.requireNonNull(clock, "Saat boş olamaz!");
    }

    public ParkingTicket park(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Araç boş olamaz!");
        String plate = vehicle.getLicensePlate();
        if (activeTicketsByPlate.containsKey(plate)) {
            throw new VehicleAlreadyParkedException(plate + " plakalı araç zaten otoparkta!");
        }

        SpotAssignment assignment = reserveSpot(vehicle);
        ParkingTicket ticket = new ParkingTicket(vehicle, assignment.spot(),
                assignment.floor().getFloorNumber(), clock.instant());

        // Aynı plaka iki girişten aynı anda gelirse yalnızca biri kazanır.
        if (activeTicketsByPlate.putIfAbsent(plate, ticket) != null) {
            assignment.spot().release();
            throw new VehicleAlreadyParkedException(plate + " plakalı araç zaten otoparkta!");
        }
        activeTicketsById.put(ticket.getTicketId(), ticket);
        return ticket;
    }

    public Receipt unpark(String ticketId) {
        Objects.requireNonNull(ticketId, "Bilet numarası boş olamaz!");
        ParkingTicket ticket = activeTicketsById.get(ticketId);
        if (ticket == null) {
            throw new InvalidTicketException(ticketId + " numaralı aktif bilet yok!");
        }

        // Ücret, hiçbir durum değişmeden önce hesaplanır; hata olursa bilet geçerli kalır.
        Instant exitTime = clock.instant();
        BigDecimal fee = feeStrategy.calculateFee(ticket, exitTime);

        // Aynı bilet iki kez okutulursa remove yalnızca bir thread için true döner.
        if (!activeTicketsById.remove(ticketId, ticket)) {
            throw new InvalidTicketException(ticketId + " numaralı bilet zaten kullanıldı!");
        }
        activeTicketsByPlate.remove(ticket.getVehicle().getLicensePlate(), ticket);
        ticket.getSpot().release();
        return new Receipt(ticket, exitTime, fee);
    }

    public Optional<ParkingTicket> findActiveTicket(String licensePlate) {
        return Optional.ofNullable(activeTicketsByPlate.get(licensePlate));
    }

    public long availableSpotCount(SpotSize size) {
        return floors.stream()
                .mapToLong(floor -> floor.availableSpotCount(size))
                .sum();
    }

    public List<ParkingFloor> getFloors() {
        return floors;
    }

    /**
     * Optimistic retry: strateji aday yer önerir, tryPark atomik olarak kapmaya çalışır.
     * Aday başka thread'e gittiyse güncel durumla yeniden aranır.
     */
    private SpotAssignment reserveSpot(Vehicle vehicle) {
        while (true) {
            SpotAssignment candidate = parkingStrategy.findSpot(floors, vehicle)
                    .orElseThrow(() -> new ParkingFullException(vehicle + " için uygun boş yer yok!"));
            ParkingSpot spot = candidate.spot();
            if (spot.tryPark(vehicle)) {
                return candidate;
            }
        }
    }
}
