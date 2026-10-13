package com.emirozgen.parking.strategy.spot;

import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.SpotSize;

import java.util.List;

final class SpotStrategyFixture {

    private SpotStrategyFixture() {
    }

    /** Katlar kasıtlı olarak sırasız verilir: stratejiler kat numarasına göre karar vermeli. */
    static List<ParkingFloor> floors() {
        ParkingFloor upper = new ParkingFloor(1, List.of(
                new ParkingSpot("F1-L1", SpotSize.LARGE, false),
                new ParkingSpot("F1-M1", SpotSize.MEDIUM, false)));
        ParkingFloor ground = new ParkingFloor(0, List.of(
                new ParkingSpot("F0-L1", SpotSize.LARGE, false),
                new ParkingSpot("F0-M1", SpotSize.MEDIUM, true),
                new ParkingSpot("F0-S1", SpotSize.SMALL, false)));
        return List.of(upper, ground);
    }
}
