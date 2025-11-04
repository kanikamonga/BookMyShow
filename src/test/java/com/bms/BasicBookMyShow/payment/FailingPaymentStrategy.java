package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.Payment;
import com.bms.BasicBookMyShow.model.PaymentMethod;
import com.bms.BasicBookMyShow.model.PaymentStatus;

/**
 * Test payment strategy that always fails
 * Used for testing payment failure scenarios
 */
public class FailingPaymentStrategy implements PaymentStrategy {
    @Override
    public Payment processPayment(double amount) {
        Payment payment = new Payment(amount, PaymentMethod.CREDIT_CARD);
        payment.setPaymentStatus(PaymentStatus.FAILED);
        return payment;
    }
}

