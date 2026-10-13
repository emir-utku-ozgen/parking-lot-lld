package com.emirozgen.parking;

import com.emirozgen.parking.exception.ParkingFullException;
import com.emirozgen.parking.factory.VehicleFactory;
import com.emirozgen.parking.model.ParkingFloor;
import com.emirozgen.parking.model.ParkingSpot;
import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.Receipt;
import com.emirozgen.parking.model.SpotSize;
import com.emirozgen.parking.model.VehicleType;
import com.emirozgen.parking.observer.DisplayBoard;
import com.emirozgen.parking.service.ParkingLot;
import com.emirozgen.parking.strategy.fee.HourlyFeeStrategy;
import com.emirozgen.parking.strategy.spot.BestFitSpotStrategy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        // Composition root: tüm bağımlılıklar burada kurulup içeri verilir.
        List<ParkingFloor> floors = List.of(
                new ParkingFloor(0, List.of(
                        new ParkingSpot("F0-S1", SpotSize.SMALL, false),
                        new ParkingSpot("F0-M1", SpotSize.MEDIUM, true),
                        new ParkingSpot("F0-M2", SpotSize.MEDIUM, false),
                        new ParkingSpot("F0-L1", SpotSize.LARGE, false))),
                new ParkingFloor(1, List.of(
                        new ParkingSpot("F1-M1", SpotSize.MEDIUM, false),
                        new ParkingSpot("F1-L1", SpotSize.LARGE, false))));

        Map<VehicleType, BigDecimal> hourlyRates = new EnumMap<>(VehicleType.class);
        hourlyRates.put(VehicleType.MOTORCYCLE, new BigDecimal("20"));
        hourlyRates.put(VehicleType.CAR, new BigDecimal("50"));
        hourlyRates.put(VehicleType.ELECTRIC_CAR, new BigDecimal("40"));
        hourlyRates.put(VehicleType.TRUCK, new BigDecimal("100"));

        ParkingLot lot = new ParkingLot(floors, new BestFitSpotStrategy(),
                new HourlyFeeStrategy(hourlyRates, Duration.ZERO), Clock.systemUTC());
        DisplayBoard board = new DisplayBoard(floors);
        lot.addListener(board);

        System.out.println("Otopark sistemi başlatılıyor...\n");
        System.out.println(board.render());

        ParkingTicket motorcycle = lot.park(VehicleFactory.create(VehicleType.MOTORCYCLE, "34 MOT 01"));
        ParkingTicket electric = lot.park(VehicleFactory.create(VehicleType.ELECTRIC_CAR, "34 EV 02"));
        ParkingTicket truck = lot.park(VehicleFactory.create(VehicleType.TRUCK, "34 TIR 03"));
        for (ParkingTicket ticket : List.of(motorcycle, electric, truck)) {
            System.out.println("Giriş: " + ticket);
        }
        System.out.println();
        System.out.println(board.render());

        lot.park(VehicleFactory.create(VehicleType.TRUCK, "34 TIR 04"));
        try {
            lot.park(VehicleFactory.create(VehicleType.TRUCK, "34 TIR 05"));
        } catch (ParkingFullException e) {
            System.out.println("Reddedildi: " + e.getMessage() + "\n");
        }

        Receipt receipt = lot.unpark(truck.getTicketId());
        System.out.println("Çıkış: " + receipt.ticket().getVehicle() + ", ücret=" + receipt.fee() + " TL\n");
        System.out.println(board.render());
    }
}
