package com.bms.BasicBookMyShow.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a temporary reservation of seats with expiry time
 */
public class SeatReservation {
    private final String reservationId;
    private final String showId;
    private final List<String> seatNumbers;
    private final String userId;
    private final LocalDateTime expiryTime;
    private final LocalDateTime createdTime;
    private static final int RESERVATION_TIMEOUT_MINUTES = 5; // 5 minutes to complete booking

    public SeatReservation(String showId, List<String> seatNumbers, String userId) {
        this.reservationId = IdGenerator.generateId();
        this.showId = showId;
        this.seatNumbers = seatNumbers;
        this.userId = userId;
        this.createdTime = LocalDateTime.now();
        this.expiryTime = createdTime.plusMinutes(RESERVATION_TIMEOUT_MINUTES);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getShowId() {
        return showId;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public boolean belongsToUser(String userId) {
        return this.userId.equals(userId);
    }
}

