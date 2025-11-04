package com.bms.BasicBookMyShow.payment;

import com.bms.BasicBookMyShow.model.Payment;
import com.bms.BasicBookMyShow.model.PaymentStatus;

public class MockPaymentProcessor {
    public static PaymentStatus mockPayment(Payment payment){
        return Math.random()>0.2 ? PaymentStatus.SUCCESSFUL:PaymentStatus.FAILED;
    }
}
