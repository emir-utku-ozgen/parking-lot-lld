package com.emirozgen.parking.observer;

import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.Receipt;
import com.emirozgen.parking.model.SpotSize;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kat ve boyut bazında boş yer panosu. Sayaçlar olaylarla +1/-1 güncellenir;
 * artırma/azaltma sırası fark etmediği için eşzamanlı olaylar sayacı bozmaz.
 * Otopark trafiğe açılmadan önce kaydedilmelidir. Katta hiç bulunmayan boyutlar panoda gösterilmez.
 */
public class DisplayBoard implements ParkingEventListener {

    private final Map<Integer, Map<SpotSize, AtomicInteger>> freeSpots;

    public DisplayBoard(List<ParkingFloor> floors) {
        Map<Integer, Map<SpotSize, AtomicInteger>> counters = new TreeMap<>();
        for (ParkingFloor floor : floors) {
            Map<SpotSize, AtomicInteger> perSize = new EnumMap<>(SpotSize.class);
            for (SpotSize size : SpotSize.values()) {
                boolean floorHasSize = floor.getSpots().stream().anyMatch(spot -> spot.getSize() == size);
                if (floorHasSize) {
                    perSize.put(size, new AtomicInteger((int) floor.availableSpotCount(size)));
                }
            }
            counters.put(floor.getFloorNumber(), Collections.unmodifiableMap(perSize));
        }
        this.freeSpots = Collections.unmodifiableMap(counters);
    }

    @Override
    public void onVehicleParked(ParkingTicket ticket) {
        counter(ticket.getFloorNumber(), ticket.getSpot()).decrementAndGet();
    }

    @Override
    public void onVehicleLeft(Receipt receipt) {
        ParkingTicket ticket = receipt.ticket();
        counter(ticket.getFloorNumber(), ticket.getSpot()).incrementAndGet();
    }

    public int availableSpots(int floorNumber, SpotSize size) {
        AtomicInteger free = counters(floorNumber).get(size);
        return free == null ? 0 : free.get();
    }

    public String render() {
        StringBuilder board = new StringBuilder("=== BOŞ YER PANOSU ===\n");
        freeSpots.forEach((floorNumber, perSize) -> {
            board.append("Kat ").append(floorNumber).append(":");
            perSize.forEach((size, free) -> {
                int count = free.get();
                board.append("  ").append(size).append("=").append(count == 0 ? "DOLU" : count);
            });
            board.append('\n');
        });
        return board.toString();
    }

    private AtomicInteger counter(int floorNumber, ParkingSpot spot) {
        return counters(floorNumber).get(spot.getSize());
    }

    private Map<SpotSize, AtomicInteger> counters(int floorNumber) {
        Map<SpotSize, AtomicInteger> perSize = freeSpots.get(floorNumber);
        if (perSize == null) {
            throw new IllegalArgumentException(floorNumber + ". kat panoda tanımlı değil!");
        }
        return perSize;
    }
}
