package com.emirozgen.parking.observer;

import com.emirozgen.parking.model.ParkingTicket;
import com.emirozgen.parking.model.Receipt;

/**
 * Otopark olaylarını dinleyenler için. Metotlar, durum değişikliği tamamlandıktan sonra
 * olayı tetikleyen thread üzerinde çağrılır; bu yüzden hızlı ve thread-safe olmalıdır.
 * İlgilenmediğin olayı override etmen gerekmez.
 */
public interface ParkingEventListener {

    default void onVehicleParked(ParkingTicket ticket) {
    }

    default void onVehicleLeft(Receipt receipt) {
    }
}
