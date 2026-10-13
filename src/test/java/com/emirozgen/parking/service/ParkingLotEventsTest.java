package com.emirozgen.parking.service;

import com.emirozgen.parking.exception.ParkingFullException;
import com.emirozgen.parking.exception.VehicleAlreadyParkedException;
import com.emirozgen.parking.factory.VehicleFactory;
import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.Receipt;
import com.emirozgen.parking.model.SpotSize;
import com.emirozgen.parking.model.VehicleType;
import com.emirozgen.parking.observer.ParkingEventListener;
import com.emirozgen.parking.strategy.fee.HourlyFeeStrategy;
import com.emirozgen.parking.strategy.spot.BestFitSpotStrategy;
import com.emirozgen.parking.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingLotEventsTest {

    /** Olayları kaydeder; ayrıca olay anında yerin gerçekten dolu/boş olduğunu not eder. */
    private static final class RecordingListener implements ParkingEventListener {
        final List<String> events = new ArrayList<>();
        final List<Boolean> spotOccupiedAtEvent = new ArrayList<>();

        @Override
        public void onVehicleParked(ParkingTicket ticket) {
            events.add("PARKED " + ticket.getVehicle().getLicensePlate());
            spotOccupiedAtEvent.add(ticket.getSpot().isOccupied());
        }

        @Override
        public void onVehicleLeft(Receipt receipt) {
            events.add("LEFT " + receipt.ticket().getVehicle().getLicensePlate());
            spotOccupiedAtEvent.add(receipt.ticket().getSpot().isOccupied());
        }
    }

    private ParkingLot lot;
    private RecordingListener recorder;

    @BeforeEach
    void setUp() {
        Map<VehicleType, BigDecimal> rates = new EnumMap<>(VehicleType.class);
        for (VehicleType type : VehicleType.values()) {
            rates.put(type, BigDecimal.TEN);
        }
        lot = new ParkingLot(
                List.of(new ParkingFloor(0, List.of(new ParkingSpot("M1", SpotSize.MEDIUM, false)))),
                new BestFitSpotStrategy(),
                new HourlyFeeStrategy(rates, Duration.ZERO),
                new MutableClock(Instant.parse("2026-10-13T08:00:00Z")));
        recorder = new RecordingListener();
        lot.addListener(recorder);
    }

    @Test
    void publishesEventsAfterStateChangeIsCommitted() {
        ParkingTicket ticket = lot.park(VehicleFactory.create(VehicleType.CAR, "34A1"));
        lot.unpark(ticket.getTicketId());

        assertEquals(List.of("PARKED 34A1", "LEFT 34A1"), recorder.events);
        // Dinleyici olayı aldığında durum zaten güncellenmiş olmalı.
        assertEquals(List.of(true, false), recorder.spotOccupiedAtEvent);
    }

    @Test
    void doesNotPublishWhenOperationFails() {
        lot.park(VehicleFactory.create(VehicleType.CAR, "34A1"));
        recorder.events.clear();

        assertThrows(VehicleAlreadyParkedException.class,
                () -> lot.park(VehicleFactory.create(VehicleType.CAR, "34A1")));
        assertThrows(ParkingFullException.class,
                () -> lot.park(VehicleFactory.create(VehicleType.CAR, "34A2")));

        assertTrue(recorder.events.isEmpty());
    }

    @Test
    void failingListenerDoesNotBreakParkingOrOtherListeners() {
        lot.removeListener(recorder);
        lot.addListener(new ParkingEventListener() {
            @Override
            public void onVehicleParked(ParkingTicket ticket) {
                throw new IllegalStateException("pano bağlantısı koptu (test)");
            }
        });
        lot.addListener(recorder);

        ParkingTicket ticket = lot.park(VehicleFactory.create(VehicleType.CAR, "34A1"));

        assertSame(ticket, lot.findActiveTicket("34A1").orElseThrow());
        assertEquals(List.of("PARKED 34A1"), recorder.events);
    }

    @Test
    void removedListenerStopsReceivingEvents() {
        lot.removeListener(recorder);

        lot.park(VehicleFactory.create(VehicleType.CAR, "34A1"));

        assertFalse(recorder.events.contains("PARKED 34A1"));
    }
}
