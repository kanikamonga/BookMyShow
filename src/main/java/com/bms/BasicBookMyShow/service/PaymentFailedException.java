package com.bms.BasicBookMyShow.service;

/**
 * Exception thrown when payment processing fails during booking confirmation
 */
public class PaymentFailedException extends Exception {
    private final String reservationId;
    private final String paymentId;

    public PaymentFailedException(String message, String reservationId, String paymentId) {
        super(message);
        this.reservationId = reservationId;
        this.paymentId = paymentId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}

