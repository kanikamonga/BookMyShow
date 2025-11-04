package com.bms.BasicBookMyShow.model;

public enum SeatStatus {
    AVAILABLE,
    RESERVED,  // Temporarily reserved, expires after timeout
    BOOKED     // Confirmed booking
}
