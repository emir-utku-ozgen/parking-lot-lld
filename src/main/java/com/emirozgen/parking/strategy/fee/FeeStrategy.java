package com.emirozgen.parking.strategy.fee;

import com.emirozgen.parking.model.ParkingTicket;

import java.math.BigDecimal;
import java.time.Instant;

public interface FeeStrategy {

    BigDecimal calculateFee(ParkingTicket ticket, Instant exitTime);
}
