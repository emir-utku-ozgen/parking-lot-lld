package com.emirozgen.parking.observer;

import com.emirozgen.parking.exception.ParkingFullException;
import com.emirozgen.parking.factory.VehicleFactory;
import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.SpotSize;
import com.emirozgen.parking.model.VehicleType;
import com.emirozgen.parking.service.ParkingLot;
import com.emirozgen.parking.strategy.fee.HourlyFeeStrategy;
import com.emirozgen.parking.strategy.spot.NearestSpotStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayBoardTest {

    private List<ParkingFloor> floors;
    private ParkingLot lot;
    private DisplayBoard board;

    @BeforeEach
    void setUp() {
        floors = List.of(floor(0, 5), floor(1, 5));
        Map<VehicleType, BigDecimal> rates = new EnumMap<>(VehicleType.class);
        for (VehicleType type : VehicleType.values()) {
            rates.put(type, BigDecimal.TEN);
        }
        lot = new ParkingLot(floors, new NearestSpotStrategy(),
                new HourlyFeeStrategy(rates, Duration.ZERO), Clock.systemUTC());
        board = new DisplayBoard(floors);
        lot.addListener(board);
    }

    private static ParkingFloor floor(int number, int mediumSpots) {
        List<ParkingSpot> spots = new ArrayList<>();
        for (int i = 0; i < mediumSpots; i++) {
            spots.add(new ParkingSpot("F" + number + "-M" + i, SpotSize.MEDIUM, false));
        }
        spots.add(new ParkingSpot("F" + number + "-L0", SpotSize.LARGE, false));
        return new ParkingFloor(number, spots);
    }

    @Test
    void startsFromCurrentFloorState() {
        assertEquals(5, board.availableSpots(0, SpotSize.MEDIUM));
        assertEquals(1, board.availableSpots(1, SpotSize.LARGE));
        assertEquals(0, board.availableSpots(1, SpotSize.SMALL));
    }

    @Test
    void hidesSizesThatDoNotExistOnFloor() {
        String rendered = board.render();

        assertTrue(rendered.contains("Kat 1:  MEDIUM=5  LARGE=1\n"), rendered);
        assertTrue(!rendered.contains("SMALL"), rendered);
    }

    @Test
    void tracksParkAndLeaveOnTheRightFloorAndSize() {
        ParkingTicket truck = lot.park(VehicleFactory.create(VehicleType.TRUCK, "34T1"));
        lot.park(VehicleFactory.create(VehicleType.CAR, "34C1"));

        assertEquals(0, board.availableSpots(0, SpotSize.LARGE));
        assertEquals(4, board.availableSpots(0, SpotSize.MEDIUM));
        assertTrue(board.render().contains("Kat 0:  MEDIUM=4  LARGE=DOLU\n"), board.render());

        lot.unpark(truck.getTicketId());

        assertEquals(1, board.availableSpots(0, SpotSize.LARGE));
    }

    @Test
    void rejectsUnknownFloor() {
        assertThrows(IllegalArgumentException.class, () -> board.availableSpots(7, SpotSize.MEDIUM));
    }

    @RepeatedTest(20)
    void staysConsistentWithRealStateUnderConcurrentTraffic() throws Exception {
        int threads = 16;
        int roundsPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            String plate = "34K" + t;
            futures.add(pool.submit(() -> {
                go.await();
                for (int r = 0; r < roundsPerThread; r++) {
                    try {
                        ParkingTicket ticket = lot.park(VehicleFactory.create(VehicleType.CAR, plate));
                        lot.unpark(ticket.getTicketId());
                    } catch (ParkingFullException ignored) {
                        // 16 araç, 10 MEDIUM yer: dolu otopark beklenen bir sonuç
                    }
                }
                return null;
            }));
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        for (Future<?> future : futures) {
            future.get();
        }

        for (ParkingFloor floor : floors) {
            for (SpotSize size : SpotSize.values()) {
                assertEquals(floor.availableSpotCount(size), board.availableSpots(floor.getFloorNumber(), size),
                        "Kat " + floor.getFloorNumber() + " " + size);
            }
        }
    }
}
