package com.emirozgen.parking.service;

import com.emirozgen.parking.exception.InvalidTicketException;
import com.emirozgen.parking.exception.ParkingFullException;
import com.emirozgen.parking.exception.VehicleAlreadyParkedException;
import com.emirozgen.parking.factory.VehicleFactory;
import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.Receipt;
import com.emirozgen.parking.model.SpotSize;
import com.emirozgen.parking.model.Vehicle;
import com.emirozgen.parking.model.VehicleType;
import com.emirozgen.parking.strategy.fee.FeeStrategy;
import com.emirozgen.parking.strategy.fee.HourlyFeeStrategy;
import com.emirozgen.parking.strategy.spot.BestFitSpotStrategy;
import com.emirozgen.parking.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingLotTest {

    private static final Instant START = Instant.parse("2026-10-13T08:00:00Z");

    private MutableClock clock;
    private FeeStrategy feeStrategy;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        Map<VehicleType, BigDecimal> rates = new EnumMap<>(VehicleType.class);
        for (VehicleType type : VehicleType.values()) {
            rates.put(type, new BigDecimal("50"));
        }
        feeStrategy = new HourlyFeeStrategy(rates, Duration.ofMinutes(15));
    }

    private ParkingLot lotWithMediumSpots(int count) {
        List<ParkingSpot> spots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            spots.add(new ParkingSpot("M" + i, SpotSize.MEDIUM, false));
        }
        return new ParkingLot(List.of(new ParkingFloor(0, spots)), new BestFitSpotStrategy(), feeStrategy, clock);
    }

    private static Vehicle car(String plate) {
        return VehicleFactory.create(VehicleType.CAR, plate);
    }

    // --- Temel akış ---

    @Test
    void parkAndUnparkChargesByInjectedClock() {
        ParkingLot lot = lotWithMediumSpots(1);

        ParkingTicket ticket = lot.park(car("34A1"));
        clock.advance(Duration.ofMinutes(90));
        Receipt receipt = lot.unpark(ticket.getTicketId());

        assertEquals(START, ticket.getEntryTime());
        assertEquals(Duration.ofMinutes(90), receipt.parkedDuration());
        assertEquals(0, new BigDecimal("100").compareTo(receipt.fee()));
        assertEquals(1, lot.availableSpotCount(SpotSize.MEDIUM));
        assertTrue(lot.findActiveTicket("34A1").isEmpty());
    }

    @Test
    void throwsWhenFull() {
        ParkingLot lot = lotWithMediumSpots(1);
        lot.park(car("34A1"));

        assertThrows(ParkingFullException.class, () -> lot.park(car("34A2")));
    }

    @Test
    void rejectsSameVehicleTwice() {
        ParkingLot lot = lotWithMediumSpots(2);
        lot.park(car("34 a 1"));

        // Plaka normalize edildiği için boşluk/küçük harf farkı kaçış yolu değil.
        assertThrows(VehicleAlreadyParkedException.class, () -> lot.park(car("34A1")));
        assertEquals(1, lot.availableSpotCount(SpotSize.MEDIUM));
    }

    @Test
    void sameVehicleCanParkAgainAfterLeaving() {
        ParkingLot lot = lotWithMediumSpots(1);
        ParkingTicket first = lot.park(car("34A1"));
        lot.unpark(first.getTicketId());

        ParkingTicket second = lot.park(car("34A1"));

        assertFalse(first.equals(second));
    }

    @Test
    void rejectsUnknownAndReusedTickets() {
        ParkingLot lot = lotWithMediumSpots(1);
        ParkingTicket ticket = lot.park(car("34A1"));
        lot.unpark(ticket.getTicketId());

        assertThrows(InvalidTicketException.class, () -> lot.unpark("yok-boyle-bilet"));
        assertThrows(InvalidTicketException.class, () -> lot.unpark(ticket.getTicketId()));
    }

    @Test
    void ticketStaysValidWhenFeeCalculationFails() {
        ParkingLot lot = lotWithMediumSpots(1);
        ParkingTicket ticket = lot.park(car("34A1"));

        clock.setInstant(START.minusSeconds(60)); // saat geri kaydı (ör. NTP düzeltmesi)
        assertThrows(IllegalArgumentException.class, () -> lot.unpark(ticket.getTicketId()));

        assertSame(ticket, lot.findActiveTicket("34A1").orElseThrow());
        assertEquals(0, lot.availableSpotCount(SpotSize.MEDIUM));

        clock.setInstant(START.plus(Duration.ofHours(1)));
        assertEquals(0, new BigDecimal("50").compareTo(lot.unpark(ticket.getTicketId()).fee()));
    }

    // --- Eşzamanlılık ---

    /** Tüm görevleri aynı anda başlatır ve sonuçlarını (exception dahil) toplar. */
    private static <T> List<Future<T>> runConcurrently(int threads, IntFunction<Callable<T>> taskFactory)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                Callable<T> task = taskFactory.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
        return futures;
    }

    @RepeatedTest(20)
    void concurrentParkingNeverDoubleBooksASpot() throws Exception {
        int spots = 10;
        int cars = 50;
        ParkingLot lot = lotWithMediumSpots(spots);

        List<Future<ParkingTicket>> results = runConcurrently(cars, i -> () -> lot.park(car("34C" + i)));

        Set<String> usedSpots = new HashSet<>();
        int full = 0;
        for (Future<ParkingTicket> result : results) {
            try {
                assertTrue(usedSpots.add(result.get().getSpot().getId()), "Aynı yer iki araca verildi!");
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof ParkingFullException, e.getCause().toString());
                full++;
            }
        }
        assertEquals(spots, usedSpots.size());
        assertEquals(cars - spots, full);
        assertEquals(0, lot.availableSpotCount(SpotSize.MEDIUM));
    }

    @RepeatedTest(20)
    void concurrentEntriesOfSamePlateAdmitOnlyOne() throws Exception {
        ParkingLot lot = lotWithMediumSpots(10);

        List<Future<ParkingTicket>> results = runConcurrently(10, i -> () -> lot.park(car("34KLON")));

        int admitted = 0;
        for (Future<ParkingTicket> result : results) {
            try {
                result.get();
                admitted++;
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof VehicleAlreadyParkedException, e.getCause().toString());
            }
        }
        assertEquals(1, admitted);
        // Kaybeden thread'lerin kaptığı yerler geri bırakılmış olmalı.
        assertEquals(9, lot.availableSpotCount(SpotSize.MEDIUM));
    }

    @RepeatedTest(20)
    void concurrentUnparkOfSameTicketSucceedsOnce() throws Exception {
        ParkingLot lot = lotWithMediumSpots(1);
        String ticketId = lot.park(car("34A1")).getTicketId();
        clock.advance(Duration.ofHours(1));

        List<Future<Receipt>> results = runConcurrently(10, i -> () -> lot.unpark(ticketId));

        int succeeded = 0;
        for (Future<Receipt> result : results) {
            try {
                result.get();
                succeeded++;
            } catch (ExecutionException e) {
                assertTrue(e.getCause() instanceof InvalidTicketException, e.getCause().toString());
            }
        }
        assertEquals(1, succeeded);
        assertEquals(1, lot.availableSpotCount(SpotSize.MEDIUM));
    }
}
